package dev.arkbuilders.canvas.presentation.resourceloader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.toSize
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dev.arkbuilders.canvas.presentation.drawing.EditManager
import timber.log.Timber
import java.nio.file.Path
import kotlin.io.path.outputStream
import kotlin.system.measureTimeMillis

class BitmapResourceManager(
    val context: Context,
    val editManager: EditManager
) : CanvasResourceManager {

    private val glideBuilder = Glide
        .with(context)
        .asBitmap()
        .skipMemoryCache(true)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
    override suspend fun loadResource(path: Path) {
        loadImage(path)
    }

    private fun loadImage(
        resourcePath: Path,
    ) {
        glideBuilder
            .load(resourcePath.toFile())
            .loadInto()
    }

    override suspend fun saveResource(path: Path) {
        val combinedBitmap = getEditedImage()
        path.outputStream().use { out ->
            combinedBitmap.asAndroidBitmap()
                .compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(path.toString()),
            arrayOf("image/*")
        ) { _, _ -> }
    }

    fun getEditedImage(): ImageBitmap {
        val size = editManager.imageSize
        var bitmap = ImageBitmap(
            size.width,
            size.height,
            ImageBitmapConfig.Argb8888
        )
        var pathBitmap: ImageBitmap? = null
        val time = measureTimeMillis {
            editManager.apply {
                val matrix = Matrix()
                if (editManager.drawPaths.isNotEmpty()) {
                    pathBitmap = ImageBitmap(
                        size.width,
                        size.height,
                        ImageBitmapConfig.Argb8888
                    )
                    val pathCanvas = Canvas(pathBitmap!!)
                    editManager.drawPaths.forEach {
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
        }
        Timber.tag("edit-viewmodel: getEditedImage").d(
            "processing edits took ${time / 1000} s ${time % 1000} ms"
        )
        return bitmap
    }


    private fun RequestBuilder<Bitmap>.loadInto() {
        into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(
                bitmap: Bitmap,
                transition: Transition<in Bitmap>?
            ) {
                editManager.apply {
                    backgroundImage.value = bitmap.asImageBitmap()
                    setOriginalBackgroundImage(backgroundImage.value)
                    scaleToFit()
                }
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        })
    }
}