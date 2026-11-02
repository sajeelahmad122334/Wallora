package com.wallora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wallora.app.ui.WalloraColors
import com.wallora.app.ui.WalloraTheme
import com.wallora.app.ui.WalloraViewModel
import com.wallora.app.ui.screens.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(9, 10, 11))
        )
        val app = application as WalloraApp
        setContent {
            val vm: WalloraViewModel = viewModel(factory = WalloraViewModel.Factory(app))
            WalloraRoot(vm, app.repository.sourceName)
        }
    }
}

private enum class Destination(val label: String) {
    Home("Home"), Categories("Categories"), Library("Favorites"), Settings("Settings")
}

private val destinationSaver = Saver<Destination, String>(
    save = { it.name },
    restore = { saved -> Destination.entries.firstOrNull { it.name == saved } ?: Destination.Home }
)

@Composable
private fun WalloraRoot(vm: WalloraViewModel, sourceName: String) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wallora_settings", android.content.Context.MODE_PRIVATE) }
    var destination by rememberSaveable(stateSaver = destinationSaver) { mutableStateOf(Destination.Home) }
    var libraryPage by rememberSaveable { mutableStateOf("Favorites") }
    var amoled by rememberSaveable { mutableStateOf(prefs.getBoolean("amoled_theme", false)) }
    val favorites by vm.favorites.collectAsState()
    val downloads by vm.downloads.collectAsState()
    val recent by vm.recentlyViewed.collectAsState()
    val preview = vm.activeWallpaper.value
    val activity = context as? ComponentActivity
    SideEffect {
        activity?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !amoled
                isAppearanceLightNavigationBars = !amoled
            }
        }
    }

    WalloraTheme(amoled = amoled) {
        BackHandler(enabled = preview != null || vm.selectedCategory.value != null) {
            if (preview != null) vm.closeWallpaper() else vm.selectedCategory.value = null
        }
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (preview == null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Destination.entries.forEach { item ->
                        val selected = destination == item
                        val icon = when (item) {
                            Destination.Home -> Icons.Default.Home
                            Destination.Categories -> Icons.Default.Explore
                            Destination.Library -> Icons.Default.Bookmarks
                            Destination.Settings -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                destination = item
                                if (item == Destination.Library) libraryPage = "Favorites"
                                if (item != Destination.Categories) vm.selectedCategory.value = null
                            },
                            icon = { Icon(icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = WalloraColors.gold,
                                selectedTextColor = WalloraColors.gold,
                                unselectedIconColor = WalloraColors.muted,
                                unselectedTextColor = WalloraColors.muted,
                                indicatorColor = WalloraColors.gold.copy(alpha = if (amoled) .18f else .14f)
                            )
                        )
                    }
                }
            }
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets).background(MaterialTheme.colorScheme.background)) {
                when (destination) {
                    Destination.Home -> HomeScreen(
                        vm,
                        favorites,
                        onCategories = { destination = Destination.Categories },
                        onCategory = { category ->
                            vm.openCategory(category)
                            destination = Destination.Categories
                        }
                    )
                    Destination.Categories -> {
                        val category = vm.selectedCategory.value
                        if (category == null) CategoriesScreen(vm, { vm.openCategory(it) })
                        else CategoryDetailScreen(vm, category, favorites) { vm.selectedCategory.value = null }
                    }
                    Destination.Library -> LibraryScreen(
                        page = libraryPage,
                        onPageChange = { page ->
                            libraryPage = page
                        },
                        favorites = favorites,
                        downloads = downloads,
                        recent = recent,
                        onOpen = vm::openWallpaper,
                        onFavorite = vm::toggleFavorite,
                        onHome = { destination = Destination.Home }
                    )
                    Destination.Settings -> SettingsScreen(
                        amoled = amoled,
                        onThemeChange = { value -> amoled = value; prefs.edit { putBoolean("amoled_theme", value) } },
                        sourceName = sourceName
                    )
                }
                preview?.let {
                    WallpaperPreview(
                        wallpaper = it,
                        favorite = favorites.any { saved -> saved.id == it.id },
                        onFavorite = { vm.toggleFavorite(it) },
                        onDownloaded = { vm.recordDownload(it) },
                        onClose = vm::closeWallpaper
                    )
                }
            }
        }
    }
}
