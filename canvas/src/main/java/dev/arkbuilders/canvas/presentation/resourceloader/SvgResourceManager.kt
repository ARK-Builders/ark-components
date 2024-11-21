package dev.arkbuilders.canvas.presentation.resourceloader

import dev.arkbuilders.canvas.presentation.drawing.EditManager
import dev.arkbuilders.canvas.presentation.utils.SVG
import java.nio.file.Path

class SvgResourceManager(
    private val editManager: EditManager,
): CanvasResourceManager {

    override suspend fun loadResource(path: Path) {
        val svgpaths = SVG.parse(path)
        svgpaths.getPaths().forEach {draw ->
            editManager.addDrawPath(draw.path)
            editManager.setPaintColor(draw.paint.color)
        }
    }

    override suspend fun saveResource(path: Path) {
        editManager.svg.generate(path)
    }
}