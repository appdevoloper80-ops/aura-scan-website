package com.yourname.aurascan.ui.screens.resizer

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yourname.aurascan.Constants
import com.yourname.aurascan.domain.model.CropRect
import com.yourname.aurascan.domain.model.ResizePreset
import com.yourname.aurascan.domain.resizer.ImageSizeOptimizer
import com.yourname.aurascan.util.BitmapUtils
import com.yourname.aurascan.util.ShareAndSaveUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

data class ResizerOutput(
    val filePath: String,
    val sizeBytes: Int,
    val width: Int,
    val height: Int,
    val quality: Int,
    val reachedTargetSize: Boolean,
    val timestamp: Long
) {
    val sizeKbFormatted: String
        get() = String.format(Locale.US, "%.1f", sizeBytes / 1024.0)
}

data class ResizerUiState(
    val sourceImagePath: String? = null,
    val sourceWidth: Int = 0,
    val sourceHeight: Int = 0,
    val cropRect: CropRect = CropRect(),
    val isCropStepOpen: Boolean = false,
    val selectedPreset: ResizePreset = ResizePreset.EXAM_PHOTO,
    val widthInput: String = ResizePreset.EXAM_PHOTO.defaultWidthPx.toString(),
    val heightInput: String = ResizePreset.EXAM_PHOTO.defaultHeightPx.toString(),
    val maxKbInput: String = ResizePreset.EXAM_PHOTO.defaultMaxKb.toString(),
    val keepAspectRatio: Boolean = true,
    val isProcessing: Boolean = false,
    val output: ResizerOutput? = null,
    val showProPrompt: Boolean = false,
    val showInvalidInputError: Boolean = false
)

class ResizerViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val appContext: Context
) : ViewModel() {

    private val initialSourcePath = savedStateHandle.get<String>(KEY_SOURCE_PATH)
        ?.takeIf { File(it).exists() }

    private val initialPreset = savedStateHandle.get<String>(KEY_PRESET)
        ?.let { runCatching { ResizePreset.valueOf(it) }.getOrNull() }
        ?: ResizePreset.EXAM_PHOTO

    private val _uiState = MutableStateFlow(
        ResizerUiState(
            sourceImagePath = initialSourcePath,
            cropRect = CropRect(
                left = savedStateHandle.get<Float>(KEY_CROP_LEFT) ?: 0f,
                top = savedStateHandle.get<Float>(KEY_CROP_TOP) ?: 0f,
                right = savedStateHandle.get<Float>(KEY_CROP_RIGHT) ?: 1f,
                bottom = savedStateHandle.get<Float>(KEY_CROP_BOTTOM) ?: 1f
            ),
            selectedPreset = initialPreset,
            widthInput = savedStateHandle.get<String>(KEY_WIDTH)
                ?: initialPreset.defaultWidthPx.toString(),
            heightInput = savedStateHandle.get<String>(KEY_HEIGHT)
                ?: initialPreset.defaultHeightPx.toString(),
            maxKbInput = savedStateHandle.get<String>(KEY_MAX_KB)
                ?: initialPreset.defaultMaxKb.toString(),
            keepAspectRatio = savedStateHandle.get<Boolean>(KEY_KEEP_ASPECT) ?: true
        )
    )
    val uiState: StateFlow<ResizerUiState> = _uiState.asStateFlow()

    init {
        initialSourcePath?.let { loadBounds(File(it)) }
    }

    fun onImagePicked(uri: Uri) {
        viewModelScope.launch {
            val copied = withContext(Dispatchers.IO) {
                val dir = File(appContext.cacheDir, "picker_resizer").apply { mkdirs() }
                val dest = File(dir, "src_${UUID.randomUUID()}.jpg")
                ShareAndSaveUtils.copyUriToFile(appContext, uri, dest).getOrNull()
            }
            if (copied != null) {
                savedStateHandle[KEY_SOURCE_PATH] = copied.absolutePath
                saveCropRect(CropRect())
                _uiState.update {
                    it.copy(
                        sourceImagePath = copied.absolutePath,
                        cropRect = CropRect(),
                        output = null,
                        showInvalidInputError = false
                    )
                }
                loadBounds(copied)
            }
        }
    }

    private fun loadBounds(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val bounds = BitmapUtils.readImageBounds(file)
            if (bounds != null) {
                _uiState.update {
                    it.copy(sourceWidth = bounds.first, sourceHeight = bounds.second)
                }
            }
        }
    }

    fun openCropStep() {
        if (_uiState.value.sourceImagePath != null) {
            _uiState.update { it.copy(isCropStepOpen = true) }
        }
    }

    fun closeCropStep() {
        _uiState.update { it.copy(isCropStepOpen = false) }
    }

    fun updateCropRect(cropRect: CropRect) {
        val normalized = cropRect.normalized()
        saveCropRect(normalized)
        _uiState.update { it.copy(cropRect = normalized, output = null) }
    }

    fun resetCropRect() {
        val full = CropRect()
        saveCropRect(full)
        _uiState.update { it.copy(cropRect = full, output = null) }
    }

    fun selectPreset(preset: ResizePreset) {
        if (preset == ResizePreset.CUSTOM && !Constants.isProUnlocked()) {
            _uiState.update { it.copy(showProPrompt = true) }
            return
        }
        savedStateHandle[KEY_PRESET] = preset.name
        savedStateHandle[KEY_WIDTH] = preset.defaultWidthPx.toString()
        savedStateHandle[KEY_HEIGHT] = preset.defaultHeightPx.toString()
        savedStateHandle[KEY_MAX_KB] = preset.defaultMaxKb.toString()
        _uiState.update {
            it.copy(
                selectedPreset = preset,
                widthInput = preset.defaultWidthPx.toString(),
                heightInput = preset.defaultHeightPx.toString(),
                maxKbInput = preset.defaultMaxKb.toString(),
                showInvalidInputError = false
            )
        }
    }

    fun onWidthChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        savedStateHandle[KEY_WIDTH] = digitsOnly
        val state = _uiState.value
        val newHeight = if (state.keepAspectRatio) {
            val w = digitsOnly.toIntOrNull()
            val (refW, refH) = effectiveCropSourceDimensions(state)
            if (w != null && w > 0 && refW > 0 && refH > 0) {
                ImageSizeOptimizer.computeAspectPreservedDimension(
                    changedDimension = w,
                    sourceWidth = refW,
                    sourceHeight = refH,
                    widthChanged = true
                ).toString()
            } else {
                state.heightInput
            }
        } else {
            state.heightInput
        }
        savedStateHandle[KEY_HEIGHT] = newHeight
        _uiState.update {
            it.copy(
                widthInput = digitsOnly,
                heightInput = newHeight,
                showInvalidInputError = false
            )
        }
    }

    fun onHeightChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        savedStateHandle[KEY_HEIGHT] = digitsOnly
        val state = _uiState.value
        val newWidth = if (state.keepAspectRatio) {
            val h = digitsOnly.toIntOrNull()
            val (refW, refH) = effectiveCropSourceDimensions(state)
            if (h != null && h > 0 && refW > 0 && refH > 0) {
                ImageSizeOptimizer.computeAspectPreservedDimension(
                    changedDimension = h,
                    sourceWidth = refW,
                    sourceHeight = refH,
                    widthChanged = false
                ).toString()
            } else {
                state.widthInput
            }
        } else {
            state.widthInput
        }
        savedStateHandle[KEY_WIDTH] = newWidth
        _uiState.update {
            it.copy(
                widthInput = newWidth,
                heightInput = digitsOnly,
                showInvalidInputError = false
            )
        }
    }

    fun onMaxKbChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }
        savedStateHandle[KEY_MAX_KB] = digitsOnly
        _uiState.update {
            it.copy(maxKbInput = digitsOnly, showInvalidInputError = false)
        }
    }

    fun onKeepAspectRatioChanged(keep: Boolean) {
        savedStateHandle[KEY_KEEP_ASPECT] = keep
        _uiState.update { it.copy(keepAspectRatio = keep) }
    }

    fun dismissProPrompt() {
        _uiState.update { it.copy(showProPrompt = false) }
    }

    fun resizeAndCompress() {
        val state = _uiState.value
        if (state.isProcessing) return
        val sourcePath = state.sourceImagePath ?: return
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return

        val targetW = state.widthInput.toIntOrNull()
        val targetH = state.heightInput.toIntOrNull()
        val maxKb = state.maxKbInput.toIntOrNull()

        if (targetW == null || targetW <= 0 || targetH == null || targetH <= 0 || maxKb == null || maxKb <= 0) {
            _uiState.update { it.copy(showInvalidInputError = true) }
            return
        }

        val requiresPro = ImageSizeOptimizer.requiresProForTarget(
            isCustomPreset = state.selectedPreset.isCustom,
            maxKb = maxKb,
            proUnlocked = Constants.isProUnlocked(),
            freeMinKb = Constants.FREE_MIN_RESIZE_KB
        )
        if (requiresPro) {
            _uiState.update { it.copy(showProPrompt = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, showInvalidInputError = false) }
            try {
                val outputResult = withContext(Dispatchers.IO) {
                    val decoded = BitmapUtils.decodeSampledBitmap(sourceFile, maxDimension = 2048)
                        ?: throw IllegalStateException("Could not decode image")
                    val cropped = try {
                        BitmapUtils.cropBitmap(decoded, state.cropRect)
                    } finally {
                        if (!decoded.isRecycled) decoded.recycle()
                    }

                    val optimization = try {
                        ImageSizeOptimizer.optimizeToMaxBytes(
                            targetWidth = targetW,
                            targetHeight = targetH,
                            maxBytes = maxKb * 1024
                        ) { w, h, quality ->
                            BitmapUtils.scaleAndCompressJpeg(cropped, w, h, quality)
                        }
                    } finally {
                        if (!cropped.isRecycled) cropped.recycle()
                    }

                    val outDir = File(appContext.cacheDir, "shared").apply { mkdirs() }
                    val outFile = File(outDir, "resized_${System.currentTimeMillis()}.jpg")
                    outFile.writeBytes(optimization.bytes)

                    ResizerOutput(
                        filePath = outFile.absolutePath,
                        sizeBytes = optimization.bytes.size,
                        width = optimization.finalWidth,
                        height = optimization.finalHeight,
                        quality = optimization.finalQuality,
                        reachedTargetSize = optimization.reachedTargetSize,
                        timestamp = System.currentTimeMillis()
                    )
                }

                _uiState.update { it.copy(isProcessing = false, output = outputResult) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isProcessing = false, showInvalidInputError = true) }
            }
        }
    }

    private fun effectiveCropSourceDimensions(state: ResizerUiState): Pair<Int, Int> {
        val norm = state.cropRect.normalized()
        val w = (state.sourceWidth * (norm.right - norm.left)).toInt().coerceAtLeast(1)
        val h = (state.sourceHeight * (norm.bottom - norm.top)).toInt().coerceAtLeast(1)
        return w to h
    }

    private fun saveCropRect(rect: CropRect) {
        savedStateHandle[KEY_CROP_LEFT] = rect.left
        savedStateHandle[KEY_CROP_TOP] = rect.top
        savedStateHandle[KEY_CROP_RIGHT] = rect.right
        savedStateHandle[KEY_CROP_BOTTOM] = rect.bottom
    }

    companion object {
        private const val KEY_SOURCE_PATH = "source_path"
        private const val KEY_PRESET = "preset"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_MAX_KB = "max_kb"
        private const val KEY_KEEP_ASPECT = "keep_aspect"
        private const val KEY_CROP_LEFT = "crop_left"
        private const val KEY_CROP_TOP = "crop_top"
        private const val KEY_CROP_RIGHT = "crop_right"
        private const val KEY_CROP_BOTTOM = "crop_bottom"
    }

    class Factory(
        private val appContext: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            val savedStateHandle = extras.createSavedStateHandle()
            return ResizerViewModel(savedStateHandle, appContext) as T
        }
    }
}
