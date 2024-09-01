package dev.arkbuilders.components.about.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.arkbuilders.components.about.R
import dev.arkbuilders.components.about.presentation.theme.ArkColor
import dev.arkbuilders.components.about.presentation.ui.DonateBtn
import dev.arkbuilders.components.about.presentation.ui.QRCryptoDialog
import dev.arkbuilders.components.about.presentation.ui.SocialLink

@Composable
fun ArkAbout(
    modifier: Modifier = Modifier,
    appName: String,
    @DrawableRes
    appLogoResId: Int,
    versionName: String,
    privacyPolicyUrl: String
) {
    val ctx = LocalContext.current

    var btcDialogVisible by remember { mutableStateOf(false) }
    var ethDialogVisible by remember { mutableStateOf(false) }

    QRCryptoDialog(
        visible = btcDialogVisible,
        title = stringResource(R.string.about_donate_btc),
        wallet = stringResource(R.string.about_btc_wallet),
        fileName = "ArkQrBtc.jpg",
        qrBitmap = R.drawable.qr_btc
    ) {
        btcDialogVisible = false
    }

    QRCryptoDialog(
        visible = ethDialogVisible,
        title = stringResource(R.string.about_donate_eth),
        wallet = stringResource(R.string.about_eth_wallet),
        fileName = "ArkQrEth.jpg",
        qrBitmap = R.drawable.qr_eth
    ) {
        ethDialogVisible = false
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                modifier = Modifier.padding(top = 32.dp),
                painter = painterResource(id = appLogoResId),
                contentDescription = "",
                tint = Color.Unspecified
            )
            Text(
                modifier = Modifier.padding(top = 20.dp),
                text = appName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = ArkColor.TextPrimary
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = stringResource(R.string.version, versionName),
                color = ArkColor.TextTertiary
            )
            Text(
                modifier = Modifier.padding(top = 12.dp),
                text = "ARK Builders · Copyright ©2024",
                color = ArkColor.TextTertiary
            )
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SocialLink(
                    painterResource(R.drawable.ic_about_site),
                    text = stringResource(R.string.website)
                ) {
                    ctx.openLink(ctx.getString(R.string.ark_website_url))
                }
                SocialLink(
                    painterResource(R.drawable.ic_about_telegram),
                    text = stringResource(R.string.telegram)
                ) {
                    ctx.openLink(ctx.getString(R.string.ark_tg_url))
                }
                SocialLink(
                    painterResource(R.drawable.ic_about_discord),
                    text = stringResource(R.string.discord)
                ) {
                    ctx.openLink(ctx.getString(R.string.ark_discord_url))
                }
            }
            OutlinedButton(
                modifier = Modifier
                    .padding(top = 28.dp)
                    .fillMaxWidth(),
                onClick = { ctx.openLink(privacyPolicyUrl) },
                border = BorderStroke(
                    width = 1.dp,
                    color = ArkColor.BorderSecondary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.privacy_policy),
                    fontWeight = FontWeight.SemiBold,
                    color = ArkColor.FGSecondary
                )
                Icon(
                    modifier = Modifier.padding(start = 6.dp),
                    painter = painterResource(R.drawable.ic_external),
                    contentDescription = "",
                    tint = ArkColor.FGSecondary
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 20.dp),
                thickness = 1.dp,
                color = ArkColor.BorderSecondary
            )
            Column {
                Text(
                    modifier = Modifier.padding(top = 20.dp),
                    text = stringResource(R.string.about_support_us),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ArkColor.TextPrimary
                )
                Text(
                    modifier = Modifier.padding(top = 4.dp),
                    text = stringResource(R.string.about_we_greatly_appreciate_every_bit_of_support),
                    color = ArkColor.TextTertiary
                )
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    DonateBtn(
                        modifier = Modifier,
                        icon = painterResource(R.drawable.btc),
                        text = stringResource(R.string.about_donate_using_btc),
                    ) {
                        btcDialogVisible = true
                    }
                    DonateBtn(
                        modifier = Modifier.padding(start = 12.dp),
                        icon = painterResource(R.drawable.eth),
                        text = stringResource(R.string.about_donate_using_eth)
                    ) {
                        ethDialogVisible = true
                    }
                }
                Row(modifier = Modifier.padding(top = 12.dp)) {
                    DonateBtn(
                        modifier = Modifier,
                        icon = painterResource(R.drawable.ic_about_patreon),
                        text = stringResource(R.string.about_donate_on_patreon)
                    ) {
                        ctx.openLink(ctx.getString(R.string.about_ark_patreon_url))
                    }
                    DonateBtn(
                        modifier = Modifier.padding(start = 12.dp),
                        icon = painterResource(R.drawable.ic_about_coffee),
                        text = stringResource(R.string.about_buy_as_a_coffee)
                    ) {
                        ctx.openLink(ctx.getString(R.string.about_ark_buy_coffee_url))
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 20.dp),
                    thickness = 1.dp,
                    color = ArkColor.BorderSecondary
                )
                Row(modifier = Modifier.padding(top = 12.dp, bottom = 50.dp)) {
                    OutlinedButton(
                        modifier = Modifier,
                        onClick = { ctx.openLink(ctx.getString(R.string.ark_contribute_url)) },
                        border = BorderStroke(
                            width = 1.dp,
                            color = ArkColor.BorderSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = stringResource(R.string.about_discover_issues_to_work_on),
                            color = ArkColor.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    OutlinedButton(
                        modifier = Modifier.padding(start = 12.dp),
                        onClick = { },
                        border = BorderStroke(
                            width = 1.dp,
                            color = ArkColor.BorderSecondary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = false
                    ) {
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = stringResource(R.string.about_see_open_bounties),
                            color = ArkColor.TextPlaceHolder,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewArkAbout() {
    ArkAbout(
        appName = "App name",
        appLogoResId = R.drawable.qr_btc,
        versionName = "1.0.0",
        privacyPolicyUrl = ""
    )
}