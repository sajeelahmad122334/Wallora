package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperPagingTest {
    @Test
    fun interleavesCategoriesAndKeepsEveryWallpaperOnce() {
        val source = listOf(
            wallpaper(1, "Nature"), wallpaper(2, "Nature"),
            wallpaper(3, "Space"), wallpaper(4, "Space"),
            wallpaper(5, "Colors"), wallpaper(6, "Colors")
        )

        val mixed = WallpaperPaging.interleaveByCategory(source)

        assertEquals(source.size, mixed.size)
        assertEquals(source.map { it.id }.toSet(), mixed.map { it.id }.toSet())
        assertEquals(3, mixed.take(3).mapNotNull { it.categoryName }.toSet().size)
    }

    private fun wallpaper(id: Long, category: String) = Wallpaper(id = id, categoryName = category)
}
