package dev.arkbuilders.components.utils

import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat

fun EditText.showKeyboard() {
    val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
    imm?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun EditText.closeKeyboard() {
    val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(this.windowToken, 0)
}

fun EditText.placeCursorToEnd() {
    requestFocus()
    post {
        setSelection(length())
    }
}