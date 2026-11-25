package com.yourname.aurascan.ui.screens.resizer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PhotoSizeSelectLarge
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yourname.aurascan.Constants
import com.yourname.aurascan.R
import com.yourname.aurascan.domain.model.CropRect
import com.yourname.aurascan.domain.model.ResizePreset
import com.yourname.aurascan.ui.components.EmptyStateCard
import com.yourname.aurascan.ui.components.ProUpsellDialog
import com.yourname.aurascan.util.ShareAndSaveUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResizerScreen(
    viewModel: ResizerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isCropStepOpen) {
        viewModel.closeCropStep()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImagePicked(uri)
        }
    }

    val saveImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { destinationUri ->
        val out = state.output ?: return@rememberLauncherForActivityResult
        if (destinationUri != null) {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    ShareAndSaveUtils.copyFileToUri(context, File(out.filePath), destinationUri)
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

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isCropStepOpen) {
                            stringResource(R.string.resizer_crop_step)
                        } else {
                            stringResource(R.string.resizer_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.isCropStepOpen) viewModel.closeCropStep() else onBack()
                        },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isCropStepOpen && state.sourceImagePath != null) {
            CropStepContent(
                imagePath = state.sourceImagePath!!,
                sourceWidth = state.sourceWidth,
                sourceHeight = state.sourceHeight,
                cropRect = state.cropRect,
                onCropChange = viewModel::updateCropRect,
                onResetCrop = viewModel::resetCropRect,
                onApplyCrop = viewModel::closeCropStep,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.sourceImagePath == null) {
                                stringResource(R.string.resizer_pick_image)
                            } else {
                                stringResource(R.string.resizer_change_image)
                            }
                        )
                    }

                    if (state.sourceImagePath != null) {
                        OutlinedButton(
                            onClick = { viewModel.openCropStep() },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Outlined.Crop, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.resizer_crop_step))
                        }
                    }
                }

                if (state.sourceImagePath == null) {
                    EmptyStateCard(
                        icon = Icons.Outlined.PhotoSizeSelectLarge,
                        title = stringResource(R.string.resizer_title),
                        subtitle = stringResource(R.string.resizer_pick_image)
                    )
                } else {
                    Text(
                        text = stringResource(R.string.resizer_presets_label),
                        style = MaterialTheme.typography.titleSmall
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedPreset == ResizePreset.EXAM_PHOTO,
                            onClick = { viewModel.selectPreset(ResizePreset.EXAM_PHOTO) },
                            label = { Text(stringResource(R.string.preset_exam_photo)) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                        FilterChip(
                            selected = state.selectedPreset == ResizePreset.EXAM_SIGNATURE,
                            onClick = { viewModel.selectPreset(ResizePreset.EXAM_SIGNATURE) },
                            label = { Text(stringResource(R.string.preset_exam_signature)) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                        FilterChip(
                            selected = state.selectedPreset == ResizePreset.PASSPORT_PHOTO,
                            onClick = { viewModel.selectPreset(ResizePreset.PASSPORT_PHOTO) },
                            label = { Text(stringResource(R.string.preset_passport_photo)) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                        FilterChip(
                            selected = state.selectedPreset == ResizePreset.CUSTOM,
                            onClick = { viewModel.selectPreset(ResizePreset.CUSTOM) },
                            leadingIcon = if (!Constants.isProUnlocked()) {
                                {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = { Text(stringResource(R.string.preset_custom)) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }

                    if (state.selectedPreset == ResizePreset.CUSTOM) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.widthInput,
                                        onValueChange = viewModel::onWidthChanged,
                                        label = { Text(stringResource(R.string.resizer_width_px)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = state.heightInput,
                                        onValueChange = viewModel::onHeightChanged,
                                        label = { Text(stringResource(R.string.resizer_height_px)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = state.maxKbInput,
                                    onValueChange = viewModel::onMaxKbChanged,
                                    label = { Text(stringResource(R.string.resizer_max_kb)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(R.string.resizer_keep_aspect),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Switch(
                                        checked = state.keepAspectRatio,
                                        onCheckedChange = viewModel::onKeepAspectRatioChanged
                                    )
                                }
                            }
                        }
                    }

                    if (state.showInvalidInputError) {
                        Text(
                            text = stringResource(R.string.resizer_invalid_input),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = { viewModel.resizeAndCompress() },
                        enabled = !state.isProcessing,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 52.dp)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.resizer_processing))
                        } else {
                            Icon(Icons.Outlined.Compress, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.resizer_process_button))
                        }
                    }

                    state.output?.let { out ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.resizer_result_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(
                                        R.string.resizer_result_stats,
                                        out.sizeKbFormatted,
                                        out.width,
                                        out.height,
                                        out.quality
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (!out.reachedTargetSize) {
                                    Text(
                                        text = stringResource(
                                            R.string.resizer_warning_min_reached,
                                            out.sizeKbFormatted
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                AsyncImage(
                                    model = File(out.filePath),
                                    contentDescription = stringResource(R.string.cd_resizer_preview),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            saveImageLauncher.launch(
                                                "AuraScan_${out.width}x${out.height}.jpg"
                                            )
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Outlined.SaveAlt, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.action_save_to_device))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val ok = ShareAndSaveUtils.shareFile(
                                                context = context,
                                                file = File(out.filePath),
                                                mimeType = "image/jpeg",
                                                chooserTitle = context.getString(R.string.action_share)
                                            )
                                            if (!ok) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        context.getString(R.string.share_failed)
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 48.dp)
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text(stringResource(R.string.action_share))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.showProPrompt) {
        ProUpsellDialog(
            title = stringResource(R.string.resizer_pro_title),
            message = stringResource(R.string.resizer_pro_message),
            onDismiss = { viewModel.dismissProPrompt() }
        )
    }
}

@Composable
private fun CropStepContent(
    imagePath: String,
    sourceWidth: Int,
    sourceHeight: Int,
    cropRect: CropRect,
    onCropChange: (CropRect) -> Unit,
    onResetCrop: () -> Unit,
    onApplyCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspect = if (sourceWidth > 0 && sourceHeight > 0) {
        (sourceWidth.toFloat() / sourceHeight.toFloat()).coerceIn(0.4f, 2.5f)
    } else {
        1f
    }
    val overlayColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.resizer_crop_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspect)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = stringResource(R.string.cd_source_preview),
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val leftPx = cropRect.left * size.width
                val topPx = cropRect.top * size.height
                val rightPx = cropRect.right * size.width
                val bottomPx = cropRect.bottom * size.height

                // Dim outside crop bounds
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset.Zero,
                    size = Size(size.width, topPx)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(0f, bottomPx),
                    size = Size(size.width, size.height - bottomPx)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(0f, topPx),
                    size = Size(leftPx, bottomPx - topPx)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(rightPx, topPx),
                    size = Size(size.width - rightPx, bottomPx - topPx)
                )
                drawRect(
                    color = overlayColor,
                    topLeft = Offset(leftPx, topPx),
                    size = Size(rightPx - leftPx, bottomPx - topPx),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        CropSliderRow(
            label = stringResource(R.string.resizer_crop_left),
            value = cropRect.left,
            valueRange = 0f..(cropRect.right - 0.1f).coerceAtLeast(0.05f),
            onValueChange = { onCropChange(cropRect.copy(left = it)) }
        )
        CropSliderRow(
            label = stringResource(R.string.resizer_crop_top),
            value = cropRect.top,
            valueRange = 0f..(cropRect.bottom - 0.1f).coerceAtLeast(0.05f),
            onValueChange = { onCropChange(cropRect.copy(top = it)) }
        )
        CropSliderRow(
            label = stringResource(R.string.resizer_crop_right),
            value = cropRect.right,
            valueRange = (cropRect.left + 0.1f).coerceAtMost(0.95f)..1f,
            onValueChange = { onCropChange(cropRect.copy(right = it)) }
        )
        CropSliderRow(
            label = stringResource(R.string.resizer_crop_bottom),
            value = cropRect.bottom,
            valueRange = (cropRect.top + 0.1f).coerceAtMost(0.95f)..1f,
            onValueChange = { onCropChange(cropRect.copy(bottom = it)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onResetCrop,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.resizer_reset_crop))
            }
            Button(
                onClick = onApplyCrop,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.resizer_apply_crop))
            }
        }
    }
}

@Composable
private fun CropSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(64.dp)
        )
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
    }
}
