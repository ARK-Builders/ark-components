package dev.arkbuilders.components.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

fun Context.toast(
    @StringRes stringId: Int,
    vararg args: Any,
    moreTime: Boolean = false,
) {
    val duration = if (moreTime) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
    Toast.makeText(this, getString(stringId, *args), duration).show()
}