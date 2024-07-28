package dev.arkbuilders.components.about.presentation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arkbuilders.components.about.R
import dev.arkbuilders.components.about.presentation.theme.ArkColor

@Composable
internal fun DonateBtn(
    modifier: Modifier,
    icon: Painter,
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        modifier = modifier,
        onClick = onClick,
        border = BorderStroke(
            width = 1.dp,
            color = ArkColor.BorderSecondary
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                .size(20.dp),
            painter = icon,
            contentDescription = "",
            tint = Color.Unspecified
        )
        Text(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ArkColor.FGSecondary
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewDonateBtn() {
    DonateBtn(
        modifier = Modifier,
        icon = painterResource(R.drawable.btc),
        text = "Donate",
        onClick = {}
    )
}