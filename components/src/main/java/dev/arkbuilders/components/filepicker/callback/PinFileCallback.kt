package dev.arkbuilders.components.filepicker.callback

import java.nio.file.Path

interface PinFileCallback {
    fun onPinFileClick(file: Path)
}