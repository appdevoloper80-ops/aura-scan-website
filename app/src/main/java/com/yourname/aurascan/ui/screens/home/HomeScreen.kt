package com.yourname.aurascan.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.yourname.aurascan.Constants
import com.yourname.aurascan.R
import com.yourname.aurascan.domain.model.Document
import com.yourname.aurascan.ui.components.AdaptiveBannerPlaceholder
import com.yourname.aurascan.ui.components.DeleteConfirmDialog
import com.yourname.aurascan.ui.components.EmptyStateCard
import com.yourname.aurascan.ui.components.RenameDocumentDialog
import com.yourname.aurascan.util.DateFormatter
import com.yourname.aurascan.util.ShareAndSaveUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDocument: (String) -> Unit,
    onOpenImagesToPdf: () -> Unit,
    onOpenResizer: () -> Unit,
    onOpenOcr: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val isSavingScan by viewModel.isSavingScan.collectAsStateWithLifecycle()

    var documentToRename by remember { mutableStateOf<Document?>(null) }
    var documentToDelete by remember { mutableStateOf<Document?>(null) }
    var pendingSaveDocument by remember { mutableStateOf<Document?>(null) }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destinationUri ->
        val doc = pendingSaveDocument
        pendingSaveDocument = null
        if (destinationUri != null && doc != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ShareAndSaveUtils.copyFileToUri(context, File(doc.pdfPath), destinationUri)
                }
                result.onSuccess {
                    snackbarHostState.showSnackbar(context.getString(R.string.saved_to_device))
                }.onFailure { err ->
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.save_failed, err.localizedMessage ?: "")
                    )
                }
            }
        }
    }

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(Constants.MAX_SCAN_PAGES)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scannerClient = remember { GmsDocumentScanning.getClient(scannerOptions) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                val pdfUri = scanResult?.pdf?.uri
                val pageUris = scanResult?.pages?.map { it.imageUri }.orEmpty()
                if (pdfUri == null && pageUris.isEmpty()) {
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.scan_saved_error))
                    }
                } else {
                    viewModel.saveScannedDocument(
                        pdfUri = pdfUri,
                        pageUris = pageUris,
                        onSuccess = { doc -> onOpenDocument(doc.id) },
                        onError = { err ->
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    err.localizedMessage ?: context.getString(R.string.scan_saved_error)
                                )
                            }
                        }
                    )
                }
            }
            Activity.RESULT_CANCELED -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.scan_cancelled))
                }
            }
            else -> {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.scan_saved_error))
                }
            }
        }
    }

    fun launchScanner() {
        val hostActivity = activity ?: return
        scannerClient.getStartScanIntent(hostActivity)
            .addOnSuccessListener { intentSender ->
                try {
                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            context.getString(R.string.scan_failed, e.localizedMessage ?: "")
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                scope.launch {
                    snackbarHostState.showSnackbar(
                        context.getString(R.string.scan_failed, e.localizedMessage ?: "")
                    )
                }
            }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Reserved fixed-height adaptive banner ad slot (empty for now)
            AdaptiveBannerPlaceholder()
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ScanHeroButton(
                    isBusy = isSavingScan,
                    onClick = { launchScanner() }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.home_tools_section),
                        style = MaterialTheme.typography.titleMedium
                    )
                    ToolsRow(
                        onOpenImagesToPdf = onOpenImagesToPdf,
                        onOpenResizer = onOpenResizer,
                        onOpenOcr = onOpenOcr
                    )
                }
            }

            item {
                Text(
                    text = stringResource(R.string.home_recent_documents),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (documents.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Description,
                        title = stringResource(R.string.home_empty_title),
                        subtitle = stringResource(R.string.home_empty_subtitle)
                    )
                }
            } else {
                items(
                    items = documents,
                    key = { it.id }
                ) { document ->
                    DocumentListItem(
                        document = document,
                        onClick = { onOpenDocument(document.id) },
                        onRename = { documentToRename = document },
                        onShare = {
                            val shared = ShareAndSaveUtils.shareFile(
                                context = context,
                                file = File(document.pdfPath),
                                mimeType = "application/pdf",
                                chooserTitle = context.getString(R.string.document_action_share_pdf)
                            )
                            if (!shared) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.share_failed))
                                }
                            }
                        },
                        onSaveToDevice = {
                            pendingSaveDocument = document
                            savePdfLauncher.launch(
                                ShareAndSaveUtils.sanitizeFileName(document.name, "pdf")
                            )
                        },
                        onDelete = { documentToDelete = document }
                    )
                }
            }
        }
    }

    documentToRename?.let { target ->
        RenameDocumentDialog(
            initialName = target.name,
            onDismiss = { documentToRename = null },
            onConfirm = { newName ->
                viewModel.renameDocument(target.id, newName) {
                    documentToRename = null
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.document_renamed))
                    }
                }
            }
        )
    }

    documentToDelete?.let { target ->
        DeleteConfirmDialog(
            documentName = target.name,
            onDismiss = { documentToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(target.id) {
                    documentToDelete = null
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.document_deleted))
                    }
                }
            }
        )
    }
}

@Composable
private fun ScanHeroButton(
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = !isBusy,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
    ) {
        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.scan_saving),
                style = MaterialTheme.typography.titleMedium
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.DocumentScanner,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_scan_document),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.home_scan_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun ToolsRow(
    onOpenImagesToPdf: () -> Unit,
    onOpenResizer: () -> Unit,
    onOpenOcr: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useVerticalStack = maxWidth < 330.dp
        if (useVerticalStack) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ToolCard(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.tool_images_to_pdf),
                    onClick = onOpenImagesToPdf,
                    modifier = Modifier.fillMaxWidth()
                )
                ToolCard(
                    icon = Icons.Outlined.PhotoSizeSelectLarge,
                    label = stringResource(R.string.tool_resizer),
                    onClick = onOpenResizer,
                    modifier = Modifier.fillMaxWidth()
                )
                ToolCard(
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.tool_ocr),
                    onClick = onOpenOcr,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ToolCard(
                    icon = Icons.Outlined.PhotoLibrary,
                    label = stringResource(R.string.tool_images_to_pdf),
                    onClick = onOpenImagesToPdf,
                    modifier = Modifier.weight(1f)
                )
                ToolCard(
                    icon = Icons.Outlined.PhotoSizeSelectLarge,
                    label = stringResource(R.string.tool_resizer),
                    onClick = onOpenResizer,
                    modifier = Modifier.weight(1f)
                )
                ToolCard(
                    icon = Icons.Outlined.TextFields,
                    label = stringResource(R.string.tool_ocr),
                    onClick = onOpenOcr,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ToolCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 104.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(22.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DocumentListItem(
    document: Document,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToDevice: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val firstPagePath = document.pagePaths.firstOrNull()
    val pageCountLabel = if (document.pageCount == 1) {
        stringResource(R.string.page_count_singular, document.pageCount)
    } else {
        stringResource(R.string.page_count_plural, document.pageCount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 56.dp, height = 74.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!firstPagePath.isNullOrBlank() && File(firstPagePath).exists()) {
                    AsyncImage(
                        model = File(firstPagePath),
                        contentDescription = stringResource(R.string.cd_document_thumbnail, document.name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = stringResource(R.string.cd_document_thumbnail, document.name),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = document.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateFormatter.formatDisplayDate(document.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = pageCountLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options, document.name)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_share)) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_save_to_device)) },
                        onClick = {
                            menuExpanded = false
                            onSaveToDevice()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
