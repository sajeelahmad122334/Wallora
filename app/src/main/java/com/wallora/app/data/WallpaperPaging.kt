package com.wallora.app.data

/** Shared paging guards and stable duplicate removal for all wallpaper feeds. */
internal object WallpaperPaging {
    fun canLoadMore(loading: Boolean, loadingMore: Boolean, page: Int, lastPage: Int): Boolean =
        !loading && !loadingMore && page < lastPage

    fun appendUnique(existing: List<Wallpaper>, incoming: List<Wallpaper>): List<Wallpaper> =
        (existing + incoming).distinctBy { wallpaper ->
            wallpaper.sourceId?.takeIf(String::isNotBlank)?.let { "source:$it" }
                ?: wallpaper.sha1?.takeIf(String::isNotBlank)?.let { "sha1:$it" }
                ?: wallpaper.originalUrl.takeIf(String::isNotBlank)?.substringBefore('#')?.let { "url:$it" }
                ?: "id:${wallpaper.id}"
        }

    /** Randomize category order, then take one wallpaper from each category per round. */
    fun interleaveByCategory(items: List<Wallpaper>): List<Wallpaper> {
        val buckets = items.groupBy { it.categoryName.orEmpty() }
            .mapValues { (_, categoryItems) -> categoryItems.toMutableList() }
        val categoryOrder = buckets.keys.shuffled()
        val mixed = ArrayList<Wallpaper>(items.size)
        while (mixed.size < items.size) {
            categoryOrder.forEach { category ->
                buckets[category]?.takeIf { it.isNotEmpty() }?.let { mixed.add(it.removeAt(0)) }
            }
        }
        return mixed
    }
}
