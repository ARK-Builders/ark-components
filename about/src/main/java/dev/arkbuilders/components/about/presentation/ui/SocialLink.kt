package dev.arkbuilders.components.about.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import dev.arkbuilders.components.about.presentation.theme.ArkColor

@Composable
internal fun SocialLink(painter: Painter, text: String, onClick: () -> Unit) {
    TextButton(
        modifier = Modifier.defaultMinSize(
            minWidth = 1.dp,
            minHeight = 1.dp
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        onClick = { onClick() },
        shape = RoundedCornerShape(4.dp)
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painter,
            contentDescription = text,
            tint = Color.Unspecified
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            color = ArkColor.TextTertiary
        )
    }
}