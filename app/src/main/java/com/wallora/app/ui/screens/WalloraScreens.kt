package com.wallora.app.ui.screens

import android.app.WallpaperManager
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import android.util.Log
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wallora.app.R
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.wallora.app.WalloraApp
import com.wallora.app.AdsController
import com.wallora.app.data.Category
import com.wallora.app.data.Wallpaper
import com.wallora.app.ui.BrandHeader
import com.wallora.app.ui.EmptyState
import com.wallora.app.ui.SectionHeading
import com.wallora.app.ui.WalloraColors
import com.wallora.app.ui.WallpaperCard
import com.wallora.app.ui.WallpaperImage
import com.wallora.app.ui.WalloraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    vm: WalloraViewModel,
    favorites: List<Wallpaper>,
    onCategories: () -> Unit,
    onCategory: (Category) -> Unit
) {
    val state = vm.homeState.value
    val context = LocalContext.current
    val activity = context as? Activity
    val runAfterInterstitial: (() -> Unit) -> Unit = { action -> AdsController.showThen(activity, context, action) }
    val homeCategories = vm.categories.value.take(10)
    val homeListState = rememberLazyListState()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val feedColumns = (screenWidthDp / 190).coerceIn(1, 4)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        BrandHeader(
            title = "WALLORA",
            subtitle = "WALLPAPERS FOR YOUR EVERY DAY",
            onAction = { runAfterInterstitial(vm::loadHome) }
        )
        PullToRefreshBox(
            isRefreshing = state.loading && state.latest.isNotEmpty(),
            onRefresh = { runAfterInterstitial(vm::loadHome) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = homeListState,
                contentPadding = PaddingValues(bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                state.featured.firstOrNull()?.let { featured ->
                    item { FeaturedCard(featured) { vm.openWallpaper(featured) } }
                }

                if (state.error != null) {
                    item {
                        TextButton(onClick = { runAfterInterstitial(vm::loadHome) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Catalog unavailable · Retry", color = WalloraColors.gold)
                        }
                    }
                }

                item {
                    SectionHeading(
                        title = "Explore by theme",
                        actionLabel = "All categories",
                        onAction = onCategories
                    )
                }

                item {
                    BannerAdView()
                }

                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(homeCategories, key = { it.id }) { category ->
                            CategoryPill(category) { onCategory(category) }
                        }
                    }
                }
                if (state.trending.isNotEmpty()) {
                    item { SectionHeading("City & architecture",) }
                    item { WallpaperRail(state.trending, favorites, vm::toggleFavorite, vm::openWallpaper) }
                }

                if (state.popular.isNotEmpty()) {
                    item { SectionHeading("Landscapes",) }
                    item { WallpaperRail(state.popular, favorites, vm::toggleFavorite, vm::openWallpaper) }
                }

                if (state.random.isNotEmpty()) {
                    item { SectionHeading("Space discoveries",) }
                    item { WallpaperRail(state.random, favorites, vm::toggleFavorite, vm::openWallpaper) }
                }

                item { SectionHeading("Latest wallpapers",) }

                if (state.latest.isEmpty()) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            if (state.loading) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    CircularProgressIndicator(Modifier.size(18.dp), color = WalloraColors.gold, strokeWidth = 2.dp)
                                    Text("Loading wallpapers…", color = WalloraColors.muted, fontSize = 13.sp)
                                }
                            } else Text(
                                if (state.error == null) "Your wallpapers will appear here."
                                else "Couldn't load the wallpaper catalog.",
                                color = WalloraColors.muted,
                                fontSize = 13.sp
                            )
                            if (state.error != null) TextButton(onClick = { runAfterInterstitial(vm::loadHome) }) {
                                Text("Try again", color = WalloraColors.gold)
                            }
                        }
                    }
                } else {
                    items(
                        state.latest.drop(1).chunked(feedColumns),
                        key = { pair -> pair.first().id }
                    ) { pair ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { wallpaper ->
                                WallpaperCard(
                                    wallpaper,
                                    favorites.any { it.id == wallpaper.id },
                                    { vm.toggleFavorite(wallpaper) },
                                    { vm.openWallpaper(wallpaper) },
                                    Modifier.weight(1f)
                                )
                            }
                            repeat(feedColumns - pair.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }

                    item {
                        LaunchedEffect(state.latest.size, state.loading, state.latestLoadingMore, state.latestPage) {
                            if (!state.loading && !state.latestLoadingMore && state.latestPage < state.latestLastPage) {
                                vm.loadMoreHome()
                            }
                        }
                        if (state.loading || state.latestLoadingMore) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = WalloraColors.gold, strokeWidth = 2.dp)
                            }
                        } else if (state.latestPageError != null) {
                            TextButton(onClick = { runAfterInterstitial(vm::loadHome) }, modifier = Modifier.fillMaxWidth()) {
                                Text("Couldn't load more · Reload collection", color = WalloraColors.gold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerAdView() {
    val context = LocalContext.current
    val bannerUnitId = if (com.wallora.app.BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/9214589741"
    } else {
        com.wallora.app.BuildConfig.LIVE_BANNER_AD_UNIT_ID
    }
    if (com.wallora.app.BuildConfig.DEBUG) {
        Log.d("WalloraAdMob", "Debug banner uses Google's official test ad unit")
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidthDp = maxWidth.value.toInt()
        if (availableWidthDp > 0) {
            val adSize = remember(context, availableWidthDp) {
                AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, availableWidthDp)
            }
            key(adSize) {
                AndroidView(
                modifier = Modifier.width(maxWidth).height(adSize.height.dp),
                factory = { viewContext ->
                    AdView(viewContext).apply {
                        resize(adSize)
                        val request = BannerAdRequest.Builder(bannerUnitId, adSize).build()
                        loadAd(request,
                            object : com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback<com.google.android.libraries.ads.mobile.sdk.banner.BannerAd> {
                                override fun onAdLoaded(ad: com.google.android.libraries.ads.mobile.sdk.banner.BannerAd) {
                                    Log.i("WalloraAdMob", "Banner loaded: unit=$bannerUnitId, size=$adSize")
                                }

                                override fun onAdFailedToLoad(adError: com.google.android.libraries.ads.mobile.sdk.common.LoadAdError) {
                                    Log.e(
                                        "WalloraAdMob",
                                        "Banner failed: unit=$bannerUnitId, code=${adError.code}, domain=not exposed by ads-mobile-sdk 1.5.0, message=${adError.message}, responseInfo=${adError.responseInfo}, error=$adError"
                                    )
                                }
                            })
                        Log.d("WalloraAdMob", "Banner request started: unit=$bannerUnitId, widthDp=$availableWidthDp, size=$adSize")
                    }
                },
                onRelease = { adView ->
                    runCatching { adView.destroy() }
                        .onFailure { Log.w("WalloraAdMob", "Unable to destroy released banner", it) }
                }
                )
            }
        } else {
            Log.w("WalloraAdMob", "Banner skipped because available width is zero")
        }
    }
}
@Composable
private fun FeaturedCard(wallpaper: Wallpaper, onOpen: () -> Unit) {
    val height = (LocalConfiguration.current.screenHeightDp * 0.34f).dp.coerceIn(240.dp, 380.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(height)
            .clip(RoundedCornerShape(22.dp))
            .background(WalloraColors.card)
            .clickable(onClick = onOpen)
    ) {
        WallpaperImage(wallpaper, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = .12f), Color.Black.copy(alpha = .84f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.TopStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "TODAY'S DISCOVERY",
                color = WalloraColors.goldLight,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "A little more\nroom to breathe.",
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = if (LocalConfiguration.current.screenWidthDp < 360) 24.sp else 29.sp,
                lineHeight = if (LocalConfiguration.current.screenWidthDp < 360) 28.sp else 33.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
        ) {
            Text(
                wallpaper.categoryName?.uppercase() ?: "GOOGLE DRIVE",
                color = WalloraColors.goldLight,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
            Text(
                wallpaper.title ?: "Wallpaper",
                color = Color.White,
                fontFamily = FontFamily.Serif,
                fontSize = 21.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Surface(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(17.dp)
                .size(42.dp),
            shape = CircleShape,
            color = WalloraColors.gold
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    "View featured wallpaper",
                    tint = WalloraColors.onGold,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(category: Category, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = WalloraColors.surface,
        border = BorderStroke(1.dp, WalloraColors.divider)
    ) {
        Row(
            Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(WalloraColors.gold)
            )
            Text(category.name, color = WalloraColors.text, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun WallpaperRail(
    items: List<Wallpaper>,
    favorites: List<Wallpaper>,
    onFavorite: (Wallpaper) -> Unit,
    onOpen: (Wallpaper) -> Unit
) {
    val cardWidth = (LocalConfiguration.current.screenWidthDp * 0.44f).dp.coerceIn(150.dp, 220.dp)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { wallpaper ->
            WallpaperCard(
                wallpaper,
                favorites.any { it.id == wallpaper.id },
                { onFavorite(wallpaper) },
                { onOpen(wallpaper) },
                Modifier.width(cardWidth)
            )
        }
    }
}

@Composable
fun CategoriesScreen(vm: WalloraViewModel, onCategory: (Category) -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val globalCategories = vm.categories.value
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        BrandHeader(
            title = "Explore",
            subtitle = "A collection for every mood",
            onAction = { AdsController.showThen(activity, context) { vm.loadCategories() } }
        )
        BannerAdView()
        if (vm.categoryError.value != null) {
            TextButton(
                onClick = { AdsController.showThen(activity, context) { vm.loadCategories() } },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 12.dp)
            ) {
                Text("Retry categories", color = WalloraColors.gold)
            }
        } else if (vm.categoryLoading.value && vm.categories.value.isEmpty()) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth(),
                color = WalloraColors.gold,
                trackColor = WalloraColors.surface
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(globalCategories, key = { it.id }) { category ->
                CategoryTile(category, onCategory)
            }
        }
    }
}

@Composable
private fun CategoryTile(category: Category, onClick: (Category) -> Unit) {
    val context = LocalContext.current
    val accents = listOf(Color(0xFF262218), Color(0xFF18201F), Color(0xFF211B25), Color(0xFF1B2029))
    val categoryIndex = if (category.id > 0L) category.id - 1L else -category.id + 1L
    val accent = accents[(categoryIndex % accents.size).toInt()]

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1.13f)
            .clip(RoundedCornerShape(17.dp))
            .background(Brush.linearGradient(listOf(accent, WalloraColors.card)))
            .clickable { onClick(category) }
    ) {
        category.coverImageUrl?.let { cover ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cover)
                    .memoryCacheKey(cover)
                    .diskCacheKey(cover)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = "${category.name} wallpaper collection",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.wallpaper_fallback),
                error = painterResource(R.drawable.wallpaper_fallback)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = .74f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                category.name,
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Explore collection",
                color = Color.White.copy(alpha = .74f),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
fun CategoryDetailScreen(
    vm: WalloraViewModel,
    category: Category,
    favorites: List<Wallpaper>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        BrandHeader(
            category.name,
            category.wallpaperCount?.let { "$it wallpapers" } ?: "Wallpaper collection",
            onAction = onBack,
            action = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WalloraColors.gold)
                }
            }
        )
        BannerAdView()
        val state = vm.categoryFeed.value
        when {
            state.loading && state.items.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = WalloraColors.gold, strokeWidth = 2.dp)
            }

            state.error != null && state.items.isEmpty() -> Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState("Couldn't open this collection", state.error!!)
                TextButton(onClick = { AdsController.showThen(activity, context) { vm.loadCategoryPage(true) } }) {
                    Text("Try again", color = WalloraColors.gold)
                }
            }

            state.items.isEmpty() -> Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EmptyState("No wallpapers found yet", "Try again in a moment or explore another collection.")
                TextButton(onClick = { AdsController.showThen(activity, context) { vm.loadCategoryPage(reset = true) } }) {
                    Text("Try again", color = WalloraColors.gold)
                }
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.items, key = { it.id }) { wallpaper ->
                    WallpaperCard(
                        wallpaper,
                        favorites.any { it.id == wallpaper.id },
                        { vm.toggleFavorite(wallpaper) },
                        { vm.openWallpaper(wallpaper) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LaunchedEffect(state.items.size) {
                        if (state.page < state.lastPage) vm.loadCategoryPage()
                    }
                    if (state.loadingMore) Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = WalloraColors.gold, strokeWidth = 2.dp)
                    } else if (state.error != null && state.items.isNotEmpty()) TextButton(
                        onClick = { AdsController.showThen(activity, context) { vm.loadCategoryPage() } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Couldn't load more · Retry", color = WalloraColors.gold)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(
    page: String,
    onPageChange: (String) -> Unit,
    favorites: List<Wallpaper>,
    downloads: List<Wallpaper>,
    recent: List<Wallpaper>,
    onOpen: (Wallpaper) -> Unit,
    onFavorite: (Wallpaper) -> Unit,
    onHome: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val items = when (page) {
        "Downloads" -> downloads
        "Recent" -> recent
        else -> favorites
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        BrandHeader("Your library", "Saved for the moments that matter")
        BannerAdView()
        PrimaryTabRow(
            selectedTabIndex = listOf("Favorites", "Downloads", "Recent").indexOf(page).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = WalloraColors.gold,
            divider = { HorizontalDivider(color = WalloraColors.divider) }
        ) {
            listOf("Favorites", "Downloads", "Recent").forEach { tab ->
                Tab(
                    selected = page == tab,
                    onClick = { onPageChange(tab) },
                    text = { Text(tab, fontSize = 12.sp) }
                )
            }
        }
        if (items.isEmpty()) {
            val (title, body) = when (page) {
                "Downloads" -> "No downloads yet" to "Save a wallpaper and it will be kept here for easy access."
                "Recent" -> "Your story starts here" to "Wallpapers you open will appear in your recent history."
                else -> "A space for your favorites" to "Tap the small gold heart on any wallpaper to save it."
            }
            EmptyState(title, body)
            if (page == "Downloads" || page == "Favorites") TextButton(
                onClick = { AdsController.showThen(activity, context, onHome) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Explore wallpapers", color = WalloraColors.gold)
            }
        } else LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items, key = { it.id }) { wallpaper ->
                WallpaperCard(
                    wallpaper,
                    favorites.any { it.id == wallpaper.id },
                    { onFavorite(wallpaper) },
                    { onOpen(wallpaper) }
                )
            }
        }
    }
}

private fun autoIntervalLabel(minutes: Int): String = when (minutes) {
    15 -> "15 minutes"
    30 -> "30 minutes"
    60 -> "1 hour"
    360 -> "6 hours"
    720 -> "12 hours"
    1440 -> "24 hours"
    else -> "${minutes / 60} hours"
}

@Composable
fun SettingsScreen(
    amoled: Boolean,
    onThemeChange: (Boolean) -> Unit,
    sourceName: String
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wallora_settings", Context.MODE_PRIVATE) }
    var infoPage by rememberSaveable { mutableStateOf<SettingsInfoPage?>(null) }
    var auto by remember { mutableStateOf(prefs.getBoolean("auto_enabled", false)) }
    var intervalMinutes by remember {
        mutableIntStateOf(prefs.getInt("auto_interval_minutes", prefs.getInt("auto_hours", 24) * 60))
    }
    LaunchedEffect(auto, intervalMinutes) {
        val app = context.applicationContext as WalloraApp
        if (auto) app.scheduleAutoWallpaper(intervalMinutes.toLong()) else app.cancelAutoWallpaper()
    }
    BackHandler(enabled = infoPage != null) { infoPage = null }
    if (infoPage != null) {
        SettingsInfoScreen(page = infoPage!!, onBack = { infoPage = null })
        return
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 22.dp)
    ) {
        BrandHeader("Settings", "A few details, your way")
        BannerAdView()
        SettingsCard("APPEARANCE") {
            Text(
                "Choose a color style",
                color = WalloraColors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOptionCard(
                    name = "AMOLED Black / Gold",
                    colors = listOf(Color.Black, Color(0xFFD5B16B)),
                    selected = amoled,
                    onClick = { onThemeChange(true) },
                    modifier = Modifier.weight(1f)
                )
                ThemeOptionCard(
                    name = "White / Black / Gold",
                    colors = listOf(Color.White, Color(0xFF171717), Color(0xFFD5B16B)),
                    selected = !amoled,
                    onClick = { onThemeChange(false) },
                    modifier = Modifier.weight(1f)
                )
            }
            SettingsValueLine("Download quality", "Original image · saved without recompression", Icons.Default.Download)
        }
        SettingsCard("WALLPAPER SOURCE") {
            Text(
                sourceName,
                color = WalloraColors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
            Text(
                "Catalog details are built into the app. Images load from Unsplash's image CDN.",
                color = WalloraColors.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp)
            )
        }
        SettingsCard("AUTO WALLPAPER") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Automatic changes", color = WalloraColors.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(
                        "Rotate your home screen in the background",
                        color = WalloraColors.muted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Switch(
                    checked = auto,
                    onCheckedChange = {
                        auto = it
                        prefs.edit { putBoolean("auto_enabled", it) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = WalloraColors.onGold,
                        checkedTrackColor = WalloraColors.gold
                    )
                )
            }
            Text(
                if (auto) "Enabled · changes every ${autoIntervalLabel(intervalMinutes)}"
                else "Off · no wallpaper changes are scheduled",
                color = if (auto) WalloraColors.goldLight else WalloraColors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp)
            )
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    15 to "15 min",
                    30 to "30 min",
                    60 to "1 hour",
                    360 to "6 hours",
                    720 to "12 hours",
                    1440 to "24 hours"
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = intervalMinutes == value,
                        onClick = {
                            intervalMinutes = value
                            prefs.edit { putInt("auto_interval_minutes", value) }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                "Android may delay background work to protect battery. Automatic changes affect the home screen only. Turn this off to cancel scheduled changes.",
                color = WalloraColors.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
        SettingsCard("PREMIUM") {
            Text(
                "Paid plans are not available in this build. No purchase button is shown until Google Play Billing is configured.",
                color = WalloraColors.muted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
        SettingsCard("ABOUT & SUPPORT") {
            SettingLine("About Wallora", "Version ${appVersion(context)}", Icons.Default.Info) {
                infoPage = SettingsInfoPage.ABOUT
            }
            SettingLine("Privacy policy", "What stays on your device and what leaves it", Icons.Default.Policy) {
                infoPage = SettingsInfoPage.PRIVACY
            }
            SettingLine("Terms of use", "Image credits and responsible use", Icons.Default.Policy) {
                infoPage = SettingsInfoPage.TERMS
            }
            SettingLine("Help", "Browse, save, download, and set wallpapers", Icons.Default.Info) {
                infoPage = SettingsInfoPage.HELP
            }
            SettingLine("Contact & report a problem", "Email smbzaz@gmail.com", Icons.Default.Share) {
                infoPage = SettingsInfoPage.CONTACT
            }
        }
        Text(
            "Designed for the way you see the world.",
            color = WalloraColors.muted,
            fontFamily = FontFamily.Serif,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 18.dp)
        )
    }
}

@Composable
private fun ThemeOptionCard(
    name: String,
    colors: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 78.dp),
        shape = RoundedCornerShape(13.dp),
        color = if (selected) WalloraColors.gold.copy(alpha = .12f) else WalloraColors.card,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) WalloraColors.gold else WalloraColors.divider
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    Box(
                        Modifier
                            .size(17.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                1.dp,
                                if (color == Color.White || color == Color.Black) WalloraColors.divider else Color.Transparent,
                                CircleShape
                            )
                    )
                }
                if (selected) Icon(
                    Icons.Default.Check,
                    "Selected",
                    tint = WalloraColors.gold,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                name,
                color = WalloraColors.text,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsValueLine(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = WalloraColors.gold, modifier = Modifier.size(20.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(start = 13.dp)
        ) {
            Text(title, color = WalloraColors.text, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Text(subtitle, color = WalloraColors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 17.dp, vertical = 7.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(WalloraColors.surface)
    ) {
        Text(
            title,
            color = WalloraColors.gold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.3.sp,
            modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 3.dp)
        )
        content()
        Spacer(Modifier.height(7.dp))
    }
}

@Composable
private fun SettingLine(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = WalloraColors.gold, modifier = Modifier.size(20.dp))
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 13.dp)
            ) {
                Text(title, color = WalloraColors.text, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Text(subtitle, color = WalloraColors.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = WalloraColors.muted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun WallpaperPreview(
    wallpaper: Wallpaper,
    favorite: Boolean,
    onFavorite: () -> Unit,
    onDownloaded: (Wallpaper) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCropEditor by remember { mutableStateOf(false) }
    var showSetOptions by remember { mutableStateOf(false) }
    var cropRotation by remember(wallpaper.id) { mutableIntStateOf(0) }
    var cropZoom by remember(wallpaper.id) { mutableFloatStateOf(1f) }
    var cropOffsetX by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var cropOffsetY by remember(wallpaper.id) { mutableFloatStateOf(0f) }
    var cropFill by remember(wallpaper.id) { mutableStateOf(true) }
    var cropViewport by remember(wallpaper.id) { mutableStateOf(IntSize.Zero) }
    var settingWallpaper by remember { mutableStateOf(false) }
    var previewFailed by remember(wallpaper.id, wallpaper.localUri) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(wallpaper.originalUrl)
                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                    .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                    .crossfade(true)
                    .build(),
                contentDescription = wallpaper.title ?: "Wallpaper preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.wallpaper_fallback),
                error = painterResource(R.drawable.wallpaper_fallback),
                onLoading = { previewFailed = false },
                onSuccess = { previewFailed = false },
                onError = { previewFailed = true }
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = .3f), Color.Transparent, Color.Black.copy(alpha = .85f))
                        )
                    )
            )
            if (previewFailed) Text(
                "Couldn't load this image. Check your connection and try again.",
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp)
                    .background(Color.Black.copy(alpha = .65f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = .42f))
            ) {
                Icon(Icons.Default.Close, "Close preview", tint = Color.White)
            }
            if (wallpaper.isPremium) Text(
                "WALLORA PLUS",
                Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(WalloraColors.gold)
                    .padding(horizontal = 11.dp, vertical = 6.dp),
                color = WalloraColors.onGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = .8.sp
            )
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 90.dp)
            ) {
                BannerAdView()
            }
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    wallpaper.categoryName?.uppercase() ?: "WALLPAPER",
                    color = WalloraColors.goldLight,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp
                )
                Text(
                    wallpaper.title ?: "Untitled",
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (LocalConfiguration.current.screenWidthDp < 360) 22.sp else 27.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp)
                )
                Text(
                    wallpaper.resolution ?: "High resolution",
                    color = Color.White.copy(alpha = .72f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                wallpaper.attribution?.let { credit ->
                    Text(
                        credit,
                        color = WalloraColors.goldLight,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .clickable {
                                wallpaper.sourceUrl?.let { url ->
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
                                }
                            }
                    )
                }
                error?.let { Text(it, color = Color(0xFFFF9A87), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                BoxWithConstraints(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 17.dp)
                ) {
                    val compact = maxWidth < 360.dp
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RoundAction(
                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            if (favorite) "Remove favorite" else "Add favorite",
                            if (favorite) WalloraColors.gold else Color.White
                        ) { onFavorite() }
                        RoundAction(Icons.Default.Share, "Share wallpaper", Color.White) {
                            val shareText = buildString {
                                append(wallpaper.title ?: "Wallpaper")
                                wallpaper.attribution?.let { append("\n$it") }
                                    ?: wallpaper.artist?.let { append("\nBy $it") }
                                wallpaper.license?.let { append("\nLicense: $it") }
                                wallpaper.licenseUrl?.let { append("\nLicense details: $it") }
                                wallpaper.sourceUrl?.let { append("\nSource: $it") }
                                if (wallpaper.localUri == null) append("\nImage: ${wallpaper.originalUrl}")
                            }
                            scope.launch {
                                runCatching { prepareWallpaperShare(context, wallpaper) }
                                    .onSuccess { imageUri ->
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/*"
                                            putExtra(Intent.EXTRA_STREAM, imageUri)
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        runCatching {
                                            context.startActivity(Intent.createChooser(intent, "Share wallpaper"))
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "No app is available to share this wallpaper",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    .onFailure {
                                        Toast.makeText(
                                            context,
                                            it.message ?: "Couldn't prepare wallpaper to share",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                        Button(
                            enabled = !busy,
                            onClick = {
                                busy = true; error = null
                                scope.launch {
                                    runCatching { saveImage(context, wallpaper, wallpaper.originalUrl) }
                                        .onSuccess { localUri ->
                                            onDownloaded(wallpaper.copy(localUri = localUri))
                                            Toast.makeText(context, "Saved to Pictures/Wallora", Toast.LENGTH_SHORT).show()
                                        }
                                        .onFailure { error = it.message ?: "Download failed" }
                                    busy = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalloraColors.surface,
                                contentColor = WalloraColors.text
                            )
                        ) {
                            if (busy) CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = WalloraColors.gold
                            )
                            else Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp)); Text("Save", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { showCropEditor = true },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WalloraColors.gold,
                                contentColor = WalloraColors.onGold
                            )
                        ) {
                            Icon(Icons.Default.Wallpaper, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(if (compact) 3.dp else 6.dp))
                            Text(
                                if (compact) "Set" else "Set wallpaper",
                                fontSize = if (compact) 11.sp else 12.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            if (showCropEditor) {
                CropEditorDialog(
                    wallpaper = wallpaper,
                    rotation = cropRotation,
                    zoom = cropZoom,
                    offsetX = cropOffsetX,
                    offsetY = cropOffsetY,
                    fill = cropFill,
                    onRotation = { cropRotation = it },
                    onZoom = { cropZoom = it },
                    onOffset = { x, y -> cropOffsetX = x; cropOffsetY = y },
                    onFill = { cropFill = it },
                    onViewport = { cropViewport = it },
                    onDismiss = { showCropEditor = false },
                    onContinue = { showCropEditor = false; showSetOptions = true }
                )
            }
            if (showSetOptions) WallpaperTargetDialog(
                onDismiss = { showSetOptions = false },
                onSelect = { flags ->
                    showSetOptions = false
                    AdsController.showThen(context as? Activity, context) {
                        settingWallpaper = true
                        scope.launch {
                            runCatching {
                                applyEditedWallpaper(
                                    context, wallpaper, flags, cropFill, cropZoom, cropOffsetX, cropOffsetY,
                                    cropRotation, cropViewport
                                )
                            }.onSuccess { Toast.makeText(context, "Wallpaper updated", Toast.LENGTH_SHORT).show() }
                                .onFailure { error = it.message ?: "Couldn't set wallpaper" }
                            settingWallpaper = false
                        }
                    }
                }
            )
            if (settingWallpaper) Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = .7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = WalloraColors.gold)
                    Text("Preparing full-resolution wallpaper…", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = .42f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .12f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CropEditorDialog(
    wallpaper: Wallpaper,
    rotation: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    fill: Boolean,
    onRotation: (Int) -> Unit,
    onZoom: (Float) -> Unit,
    onOffset: (Float, Float) -> Unit,
    onFill: (Boolean) -> Unit,
    onViewport: (IntSize) -> Unit,
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val imageData = wallpaper.originalUrl
    val currentZoom by rememberUpdatedState(zoom)
    val currentOffsetX by rememberUpdatedState(offsetX)
    val currentOffsetY by rememberUpdatedState(offsetY)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = WalloraColors.gold)
                }
                Column(Modifier.weight(1f)) {
                    Text("Edit wallpaper", color = WalloraColors.text, fontFamily = FontFamily.Serif, fontSize = 21.sp)
                    Text("Drag to reposition · pinch to zoom", color = WalloraColors.muted, fontSize = 11.sp)
                }
                IconButton(onClick = { onRotation((rotation + 90) % 360); onOffset(0f, 0f) }) {
                    Icon(Icons.AutoMirrored.Filled.RotateRight, "Rotate image", tint = WalloraColors.gold)
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                val metrics = context.resources.displayMetrics
                val phoneAspect = metrics.widthPixels.toFloat() / metrics.heightPixels.coerceAtLeast(1)
                Box(
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxHeight()
                        .aspectRatio(phoneAspect)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .onSizeChanged(onViewport)
                        .pointerInput(imageData) {
                            detectTransformGestures { _, pan, zoomChange, _ ->
                                onZoom((currentZoom * zoomChange).coerceIn(0.5f, 4f))
                                onOffset(currentOffsetX + pan.x, currentOffsetY + pan.y)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageData)
                            .memoryCacheKey(imageData)
                            .diskCacheKey(imageData)
                            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                            .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Adjust wallpaper crop",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom
                                scaleY = zoom
                                translationX = offsetX
                                translationY = offsetY
                                rotationZ = rotation.toFloat()
                            },
                        contentScale = if (fill) ContentScale.Crop else ContentScale.Fit,
                        placeholder = painterResource(R.drawable.wallpaper_fallback),
                        error = painterResource(R.drawable.wallpaper_fallback)
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .border(1.dp, Color.White.copy(alpha = .52f), RoundedCornerShape(11.dp))
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                FilterChip(
                    selected = !fill,
                    onClick = { onFill(false); onZoom(1f); onOffset(0f, 0f) },
                    label = { Text("Fit") }
                )
                FilterChip(
                    selected = fill,
                    onClick = { onFill(true); onZoom(1f); onOffset(0f, 0f) },
                    label = { Text("Fill") }
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onZoom(1f); onOffset(0f, 0f); onRotation(0) }) {
                    Text("Reset", color = WalloraColors.goldLight)
                }
            }
            Slider(
                value = zoom,
                onValueChange = onZoom,
                valueRange = 0.5f..4f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(thumbColor = WalloraColors.gold, activeTrackColor = WalloraColors.gold)
            )
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WalloraColors.gold,
                    contentColor = WalloraColors.onGold
                ),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Continue to set", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun WallpaperTargetDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = WalloraColors.surface,
        title = { Text("Set wallpaper", fontFamily = FontFamily.Serif, color = WalloraColors.text) },
        text = {
            Text("Choose where you would like this image to appear.", color = WalloraColors.muted, fontSize = 13.sp)
        },
        confirmButton = {
            TextButton(onClick = { onSelect(WallpaperManager.FLAG_SYSTEM) }) {
                Text("Home screen", color = WalloraColors.gold)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSelect(WallpaperManager.FLAG_LOCK) }) {
                    Text("Lock screen", color = WalloraColors.goldLight)
                }
                TextButton(onClick = { onSelect(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK) }) {
                    Text("Both", color = WalloraColors.goldLight)
                }
            }
        }
    )
}

private suspend fun saveImage(context: Context, wallpaper: Wallpaper, url: String) = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val connection = openImageConnection(url)
    val allowedMimeTypes = setOf("image/jpeg", "image/png", "image/webp", "image/avif", "image/heic", "image/heif")
    val mime = connection.contentType?.substringBefore(';')?.lowercase()?.takeIf { it in allowedMimeTypes }
        ?: run { connection.disconnect(); error("The image source returned an unsupported file") }
    val extension = when (mime) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        "image/heic", "image/heif" -> "heic"
        else -> "jpg"
    }
    val safeTitle =
        wallpaper.title.orEmpty().replace(Regex("[^a-zA-Z0-9_-]"), "_").trim('_').take(80).ifBlank { "wallpaper" }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "${safeTitle}_${System.currentTimeMillis()}.$extension")
        put(MediaStore.Images.Media.MIME_TYPE, mime)
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Wallora")
        wallpaper.attribution?.let { put(MediaStore.Images.Media.DESCRIPTION, it) }
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: run { connection.disconnect(); error("Could not create a gallery item") }
    try {
        connection.inputStream.use { source ->
            resolver.openOutputStream(uri)?.use { output -> source.copyTo(output) } ?: error("Could not write image")
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        uri.toString()
    } catch (error: Exception) {
        resolver.delete(uri, null, null)
        throw error
    } finally {
        connection.disconnect()
    }
}

private suspend fun prepareWallpaperShare(context: Context, wallpaper: Wallpaper): Uri = withContext(Dispatchers.IO) {
    val connection = openImageConnection(wallpaper.originalUrl)
    val extension = when (connection.contentType?.substringBefore(';')?.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        else -> "jpg"
    }
    val directory = File(context.cacheDir, "shared_wallpapers").apply { mkdirs() }
    val file = File(directory, "wallora_${wallpaper.id}.$extension")
    try {
        connection.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
    } finally {
        connection.disconnect()
    }
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun openImageConnection(url: String): java.net.HttpURLConnection {
    val connection = URL(url).openConnection() as? java.net.HttpURLConnection
        ?: error("The image source is not an HTTP link")
    connection.connectTimeout = 15_000
    connection.readTimeout = 30_000
    connection.instanceFollowRedirects = true
    val status = connection.responseCode
    if (status !in 200..299) {
        connection.disconnect()
        error("The image source returned HTTP $status")
    }
    val contentType = connection.contentType?.substringBefore(';')?.lowercase()
    if (contentType == null || !contentType.startsWith("image/")) {
        connection.disconnect()
        error("The source did not return an image")
    }
    return connection
}

private suspend fun applyEditedWallpaper(
    context: Context,
    wallpaper: Wallpaper,
    flags: Int,
    fill: Boolean,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    rotation: Int,
    viewport: IntSize
) = withContext(Dispatchers.IO) {
    val sourceFile = File(context.cacheDir, "wallora_source_" + wallpaper.id + "_" + System.currentTimeMillis())
    try {
        val connection = openImageConnection(wallpaper.originalUrl)
        try {
            connection.inputStream.use { input -> sourceFile.outputStream().use { output -> input.copyTo(output) } }
        } finally {
            connection.disconnect()
        }

        val metrics = context.resources.displayMetrics
        val aspect = if (viewport.width > 0 && viewport.height > 0) viewport.width.toFloat() / viewport.height
        else metrics.widthPixels.coerceAtLeast(1).toFloat() / metrics.heightPixels.coerceAtLeast(1)
        val screenWidth = metrics.widthPixels.coerceAtLeast(1)
        val screenHeight = metrics.heightPixels.coerceAtLeast(1)
        val outWidth = if (screenWidth.toFloat() / screenHeight >= aspect) {
            (screenHeight * aspect).toInt().coerceAtLeast(1)
        } else screenWidth
        val outHeight = (outWidth / aspect).toInt().coerceIn(1, screenHeight)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
        check(bounds.outWidth > 0 && bounds.outHeight > 0) { "The image source returned an unreadable image" }
        val rotatedSource = rotation % 180 != 0
        val decodeWidth = if (rotatedSource) outHeight else outWidth
        val decodeHeight = if (rotatedSource) outWidth else outHeight
        var sampleSize = 1
        while (bounds.outWidth / (sampleSize * 2) >= decodeWidth &&
            bounds.outHeight / (sampleSize * 2) >= decodeHeight
        ) {
            sampleSize *= 2
        }
        val source = BitmapFactory.decodeFile(
            sourceFile.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        ) ?: error("The image source returned an unreadable image")
        val rotated = if (rotation % 360 == 0) source else {
            val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
                .also { if (it !== source) source.recycle() }
        }
        try {
            val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(output)
            canvas.drawColor(android.graphics.Color.BLACK)
            val baseScale = if (fill) maxOf(outWidth.toFloat() / rotated.width, outHeight.toFloat() / rotated.height)
            else minOf(outWidth.toFloat() / rotated.width, outHeight.toFloat() / rotated.height)
            val scale = baseScale * zoom.coerceIn(0.5f, 4f)
            val ratio = outWidth.toFloat() / viewport.width.coerceAtLeast(1)
            val dx = offsetX * ratio
            val dy = offsetY * ratio
            val drawWidth = rotated.width * scale
            val drawHeight = rotated.height * scale

            val destination = RectF(
                (outWidth - drawWidth) / 2f + dx,
                (outHeight - drawHeight) / 2f + dy,
                (outWidth + drawWidth) / 2f + dx,
                (outHeight + drawHeight) / 2f + dy
            )
            canvas.drawBitmap(
                rotated,
                null,
                destination,
                android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG)
            )
            val file = File(context.cacheDir, "wallora_crop_${wallpaper.id}_${System.currentTimeMillis()}.png")
            try {
                try {
                    file.outputStream().use { output.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    file.inputStream().use { WallpaperManager.getInstance(context).setStream(it, null, true, flags) }
                } finally {
                    output.recycle()
                }
            } finally {
                file.delete()
            }
        } finally {
            rotated.recycle()
            if (rotated !== source && !source.isRecycled) source.recycle()
        }
    } finally {
        sourceFile.delete()
    }
}
