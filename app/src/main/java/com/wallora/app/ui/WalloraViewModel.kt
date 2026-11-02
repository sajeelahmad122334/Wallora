package com.wallora.app.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wallora.app.WalloraApp
import com.wallora.app.data.Category
import com.wallora.app.data.UserLibraryStore
import com.wallora.app.data.Wallpaper
import com.wallora.app.data.WallpaperPage
import com.wallora.app.data.WallpaperPaging
import com.wallora.app.data.WallpaperQuality
import com.wallora.app.data.WalloraRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

data class FeedState(
    val items: List<Wallpaper> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null,
    val page: Int = 1,
    val lastPage: Int = 1
)

data class HomeState(
    val featured: List<Wallpaper> = emptyList(),
    val trending: List<Wallpaper> = emptyList(),
    val popular: List<Wallpaper> = emptyList(),
    val random: List<Wallpaper> = emptyList(),
    val latest: List<Wallpaper> = emptyList(),
    val latestPage: Int = 1,
    val latestLastPage: Int = 1,
    val latestLoadingMore: Boolean = false,
    val latestPageError: String? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class WalloraViewModel(
    private val repository: WalloraRepository,
    private val library: UserLibraryStore
) : ViewModel() {
    private var categoryRequest: Job? = null
    private var categoriesRequest: Job? = null
    private var homeRequest: Job? = null
    private var homeMoreRequest: Job? = null
    private var categoryGeneration = 0L
    private var homeGeneration = 0L
    val favorites = library.favorites.map { items -> items.filter(WallpaperQuality::isSuitable) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val downloads = library.downloads.map { items -> items.filter(WallpaperQuality::isSuitable) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentlyViewed = library.recentlyViewed.map { items -> items.filter(WallpaperQuality::isSuitable) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var homeState = androidx.compose.runtime.mutableStateOf(HomeState())
        private set
    var categories = androidx.compose.runtime.mutableStateOf<List<Category>>(emptyList())
        private set
    var categoryLoading = androidx.compose.runtime.mutableStateOf(false)
        private set
    var categoryError = androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    var categoryFeed = androidx.compose.runtime.mutableStateOf(FeedState())
        private set
    var selectedCategory = androidx.compose.runtime.mutableStateOf<Category?>(null)
        private set
    var activeWallpaper = androidx.compose.runtime.mutableStateOf<Wallpaper?>(null)
        private set
    init {
        val firstPage = repository.cachedWallpapers(perPage = HOME_PAGE_SIZE)
        val mixed = WallpaperPaging.interleaveByCategory(firstPage?.items.orEmpty())
        homeState.value = HomeState(
            featured = mixed.take(1),
            latest = mixed,
            latestPage = firstPage?.page ?: 1,
            latestLastPage = firstPage?.lastPage ?: 1,
            loading = firstPage == null
        )
        categories.value = repository.cachedCategories()
        categoryLoading.value = categories.value.isEmpty()
        loadHome()
        loadCategories()
    }

    fun loadHome() {
        homeRequest?.cancel()
        homeMoreRequest?.cancel()
        val requestId = ++homeGeneration
        homeRequest = viewModelScope.launch {
            homeState.value = homeState.value.copy(loading = true, latestLoadingMore = false, error = null, latestPageError = null)
            try {
            val latest = repository.wallpapers(sort = "newest", perPage = HOME_PAGE_SIZE, forceRefresh = true)
            if (requestId != homeGeneration) return@launch
            val mixed = WallpaperPaging.interleaveByCategory(
                latest.items.distinctBy { it.sourceId?.takeIf(String::isNotBlank) ?: it.id.toString() }
            )
            homeState.value = homeState.value.copy(
                featured = mixed.take(1),
                latest = mixed,
                latestPage = latest.page,
                latestLastPage = latest.lastPage,
                error = null
            )
            homeState.value = homeState.value.copy(loading = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d(TAG, "Loading Drive wallpaper catalog failed", error)
                if (requestId == homeGeneration) {
                    homeState.value = homeState.value.copy(loading = false, error = error.message ?: "Couldn't load wallpapers")
                }
            }
        }
    }

    fun loadMoreHome() {
        val previous = homeState.value
        if (!com.wallora.app.data.WallpaperPaging.canLoadMore(previous.loading, previous.latestLoadingMore, previous.latestPage, previous.latestLastPage)) return
        val requestId = homeGeneration
        homeState.value = previous.copy(latestLoadingMore = true, latestPageError = null)
        homeMoreRequest = viewModelScope.launch {
            try {
                val page = repository.wallpapers(page = previous.latestPage + 1, sort = "newest", perPage = HOME_PAGE_SIZE)
                if (requestId == homeGeneration) {
                    val current = homeState.value
                    homeState.value = current.copy(
                        latest = current.latest.appendUnique(page.items),
                        latestPage = page.page,
                        latestLastPage = page.lastPage,
                        latestLoadingMore = false,
                        latestPageError = null
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d(TAG, "Loading more Drive wallpapers failed", error)
                if (requestId == homeGeneration) {
                    homeState.value = homeState.value.copy(latestLoadingMore = false, latestPageError = error.message ?: "Couldn't load more wallpapers")
                }
            }
        }
    }

    fun loadCategories() {
        if (categoriesRequest?.isActive == true) return
        categoryLoading.value = true
        categoriesRequest = viewModelScope.launch {
            try {
                categories.value = repository.refreshCategories()
                categoryError.value = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d(TAG, "Loading Drive categories failed", error)
                categoryError.value = error.message ?: "Couldn't load categories"
            } finally {
                categoryLoading.value = false
            }
        }
    }

    fun openCategory(category: Category) {
        selectedCategory.value = category
        loadCategoryPage(reset = true)
    }

    fun loadCategoryPage(reset: Boolean = false) {
        val category = selectedCategory.value ?: return
        if (reset) {
            categoryRequest?.cancel()
            categoryGeneration++
        }
        val requestId = categoryGeneration
        val previous = categoryFeed.value
        if (!reset && !com.wallora.app.data.WallpaperPaging.canLoadMore(previous.loading, previous.loadingMore, previous.page, previous.lastPage)) return
        categoryFeed.value = if (reset) FeedState(loading = true) else previous.copy(loadingMore = true)
        val requestedPage = if (reset) 1 else previous.page + 1
        categoryRequest = viewModelScope.launch {
            try {
                val page = repository.categoryWallpapers(category, requestedPage)
                if (requestId != categoryGeneration || selectedCategory.value?.id != category.id) return@launch
                categoryFeed.value = if (reset) page.toFeedState() else categoryFeed.value.copy(
                    items = categoryFeed.value.items.appendUnique(page.items),
                    page = page.page,
                    lastPage = page.lastPage,
                    loadingMore = false,
                    error = null
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.d(TAG, "Loading category ${category.name} failed", error)
                if (requestId == categoryGeneration && selectedCategory.value?.id == category.id) {
                    categoryFeed.value = if (reset) FeedState(error = error.message ?: "Couldn't load this category")
                    else categoryFeed.value.copy(loadingMore = false, error = error.message ?: "Couldn't load more wallpapers")
                }
            }
        }
    }

    fun openWallpaper(wallpaper: Wallpaper) {
        activeWallpaper.value = wallpaper
        viewModelScope.launch { library.addRecent(wallpaper) }
    }
    fun closeWallpaper() { activeWallpaper.value = null }
    fun toggleFavorite(wallpaper: Wallpaper) { viewModelScope.launch { library.toggleFavorite(wallpaper) } }
    fun recordDownload(wallpaper: Wallpaper) { viewModelScope.launch { library.addDownload(wallpaper) } }

    private fun WallpaperPage.toFeedState() = FeedState(items = items, page = page, lastPage = lastPage)

    private fun List<Wallpaper>.appendUnique(newItems: List<Wallpaper>): List<Wallpaper> =
        com.wallora.app.data.WallpaperPaging.appendUnique(this, newItems)

    private companion object {
        const val TAG = "WalloraViewModel"
        const val HOME_PAGE_SIZE = 24
    }

    class Factory(private val app: WalloraApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            WalloraViewModel(app.repository, app.library) as T
    }
}
