package dev.arkbuilders.canvas.presentation.resourceloader

import dev.arkbuilders.canvas.presentation.drawing.EditManager
import java.nio.file.Path

interface CanvasResourceLoader {
    suspend fun loadResourceInto(path: Path, editManager: EditManager)
    suspend fun getResource()
}