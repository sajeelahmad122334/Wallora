package com.wallora.app.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.wallora.app.ui.WalloraColors

enum class SettingsInfoPage(val title: String, val subtitle: String) {
    ABOUT("About Wallora", "A calmer way to find your next wallpaper"),
    PRIVACY("Privacy", "A clear summary of how this build handles data"),
    TERMS("Terms of use", "Understand image ownership and license terms"),
    HELP("Help", "A few quick answers"),
    CONTACT("Contact", "Send feedback using an app on your phone")
}

@Composable
fun SettingsInfoScreen(page: SettingsInfoPage, onBack: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WalloraColors.gold)
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(page.title, color = WalloraColors.text, fontFamily = FontFamily.Serif, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(page.subtitle, color = WalloraColors.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        BannerAdView()
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (page) {
                SettingsInfoPage.ABOUT -> AboutContent(context)
                SettingsInfoPage.PRIVACY -> PrivacyContent()
                SettingsInfoPage.TERMS -> TermsContent(context)
                SettingsInfoPage.HELP -> HelpContent(context)
                SettingsInfoPage.CONTACT -> ContactContent(context)
            }
        }
    }
}

@Composable
private fun AboutContent(context: Context) {
    InfoHeading("Wallora")
    InfoBody("Wallora is a native Android wallpaper browser with an offline-ready catalog and Unsplash image delivery. Browse categories, preview wallpapers, save favorites, download images, and set a wallpaper from your device.")
    InfoFact("Version", appVersion(context))
    InfoFact("Image catalog", "Bundled catalog · Unsplash CDN")
    InfoFact("Storage", "Favorites and history stay on this device")
    InfoBody("Wallora does not include a paid subscription in this build. Wallpaper catalog details are bundled with the app; image files are delivered by Unsplash.")
}

@Composable
private fun PrivacyContent() {
    InfoHeading("Data in this build")
    InfoBody("Wallora does not ask you to create an account and does not send favorites, recent items, or download history to a Wallora server. Those library entries and app preferences are stored locally on your phone.")
    InfoHeading("Browsing and image requests")
    InfoBody("Wallpaper names and categories are bundled in the app and can be browsed offline. When an image is not cached, Wallora requests its image file from Unsplash's CDN, which receives normal network information such as your IP address. Ad requests are handled by the Google Mobile Ads SDK.")
    InfoHeading("Downloads and wallpaper setting")
    InfoBody("When you save an image, Wallora downloads it to Pictures/Wallora through Android MediaStore. The image credit and license details are retained in the gallery description when Android supports it. Setting a wallpaper sends the selected image to Android's WallpaperManager on your device.")
    InfoHeading("Controls")
    InfoBody("You can remove favorites from the Favorites tab and clear app data from Android Settings to remove locally stored preferences and history. Gallery downloads are managed separately in your Photos or Gallery app. Image files are fetched from Unsplash's CDN when previewed, downloaded, or set as wallpaper.")
}

@Composable
private fun TermsContent(context: Context) {
    InfoHeading("Image licenses")
    InfoBody("Images in the catalog are delivered by Unsplash and remain the work of their creators. Review the applicable Unsplash License and image-specific terms before reusing an image outside personal wallpaper use.")
    InfoHeading("Image catalog")
    InfoBody("Wallora bundles catalog metadata and requests image files from Unsplash's CDN. Image availability and CDN delivery are controlled by the image provider.")
    InfoHeading("Use of the service")
    InfoBody("Use images only in ways permitted by their owners and applicable law. Wallora provides a read-only catalog and does not make third-party works exclusive. This in-app page is a product notice; the app publisher should add a reviewed, jurisdiction-specific legal agreement before public release.")
}

@Composable
private fun HelpContent(context: Context) {
    InfoHeading("Browse collections")
    InfoBody("Choose Categories to browse the collections included in the app. Open any collection to see its wallpapers, or scroll the home feed to discover images from across the catalog.")
    InfoHeading("Save and download")
    InfoBody("Tap the heart on a card or in preview to save a favorite. In preview, Save writes the original image to Pictures/Wallora. The Downloads tab keeps a local list of items saved from Wallora.")
    InfoHeading("Set a wallpaper")
    InfoBody("Open a wallpaper and choose Set wallpaper, then select Home screen, Lock screen, or Both. Android applies the image using its native wallpaper service.")
    InfoHeading("Automatic changes")
    InfoBody("Choose an interval in Settings. Android WorkManager schedules home-screen changes and may delay them to save battery. This version does not schedule lock-screen changes.")
}

@Composable
private fun ContactContent(context: Context) {
    InfoHeading("We're here to help")
    InfoBody("Contact the Wallora team or report an issue. Your email app will open with the support address and a useful subject already filled in.")
    InfoAction("Contact Us") { composeSupportEmail(context, "Wallora support") }
    InfoAction("Report a Problem") { composeSupportEmail(context, "Wallora problem report") }
    InfoFact("Email", SUPPORT_EMAIL)
}

private const val SUPPORT_EMAIL = "smbzaz@gmail.com"

private fun composeSupportEmail(context: Context, subject: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", SUPPORT_EMAIL, null)).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, "Wallora version ${appVersion(context)}\n\nPlease describe your message or the steps to reproduce the problem:\n")
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "No email app is available", Toast.LENGTH_SHORT).show() }
}

@Composable
private fun InfoHeading(text: String) {
    Text(text, color = WalloraColors.goldLight, fontFamily = FontFamily.Serif, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InfoBody(text: String) {
    Text(text, color = WalloraColors.muted, fontSize = 13.sp, lineHeight = 20.sp)
}

@Composable
private fun InfoFact(label: String, value: String) {
    Column(Modifier.fillMaxWidth().background(WalloraColors.surface, MaterialTheme.shapes.medium).padding(14.dp)) {
        Text(label.uppercase(), color = WalloraColors.gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = WalloraColors.text, fontSize = 14.sp)
    }
}

@Composable
private fun InfoLink(context: Context, label: String, url: String) {
    InfoAction(label) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
            .onFailure { Toast.makeText(context, "Couldn't open the link", Toast.LENGTH_SHORT).show() }
    }
}

@Composable
private fun InfoAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = WalloraColors.surface, contentColor = WalloraColors.gold),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(label)
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(start = 8.dp))
    }
}

fun openExternalPage(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
        .onFailure { Toast.makeText(context, "Couldn't open the link", Toast.LENGTH_SHORT).show() }
}

fun appVersion(context: Context): String = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
}.getOrDefault("1.0.0")
