package dev.arkbuilders.sample.extension

import android.net.Uri
import android.os.Environment

fun Uri.getAbsolutePath(): String {
    val sections = this.path?.split(":") ?: emptyList()
    if (sections.isEmpty()) return this.path ?: ""
    return Environment.getExternalStorageDirectory().path + "/" + sections[sections.size - 1]
}