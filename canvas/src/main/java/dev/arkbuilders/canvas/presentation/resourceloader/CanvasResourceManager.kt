package dev.arkbuilders.canvas.presentation.resourceloader

import java.nio.file.Path

interface CanvasResourceManager {
    suspend fun loadResource(path: Path)
    suspend fun saveResource(path: Path)
}