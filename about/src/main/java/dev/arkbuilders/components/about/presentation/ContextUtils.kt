package dev.arkbuilders.components.about.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.arkbuilders.components.about.R

internal fun Context.openLink(url: String) {
    toastIfError(this) {
        startActivity(Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)))
    }
}

internal fun Context.openEmail(mailTo: String) {
    toastIfError(this) {
        val uri = Uri.parse("mailto:$mailTo")
        startActivity(Intent(Intent.ACTION_SENDTO, uri))
    }
}

private fun toastIfError(ctx: Context, action: () -> Unit) {
    try {
        action()
    } catch (e: Throwable) {
        Toast.makeText(
            ctx,
            ctx.getString(R.string.oops_something_went_wrong),
            Toast.LENGTH_SHORT
        ).show()
    }
}