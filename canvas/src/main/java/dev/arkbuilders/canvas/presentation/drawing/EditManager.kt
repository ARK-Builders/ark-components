package dev.arkbuilders.canvas.presentation.drawing

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import dev.arkbuilders.canvas.presentation.data.ImageDefaults
import dev.arkbuilders.canvas.presentation.data.Resolution
import dev.arkbuilders.canvas.presentation.edit.Operation
import dev.arkbuilders.canvas.presentation.edit.blur.BlurOperation
import dev.arkbuilders.canvas.presentation.edit.crop.CropOperation
import dev.arkbuilders.canvas.presentation.edit.crop.CropWindow
import dev.arkbuilders.canvas.presentation.edit.draw.DrawOperation
import dev.arkbuilders.canvas.presentation.edit.resize.ResizeOperation
import dev.arkbuilders.canvas.presentation.edit.rotate.RotateOperation
import dev.arkbuilders.canvas.presentation.utils.SVG
import timber.log.Timber
import java.util.Stack
import kotlin.system.measureTimeMillis

object ArkColorPalette {
    val primary: Color = Color.Green

}
class EditManager{
    private val drawPaint: MutableState<Paint> = mutableStateOf(defaultPaint())

    private val _paintColor: MutableState<Color> =
        mutableStateOf(drawPaint.value.color)
    val svg = SVG()
    val paintColor: State<Color> = _paintColor
    private val _backgroundColor = mutableStateOf(Color.Transparent)
    val backgroundColor: State<Color> = _backgroundColor

    private val erasePaint: Paint = Paint().apply {
        shader = null
        color = backgroundColor.value
        style = PaintingStyle.Stroke
        blendMode = BlendMode.SrcOut
    }

    val backgroundPaint: Paint
        get() {
            return Paint().apply {
                color = backgroundImage.value?.let {
                    Color.Transparent
                } ?: backgroundColor.value
            }
        }

    val blurIntensity = mutableStateOf(12f)

    val cropWindow = CropWindow(this)

    val drawOperation = DrawOperation(this)
    val resizeOperation = ResizeOperation(this)
    val rotateOperation = RotateOperation(this)
    val cropOperation = CropOperation(this)
    val blurOperation = BlurOperation(this)

     val currentPaint: Paint
        get() = when (true) {
            isEraseMode.value -> erasePaint
            else -> drawPaint.value
        }

    val drawPaths = Stack<DrawPath>()

    val redoPaths = Stack<DrawPath>()

    val backgroundImage = mutableStateOf<ImageBitmap?>(null)
    val backgroundImage2 = mutableStateOf<ImageBitmap?>(null)
    private val originalBackgroundImage = mutableStateOf<ImageBitmap?>(null)

    val matrix = Matrix()
    val editMatrix = Matrix()
    val backgroundMatrix = Matrix()
    val rectMatrix = Matrix()

    private val matrixScale = mutableStateOf(1f)
    var zoomScale = 1f
    lateinit var bitmapScale: ResizeOperation.Scale
        private set

    val imageSize: IntSize
        get() {
            return if (isResizeMode.value)
                backgroundImage2.value?.let {
                    IntSize(it.width, it.height)
                } ?: originalBackgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize()!!
            else
                backgroundImage.value?.let {
                    IntSize(it.width, it.height)
                } ?: resolution.value?.toIntSize() ?: drawAreaSize.value
        }

    private val _resolution = mutableStateOf<Resolution?>(null)
    val resolution: State<Resolution?> = _resolution
    var drawAreaSize = mutableStateOf(IntSize.Zero)
    val availableDrawAreaSize = mutableStateOf(IntSize.Zero)

    var invalidatorTick = mutableStateOf(0)

    private val _isEraseMode: MutableState<Boolean> = mutableStateOf(false)
    val isEraseMode: State<Boolean> = _isEraseMode

    private val _canUndo: MutableState<Boolean> = mutableStateOf(false)
    val canUndo: State<Boolean> = _canUndo

    private val _canRedo: MutableState<Boolean> = mutableStateOf(false)
    val canRedo: State<Boolean> = _canRedo

    private val _isRotateMode = mutableStateOf(false)
    val isRotateMode: State<Boolean> = _isRotateMode

    private val _isResizeMode = mutableStateOf(false)
    val isResizeMode: State<Boolean> = _isResizeMode

    private val _isEyeDropperMode = mutableStateOf(false)
    val isEyeDropperMode: State<Boolean> = _isEyeDropperMode

    private val _isBlurMode = mutableStateOf(false)
    val isBlurMode: State<Boolean> = _isBlurMode

    private val _isZoomMode = mutableStateOf(false)
    val isZoomMode: State<Boolean> = _isZoomMode
    private val _isPanMode = mutableStateOf(false)
    val isPanMode: State<Boolean> = _isPanMode

    val rotationAngle = mutableStateOf(0F)
    var prevRotationAngle = 0f

    private val editedPaths = Stack<Stack<DrawPath>>()

    val redoResize = Stack<ImageBitmap>()
    val resizes = Stack<ImageBitmap>()
    val rotationAngles = Stack<Float>()
    val redoRotationAngles = Stack<Float>()

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()

    private val _isCropMode = mutableStateOf(false)
    val isCropMode = _isCropMode

    val cropStack = Stack<ImageBitmap>()
    val redoCropStack = Stack<ImageBitmap>()

    fun applyOperation() {
        val operation: Operation =
            when (true) {
                isRotateMode.value -> rotateOperation
                isCropMode.value -> cropOperation
                isBlurMode.value -> blurOperation
                isResizeMode.value -> resizeOperation
                else -> drawOperation
            }
        operation.apply()
    }


    private fun undoOperation(operation: Operation) {
        operation.undo()
    }

    private fun redoOperation(operation: Operation) {
        operation.redo()
    }

    fun scaleToFit() {
        val viewParams = backgroundImage.value?.let {
            fitImage(
                it,
                drawAreaSize.value.width,
                drawAreaSize.value.height
            )
        } ?: run {
            fitBackground(
                imageSize,
                drawAreaSize.value.width,
                drawAreaSize.value.height
            )
        }
        matrixScale.value = viewParams.scale.x
        scaleMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        val bitmapXScale =
            imageSize.width.toFloat() / viewParams.drawArea.width.toFloat()
        val bitmapYScale =
            imageSize.height.toFloat() / viewParams.drawArea.height.toFloat()
        bitmapScale = ResizeOperation.Scale(
            bitmapXScale,
            bitmapYScale
        )
    }

    fun scaleToFitOnEdit(
        maxWidth: Int = drawAreaSize.value.width,
        maxHeight: Int = drawAreaSize.value.height
    ): ImageViewParams {
        val viewParams = backgroundImage.value?.let {
            fitImage(it, maxWidth, maxHeight)
        } ?: run {
            fitBackground(
                imageSize,
                maxWidth,
                maxHeight
            )
        }
        scaleEditMatrix(viewParams)
        updateAvailableDrawArea(viewParams.drawArea)
        return viewParams
    }

    private fun scaleMatrix(viewParams: ImageViewParams) {
        matrix.setScale(viewParams.scale.x, viewParams.scale.y)
        backgroundMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (prevRotationAngle != 0f) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            matrix.postRotate(prevRotationAngle, centerX, centerY)
        }
    }

    private fun scaleEditMatrix(viewParams: ImageViewParams) {
        editMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        backgroundMatrix.setScale(viewParams.scale.x, viewParams.scale.y)
        if (prevRotationAngle != 0f && isRotateMode.value) {
            val centerX = viewParams.drawArea.width / 2f
            val centerY = viewParams.drawArea.height / 2f
            editMatrix.postRotate(prevRotationAngle, centerX, centerY)
        }
    }

    fun setBackgroundColor(color: Color) {
        _backgroundColor.value = color
    }

    fun setImageResolution(value: Resolution) {
        _resolution.value = value
    }

    fun initDefaults(defaults: ImageDefaults, maxResolution: Resolution) {
        defaults.resolution?.let {
            _resolution.value = it
        }
        if (resolution.value == null)
            _resolution.value = maxResolution
        _backgroundColor.value = Color(defaults.colorValue)
    }

    fun getEditedImage(): ImageBitmap {
        val size = this.imageSize
        var bitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        var pathBitmap: ImageBitmap? = null
        val time = measureTimeMillis {
            val matrix = Matrix()
            if (this.drawPaths.isNotEmpty()) {
                pathBitmap = ImageBitmap(
                    size.width,
                    size.height,
                    ImageBitmapConfig.Argb8888
                )
                val pathCanvas = Canvas(pathBitmap!!)
                this.drawPaths.forEach {
                    pathCanvas.drawPath(it.path, it.paint)
                }
            }
            backgroundImage.value?.let {
                val canvas = Canvas(bitmap)
                if (prevRotationAngle == 0f && drawPaths.isEmpty()) {
                    bitmap = it
                    return@let
                }
                if (prevRotationAngle != 0f) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    matrix.setRotate(prevRotationAngle, centerX, centerY)
                }
                canvas.nativeCanvas.drawBitmap(
                    it.asAndroidBitmap(),
                    matrix,
                    null
                )
                if (drawPaths.isNotEmpty()) {
                    canvas.nativeCanvas.drawBitmap(
                        pathBitmap?.asAndroidBitmap()!!,
                        matrix,
                        null
                    )
                }
            } ?: run {
                val canvas = Canvas(bitmap)
                if (prevRotationAngle != 0f) {
                    val centerX = size.width / 2
                    val centerY = size.height / 2
                    matrix.setRotate(
                        prevRotationAngle,
                        centerX.toFloat(),
                        centerY.toFloat()
                    )
                    canvas.nativeCanvas.setMatrix(matrix)
                }
                canvas.drawRect(
                    Rect(Offset.Zero, size.toSize()),
                    backgroundPaint
                )
                if (drawPaths.isNotEmpty()) {
                    canvas.drawImage(
                        pathBitmap!!,
                        Offset.Zero,
                        Paint()
                    )
                }
            }

        }
        Timber.tag("edit-viewmodel: getEditedImage").d(
            "processing edits took ${time / 1000} s ${time % 1000} ms"
        )
        return bitmap
    }

    fun enterCropMode() {
        toggleCropMode()
        if (_isCropMode.value) {
            val bitmap = getEditedImage()
            setBackgroundImage2()
            backgroundImage.value = bitmap
            this.cropWindow.init(
                bitmap.asAndroidBitmap()
            )
            return
        }
        cancelCropMode()
        scaleToFit()
        cropWindow.close()
    }

    fun enterRotateMode() {
        toggleRotateMode()
        if (isRotateMode.value) {
            setBackgroundImage2()
            scaleToFitOnEdit()
            return
        }
        cancelRotateMode()
        scaleToFit()
    }

    fun updateAvailableDrawAreaByMatrix() {
        val drawArea = backgroundImage.value?.let {
            val drawWidth = it.width * matrixScale.value
            val drawHeight = it.height * matrixScale.value
            IntSize(
                drawWidth.toInt(),
                drawHeight.toInt()
            )
        } ?: run {
            val drawWidth = resolution.value?.width!! * matrixScale.value
            val drawHeight = resolution.value?.height!! * matrixScale.value
            IntSize(
                drawWidth.toInt(),
                drawHeight.toInt()
            )
        }
        updateAvailableDrawArea(drawArea)
    }

    fun updateAvailableDrawArea(bitmap: ImageBitmap? = backgroundImage.value) {
        if (bitmap == null) {
            resolution.value?.let {
                availableDrawAreaSize.value = it.toIntSize()
            }
            return
        }
        availableDrawAreaSize.value = IntSize(
            bitmap.width,
            bitmap.height
        )
    }

    fun updateAvailableDrawArea(area: IntSize) {
        availableDrawAreaSize.value = area
    }

    internal fun clearRedoPath() {
        redoPaths.clear()
    }

    fun toggleEyeDropper() {
        _isEyeDropperMode.value = !isEyeDropperMode.value
    }

    fun updateRevised() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    fun resizeDown(width: Int = 0, height: Int = 0) =
        resizeOperation.resizeDown(width, height) {
            backgroundImage.value = it
        }

    fun rotate(angle: Float) {
        val centerX = availableDrawAreaSize.value.width / 2
        val centerY = availableDrawAreaSize.value.height / 2
        if (isRotateMode.value) {
            rotationAngle.value += angle
            rotateOperation.rotate(
                editMatrix,
                angle,
                centerX.toFloat(),
                centerY.toFloat()
            )
            return
        }
        rotateOperation.rotate(
            matrix,
            angle,
            centerX.toFloat(),
            centerY.toFloat()
        )
    }

    fun addRotation() {
        if (canRedo.value) clearRedo()
        rotationAngles.add(prevRotationAngle)
        undoStack.add(ROTATE)
        prevRotationAngle = rotationAngle.value
        updateRevised()
    }

    private fun addAngle() {
        rotationAngles.add(prevRotationAngle)
    }

    fun addResize() {
        if (canRedo.value) clearRedo()
        resizes.add(backgroundImage2.value)
        undoStack.add(RESIZE)
        keepEditedPaths()
        updateRevised()
    }

    fun keepEditedPaths() {
        val stack = Stack<DrawPath>()
        if (drawPaths.isNotEmpty()) {
            val size = drawPaths.size
            for (i in 1..size) {
                stack.push(drawPaths.pop())
            }
        }
        editedPaths.add(stack)
    }

    fun redrawEditedPaths() {
        if (editedPaths.isNotEmpty()) {
            val paths = editedPaths.pop()
            if (paths.isNotEmpty()) {
                val size = paths.size
                for (i in 1..size) {
                    drawPaths.push(paths.pop())
                }
            }
        }
    }

    fun addCrop() {
        if (canRedo.value) clearRedo()
        cropStack.add(backgroundImage2.value)
        undoStack.add(CROP)
        updateRevised()
    }

    fun addBlur() {
        if (canRedo.value) clearRedo()
        undoStack.add(BLUR)
        updateRevised()
    }

    private fun operationByTask(task: String) = when (task) {
        ROTATE -> rotateOperation
        RESIZE -> resizeOperation
        CROP -> cropOperation
        BLUR -> blurOperation
        else -> drawOperation
    }

    fun isEligibleForUndoOrRedo(): Boolean = (
            !_isRotateMode.value &&
                    !_isResizeMode.value &&
                    !_isCropMode.value &&
                    !_isEyeDropperMode.value &&
                    !_isBlurMode.value
            )

    fun isEligibleForCropOrRotate(): Boolean {
        return (
                !_isCropMode.value &&
                        !_isResizeMode.value &&
                        !_isEyeDropperMode.value &&
                        !_isEraseMode.value &&
                        !_isBlurMode.value
                )
    }

    fun isEligibleForStrokeExpandOrErase(): Boolean = (
            !_isRotateMode.value &&
                    !_isCropMode.value &&
                    !_isResizeMode.value &&
                    !_isEyeDropperMode.value &&
                    !_isBlurMode.value
            )

    fun isEligibleForPanOrZoomMode(): Boolean = (
            !_isRotateMode.value &&
                    !_isResizeMode.value &&
                    !_isCropMode.value &&
                    !_isEyeDropperMode.value &&
                    !_isBlurMode.value &&
                    !_isEraseMode.value
            )

    fun shouldApplyOperation(): Boolean = (
            _isCropMode.value ||
                    _isRotateMode.value ||
                    _isResizeMode.value ||
                    _isBlurMode.value
            )

    fun isControlsDisabled(): Boolean = (
            !_isRotateMode.value &&
                    !_isResizeMode.value &&
                    !_isCropMode.value &&
                    !_isEyeDropperMode.value
            )

    fun shouldCancelOperation(): Boolean = (
            _isCropMode.value ||
                    _isRotateMode.value ||
                    _isResizeMode.value ||
                    _isEyeDropperMode.value ||
                    _isBlurMode.value
            )

    fun isEligibleForBlurMode() = (
            !_isRotateMode.value &&
                    !_isCropMode.value &&
                    !_isEyeDropperMode.value &&
                    !_isResizeMode.value &&
                    !_isEraseMode.value
            )

    fun isEligibleForResizeMode() = (
            !_isRotateMode.value &&
                    !_isCropMode.value &&
                    !_isEyeDropperMode.value &&
                    !_isEraseMode.value &&
                    !_isBlurMode.value
            )

    fun shouldExpandColorDialog() = (
            !_isRotateMode.value &&
                    !_isResizeMode.value &&
                    !_isCropMode.value &&
                    !_isEraseMode.value &&
                    !_isBlurMode.value
            )

    fun undo() {
        if (canUndo.value) {
            val undoTask = undoStack.pop()
            redoStack.push(undoTask)
            undoOperation(operationByTask(undoTask))
        }
        invalidatorTick.value++
        updateRevised()
    }

    fun redo() {
        if (canRedo.value) {
            val redoTask = redoStack.pop()
            undoStack.push(redoTask)
            redoOperation(operationByTask(redoTask))
            invalidatorTick.value++
            updateRevised()
        }
    }

    fun saveRotationAfterOtherOperation() {
        addAngle()
        resetRotation()
    }

    fun restoreRotationAfterUndoOtherOperation() {
        if (rotationAngles.isNotEmpty()) {
            prevRotationAngle = rotationAngles.pop()
            rotationAngle.value = prevRotationAngle
        }
    }

    fun addDrawPath(path: Path) {
        drawPaths.add(
            DrawPath(
                path,
                currentPaint.copy().apply {
                    strokeWidth = drawPaint.value.strokeWidth
                }
            )
        )
        if (canRedo.value) clearRedo()
        undoStack.add(DRAW)
    }

    fun setPaintColor(color: Color) {
        drawPaint.value.color = color
        _paintColor.value = color
    }

    private fun clearPaths() {
        drawPaths.clear()
        redoPaths.clear()
        invalidatorTick.value++
        updateRevised()
    }

    private fun clearResizes() {
        resizes.clear()
        redoResize.clear()
        updateRevised()
    }

    private fun resetRotation() {
        rotationAngle.value = 0f
        prevRotationAngle = 0f
    }

    private fun clearRotations() {
        rotationAngles.clear()
        redoRotationAngles.clear()
        resetRotation()
    }

    fun clearEdits() {
        if (_isRotateMode.value || _isResizeMode.value || _isCropMode.value || _isEyeDropperMode.value) return
        clearPaths()
        clearResizes()
        clearRotations()
        clearCrop()
        blurOperation.clear()
        undoStack.clear()
        redoStack.clear()
        restoreOriginalBackgroundImage()
        scaleToFit()
        updateRevised()
    }

    private fun clearRedo() {
        redoPaths.clear()
        redoCropStack.clear()
        redoRotationAngles.clear()
        redoResize.clear()
        redoStack.clear()
        updateRevised()
    }

    private fun clearCrop() {
        cropStack.clear()
        redoCropStack.clear()
        updateRevised()
    }

    fun setBackgroundImage2() {
        backgroundImage2.value = backgroundImage.value
    }

    fun redrawBackgroundImage2() {
        backgroundImage.value = backgroundImage2.value
    }

    fun setOriginalBackgroundImage(imgBitmap: ImageBitmap?) {
        originalBackgroundImage.value = imgBitmap
    }

    private fun restoreOriginalBackgroundImage() {
        backgroundImage.value = originalBackgroundImage.value
        updateAvailableDrawArea()
    }

    fun toggleEraseMode() {
        _isEraseMode.value = !isEraseMode.value
    }

    fun toggleRotateMode() {
        _isRotateMode.value = !isRotateMode.value
        if (isRotateMode.value) editMatrix.set(matrix)
    }

    fun toggleCropMode() {
        _isCropMode.value = !isCropMode.value
        if (!isCropMode.value) cropWindow.close()
    }

    fun toggleZoomMode() {
        _isZoomMode.value = !isZoomMode.value
    }

    fun togglePanMode() {
        _isPanMode.value = !isPanMode.value
    }

    fun cancelCropMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun cancelRotateMode() {
        rotationAngle.value = prevRotationAngle
        editMatrix.reset()
    }

    fun toggleResizeMode() {
        _isResizeMode.value = !isResizeMode.value
    }

    fun enterResizeMode() {
        if (!isEligibleForResizeMode())
            return
        toggleResizeMode()
        if (isResizeMode.value) {
            setBackgroundImage2()
            val imgBitmap = getEditedImage()
            backgroundImage.value = imgBitmap
            resizeOperation.init(
                imgBitmap.asAndroidBitmap()
            )
            return
        }
        cancelResizeMode()
        scaleToFit()
    }

    fun cancelResizeMode() {
        backgroundImage.value = backgroundImage2.value
        editMatrix.reset()
    }

    fun toggleBlurMode() {
        _isBlurMode.value = !isBlurMode.value
    }

    fun enterBlurMode(strokeSliderExpanded: Boolean) {
        if (isEligibleForBlurMode() && !strokeSliderExpanded) toggleBlurMode()
        if (isBlurMode.value) {
            setBackgroundImage2()
            backgroundImage.value = getEditedImage()
            blurOperation.init()
            return
        }
        blurOperation.cancel()
        scaleToFit()
    }

    fun setPaintStrokeWidth(strokeWidth: Float) {
        drawPaint.value.strokeWidth = strokeWidth
    }

    fun calcImageOffset(): Offset {
        val drawArea = drawAreaSize.value
        val allowedArea = availableDrawAreaSize.value
        val xOffset = ((drawArea.width - allowedArea.width) / 2f)
            .coerceAtLeast(0f)
        val yOffset = ((drawArea.height - allowedArea.height) / 2f)
            .coerceAtLeast(0f)
        return Offset(xOffset, yOffset)
    }

    fun calcCenter() = Offset(
        availableDrawAreaSize.value.width / 2f,
        availableDrawAreaSize.value.height / 2f
    )


    fun resize(
        imageBitmap: ImageBitmap,
        maxWidth: Int,
        maxHeight: Int
    ): ImageBitmap {
        val bitmap = imageBitmap.asAndroidBitmap()
        val width = bitmap.width
        val height = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (maxRatio > bitmapRatio) {
            finalWidth = (maxHeight.toFloat() * bitmapRatio).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / bitmapRatio).toInt()
        }
        return Bitmap
            .createScaledBitmap(bitmap, finalWidth, finalHeight, true)
            .asImageBitmap()
    }

    fun onResizeChanged(newSize: IntSize) {
        when (true) {
            isCropMode.value -> {
                cropWindow.updateOnDrawAreaSizeChange(newSize)
                return
            }

            isResizeMode.value -> {
                if (
                    backgroundImage.value?.width ==
                    imageSize.width &&
                    backgroundImage.value?.height ==
                    imageSize.height
                ) {
                    val editMatrixScale = scaleToFitOnEdit().scale
                    resizeOperation
                        .updateEditMatrixScale(editMatrixScale)
                }
                if (
                    resizeOperation.isApplied()
                ) {
                    resizeOperation.resetApply()
                }
                return
            }

            isRotateMode.value -> {
                scaleToFitOnEdit()
                return
            }

            isZoomMode.value -> {
                return
            }

            else -> {
                scaleToFit()
                return
            }
        }
    }

    fun fitImage(
        imageBitmap: ImageBitmap,
        maxWidth: Int,
        maxHeight: Int
    ): ImageViewParams {
        val bitmap = imageBitmap.asAndroidBitmap()
        val width = bitmap.width
        val height = bitmap.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (maxRatio > bitmapRatio) {
            finalWidth = (maxHeight.toFloat() * bitmapRatio).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / bitmapRatio).toInt()
        }
        return ImageViewParams(
            IntSize(
                finalWidth,
                finalHeight,
            ),
            ResizeOperation.Scale(
                finalWidth.toFloat() / width.toFloat(),
                finalHeight.toFloat() / height.toFloat()
            )
        )
    }

    fun fitBackground(
        resolution: IntSize,
        maxWidth: Int,
        maxHeight: Int
    ): ImageViewParams {

        val width = resolution.width
        val height = resolution.height

        val resolutionRatio = width.toFloat() / height.toFloat()
        val maxRatio = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (maxRatio > resolutionRatio) {
            finalWidth = (maxHeight.toFloat() * resolutionRatio).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / resolutionRatio).toInt()
        }
        return ImageViewParams(
            IntSize(
                finalWidth,
                finalHeight,
            ),
            ResizeOperation.Scale(
                finalWidth.toFloat() / width.toFloat(),
                finalHeight.toFloat() / height.toFloat()
            )
        )
    }

    class ImageViewParams(
        val drawArea: IntSize,
        val scale: ResizeOperation.Scale
    )

    private companion object {
        private const val DRAW = "draw"
        private const val CROP = "crop"
        private const val RESIZE = "resize"
        private const val ROTATE = "rotate"
        private const val BLUR = "blur"
    }
}

class DrawPath(
    val path: Path,
    val paint: Paint
)

fun Paint.copy(): Paint {
    val from = this
    return Paint().apply {
        alpha = from.alpha
        isAntiAlias = from.isAntiAlias
        color = from.color
        blendMode = from.blendMode
        style = from.style
        strokeWidth = from.strokeWidth
        strokeCap = from.strokeCap
        strokeJoin = from.strokeJoin
        strokeMiterLimit = from.strokeMiterLimit
        filterQuality = from.filterQuality
        shader = from.shader
        colorFilter = from.colorFilter
        pathEffect = from.pathEffect
        asFrameworkPaint().apply {
            maskFilter = from.asFrameworkPaint().maskFilter
        }
    }
}

fun defaultPaint(): Paint {
    return Paint().apply {
        color = Color.White
        strokeWidth = 14f
        isAntiAlias = true
        style = PaintingStyle.Stroke
        strokeJoin = StrokeJoin.Round
        strokeCap = StrokeCap.Round
    }
}
