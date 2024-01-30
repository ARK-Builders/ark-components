package dev.arkbuilders.components.filepicker.callback

import java.nio.file.Path

interface OnFileItemLongClick {
    fun onLongClick(file: Path)
}