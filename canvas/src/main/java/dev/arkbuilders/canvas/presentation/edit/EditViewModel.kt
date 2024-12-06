package dev.arkbuilders.canvas.presentation.edit

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.toSize
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.arkbuilders.canvas.presentation.data.Preferences
import dev.arkbuilders.canvas.presentation.data.Resolution
import dev.arkbuilders.canvas.presentation.drawing.EditManager
import dev.arkbuilders.canvas.presentation.resourceloader.CanvasResourceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

class EditViewModel(
    private val primaryColor: Long,
    private val launchedFromIntent: Boolean,
    private val imagePath: Path?,
    private val imageUri: String?,
    private val maxResolution: Resolution,
    private val prefs: Preferences,
    val editManager: EditManager,
    private val bitMapResourceManager: CanvasResourceManager,
    private val svgResourceManager: CanvasResourceManager,
) : ViewModel() {
    var strokeSliderExpanded by mutableStateOf(false)
    var menusVisible by mutableStateOf(true)
    var strokeWidth by mutableStateOf(5f)
    var showSavePathDialog by mutableStateOf(false)
    val showOverwriteCheckbox = mutableStateOf(imagePath != null)
    var showExitDialog by mutableStateOf(false)
    var showMoreOptionsPopup by mutableStateOf(false)
    var imageSaved by mutableStateOf(false)
    var isSavingImage by mutableStateOf(false)
    var showEyeDropperHint by mutableStateOf(false)
    val showConfirmClearDialog = mutableStateOf(false)
    var isLoaded by mutableStateOf(false)
    var exitConfirmed = false
        private set
    val bottomButtonsScrollIsAtStart = mutableStateOf(true)
    val bottomButtonsScrollIsAtEnd = mutableStateOf(false)

    private val _usedColors = mutableListOf<Color>()
    val usedColors: List<Color> = _usedColors

    fun onBottomButtonStateChange(scrollStateValue: Int, minStateValue: Int = 0, maxStateValue: Int) {
        bottomButtonsScrollIsAtStart.value = scrollStateValue == minStateValue
        bottomButtonsScrollIsAtEnd.value = scrollStateValue == maxStateValue
    }

    private fun loadResource(path: Path) {
        viewModelScope.launch {
            if (path.name.endsWith(".png")) {
                bitMapResourceManager.loadResource(path)
            } else {
                svgResourceManager.loadResource(path)
            }
        }
    }

    private fun loadUsedColors() {
        viewModelScope.launch {
            _usedColors.addAll(prefs.readUsedColors())

            val color = if (_usedColors.isNotEmpty()) {
                _usedColors.last()
            } else {
                val defaultColor = Color.Blue

                _usedColors.add(defaultColor)
                defaultColor
            }

            editManager.setPaintColor(color)
        }
    }

    private fun initDefaults() {
        viewModelScope.launch {
            editManager.initDefaults(
                prefs.readDefaults(),
                maxResolution
            )
        }
    }

    init {
        imagePath?.let {
            loadResource(it)
        }
        initDefaults()
        loadUsedColors()
    }

    fun loadImage(context: Context) {
        isLoaded = true
        editManager.scaleToFit()
    }

    fun saveImage(path: Path) {
        viewModelScope.launch(Dispatchers.IO) {
            isSavingImage = true
            if (path.name.endsWith("svg")) {
                svgResourceManager.saveResource(path)
            } else {
                bitMapResourceManager.saveResource(path)
            }
            imageSaved = true
            isSavingImage = false
            showSavePathDialog = false
        }
    }

    fun shareImage(context: Context) =
        viewModelScope.launch(Dispatchers.IO) {
            val intent = Intent(Intent.ACTION_SEND)
            val uri = getCachedImageUri(context)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            context.apply {
                startActivity(
                    Intent.createChooser(
                        intent,
                        "Share"
                    )
                )
            }
        }

    fun getImageUri(
        context: Context,
        bitmap: Bitmap? = null,
        name: String = ""
    ) = getCachedImageUri(context, bitmap, name)

    private fun getCachedImageUri(
        context: Context,
        bitmap: Bitmap? = null,
        name: String = ""
    ): Uri {
        var uri: Uri? = null
        val imageCacheFolder = File(context.cacheDir, "images")
        val imgBitmap = bitmap ?: getEditedImage().asAndroidBitmap()
        try {
            imageCacheFolder.mkdirs()
            val file = File(imageCacheFolder, "image$name.png")
            file.outputStream().use { out ->
                imgBitmap
                    .compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Timber.tag("Cached image path").d(file.path.toString())
            uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return uri!!
    }

    fun trackColor(color: Color) {
        _usedColors.remove(color)
        _usedColors.add(color)

        val excess = _usedColors.size - KEEP_USED_COLORS
        repeat(excess) {
            _usedColors.removeFirst()
        }

        viewModelScope.launch {
            prefs.persistUsedColors(usedColors)
        }
    }

    fun toggleEyeDropper() {
        editManager.toggleEyeDropper()
    }

    fun cancelEyeDropper() {
        editManager.setPaintColor(usedColors.last())
    }

    fun applyEyeDropper(action: Int, x: Int, y: Int) {
        try {
            val bitmap = getEditedImage().asAndroidBitmap()
            val imageX = (x * editManager.bitmapScale.x).toInt()
            val imageY = (y * editManager.bitmapScale.y).toInt()
            val pixel = bitmap.getPixel(imageX, imageY)
            val color = Color(pixel)
            if (color == Color.Transparent) {
                showEyeDropperHint = true
                return
            }
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP -> {
                    trackColor(color)
                    toggleEyeDropper()
                    menusVisible = true
                }
            }
            editManager.setPaintColor(color)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getCombinedImageBitmap(): ImageBitmap {
        val size = editManager.imageSize
        val drawBitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        val combinedBitmap =
            ImageBitmap(size.width, size.height, ImageBitmapConfig.Argb8888)

        val time = measureTimeMillis {
            val backgroundPaint = Paint().also {
                it.color = editManager.backgroundColor.value
            }
            val drawCanvas = Canvas(drawBitmap)
            val combinedCanvas = Canvas(combinedBitmap)
            val matrix = Matrix().apply {
                if (editManager.rotationAngles.isNotEmpty()) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    setRotate(
                        editManager.rotationAngle.value,
                        centerX.toFloat(),
                        centerY.toFloat()
                    )
                }
            }
            combinedCanvas.drawRect(
                Rect(Offset.Zero, size.toSize()),
                backgroundPaint
            )
            combinedCanvas.nativeCanvas.setMatrix(matrix)
            editManager.backgroundImage.value?.let {
                combinedCanvas.drawImage(
                    it,
                    Offset.Zero,
                    Paint()
                )
            }
            editManager.drawPaths.forEach {
                drawCanvas.drawPath(it.path, it.paint)
            }
            combinedCanvas.drawImage(drawBitmap, Offset.Zero, Paint())
        }
        Timber.tag("edit-viewmodel: getCombinedImageBitmap").d(
            "processing edits took ${time / 1000} s ${time % 1000} ms"
        )
        return combinedBitmap
    }

    fun getEditedImage(): ImageBitmap = editManager.getEditedImage()

    fun confirmExit() = viewModelScope.launch {
        exitConfirmed = true
        isLoaded = false
        delay(2_000)
        exitConfirmed = false
        isLoaded = true
    }

    fun applyOperation() {
        editManager.applyOperation()
        menusVisible = true
    }

    fun cancelOperation() {
        editManager.apply {
            if (isRotateMode.value) {
                toggleRotateMode()
                cancelRotateMode()
                menusVisible = true
            }
            if (isCropMode.value) {
                toggleCropMode()
                cancelCropMode()
                menusVisible = true
            }
            if (isResizeMode.value) {
                toggleResizeMode()
                cancelResizeMode()
                menusVisible = true
            }
            if (isEyeDropperMode.value) {
                toggleEyeDropper()
                cancelEyeDropper()
                menusVisible = true
            }
            if (isBlurMode.value) {
                toggleBlurMode()
                blurOperation.cancel()
                menusVisible = true
            }
            scaleToFit()
        }
    }

    fun persistDefaults(color: Color, resolution: Resolution) {
        viewModelScope.launch {
            prefs.persistDefaults(color, resolution)
        }
    }

    companion object {
        private const val KEEP_USED_COLORS = 20
    }
}