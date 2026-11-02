package com.wallora.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wallora.app.R
import com.wallora.app.data.Wallpaper

private val WalloraTypography = Typography().copy(
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = .1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = .1.sp
    )
)

@Immutable
data class WalloraPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val gold: Color,
    val goldLight: Color,
    val onGold: Color,
    val text: Color,
    val muted: Color,
    val divider: Color
)

private val AmoledPalette = WalloraPalette(
    background = Color.Black,
    surface = Color(0xFF111111),
    card = Color(0xFF191919),
    gold = Color(0xFFD5B16B),
    goldLight = Color(0xFFE9D39B),
    onGold = Color(0xFF090A0B),
    text = Color(0xFFF5F1E8),
    muted = Color(0xFFA4A4A0),
    divider = Color(0xFF292929)
)

private val LightPalette = WalloraPalette(
    background = Color.White,
    surface = Color(0xFFF6F5F2),
    card = Color(0xFFEFEDEA),
    gold = Color(0xFFB38328),
    goldLight = Color(0xFF966A1C),
    onGold = Color(0xFF171717),
    text = Color(0xFF171717),
    muted = Color(0xFF626262),
    divider = Color(0xFFE0DED9)
)

private val LocalWalloraPalette = staticCompositionLocalOf { AmoledPalette }

/** Theme-aware colors used by the existing screens and shared components. */
object WalloraColors {
    val background: Color @Composable get() = LocalWalloraPalette.current.background
    val surface: Color @Composable get() = LocalWalloraPalette.current.surface
    val card: Color @Composable get() = LocalWalloraPalette.current.card
    val gold: Color @Composable get() = LocalWalloraPalette.current.gold
    val goldLight: Color @Composable get() = LocalWalloraPalette.current.goldLight
    val onGold: Color @Composable get() = LocalWalloraPalette.current.onGold
    val text: Color @Composable get() = LocalWalloraPalette.current.text
    val muted: Color @Composable get() = LocalWalloraPalette.current.muted
    val divider: Color @Composable get() = LocalWalloraPalette.current.divider
}

@Composable
fun WalloraTheme(amoled: Boolean = true, content: @Composable () -> Unit) {
    val palette = if (amoled) AmoledPalette else LightPalette
    CompositionLocalProvider(LocalWalloraPalette provides palette) {
        val scheme = if (amoled) {
            darkColorScheme(
                primary = palette.gold,
                onPrimary = palette.onGold,
                secondary = palette.goldLight,
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.text,
                onBackground = palette.text,
                surfaceVariant = palette.card,
                onSurfaceVariant = palette.muted
            )
        } else {
            lightColorScheme(
                primary = palette.gold,
                onPrimary = palette.onGold,
                secondary = palette.goldLight,
                background = palette.background,
                surface = palette.surface,
                onSurface = palette.text,
                onBackground = palette.text,
                surfaceVariant = palette.card,
                onSurfaceVariant = palette.muted
            )
        }
        MaterialTheme(colorScheme = scheme, typography = WalloraTypography, content = content)
    }
}

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Int = 40) {
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape(13.dp)).background(WalloraColors.card)
            .border(1.dp, WalloraColors.gold.copy(alpha = .38f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.wallora_logo),
            contentDescription = "Wallora logo",
            modifier = Modifier.size((size * .66f).dp)
        )
    }
}

@Composable
fun BrandHeader(
    title: String,
    subtitle: String? = null,
    onAction: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            Modifier.weight(1f).padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BrandMark()
            Column(Modifier.weight(1f)) {
                Text(title, color = WalloraColors.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Serif, letterSpacing = .2.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                subtitle?.let { Text(it, color = WalloraColors.muted, fontSize = 11.sp, letterSpacing = .25.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
            }
        }
        if (action != null) action() else if (onAction != null) {
            Surface(onClick = onAction, modifier = Modifier.size(44.dp), shape = CircleShape, color = WalloraColors.card, border = BorderStroke(1.dp, WalloraColors.divider)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("↻", color = WalloraColors.gold, fontSize = 20.sp)
                }
            }
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = WalloraColors.text, fontSize = 18.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
        if (actionLabel != null && onAction != null) TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
            Text(actionLabel, color = WalloraColors.gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun WallpaperImage(wallpaper: Wallpaper, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val context = LocalContext.current
    Box(modifier) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(wallpaper.previewUrl)
                .memoryCacheKey(wallpaper.previewUrl)
                .diskCacheKey(wallpaper.previewUrl)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                .crossfade(true)
                .build(),
            contentDescription = wallpaper.title ?: "Wallpaper",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            placeholder = painterResource(R.drawable.wallpaper_fallback),
            error = painterResource(R.drawable.wallpaper_fallback)
        )
    }
}

@Composable
fun WallpaperCard(
    wallpaper: Wallpaper,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(wallpaper.imageAspectRatio).clip(RoundedCornerShape(15.dp))
                .clickable(onClick = onOpen)
        ) {
            WallpaperImage(wallpaper, Modifier.fillMaxSize().clip(RoundedCornerShape(15.dp)), contentScale = ContentScale.Fit)
            Surface(
                onClick = onFavorite,
                modifier = Modifier.align(Alignment.TopEnd).padding(9.dp).size(33.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = .52f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = .12f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save to favorites", tint = if (favorite) WalloraColors.gold else Color.White, modifier = Modifier.size(17.dp))
                }
            }
            if (wallpaper.isPremium) {
                Text("PLUS", Modifier.align(Alignment.BottomStart).padding(9.dp).clip(RoundedCornerShape(5.dp)).background(WalloraColors.gold).padding(horizontal = 7.dp, vertical = 3.dp), color = WalloraColors.onGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
            }
        }
        Text(wallpaper.title ?: "Untitled", color = WalloraColors.text, fontWeight = FontWeight.Medium, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 2.dp, top = 9.dp))
        Text(wallpaper.categoryName ?: wallpaper.resolution.orEmpty(), color = WalloraColors.muted, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(start = 2.dp, top = 3.dp))
    }
}

@Composable
fun EmptyState(title: String, body: String, icon: @Composable () -> Unit = {
    Icon(Icons.Default.ImageNotSupported, null, tint = WalloraColors.gold.copy(alpha = .78f), modifier = Modifier.size(28.dp))
}) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(66.dp).clip(CircleShape).background(WalloraColors.card), contentAlignment = Alignment.Center) { icon() }
        Text(title, color = WalloraColors.text, fontFamily = FontFamily.Serif, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(body, color = WalloraColors.muted, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
