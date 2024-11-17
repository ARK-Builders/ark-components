package dev.arkbuilders.canvas.presentation.resourceloader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposePath
import dev.arkbuilders.canvas.presentation.drawing.DrawPath
import dev.arkbuilders.canvas.presentation.drawing.EditManager
import dev.arkbuilders.canvas.presentation.graphics.SVG
import java.nio.file.Path

class SvgResourceManager(
    private val editManager: EditManager,
): CanvasResourceManager {

    override suspend fun loadResource(path: Path) {
        val svgpaths = SVG.parse(path)
        svgpaths.getPaths().forEach {
            val draw = DrawPath(
                path = it.path.asComposePath(),
                paint = Paint().apply {
                    color = Color(it.paint.color)
                }
            )
            editManager.addDrawPath(draw.path)
            editManager.setPaintColor(draw.paint.color)
        }
    }

    override suspend fun saveResource(path: Path) {
        TODO("Not yet implemented")
    }
}