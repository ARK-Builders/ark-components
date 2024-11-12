package dev.arkbuilders.canvas.presentation.edit

interface Operation {
    fun apply()

    fun undo()

    fun redo()
}
