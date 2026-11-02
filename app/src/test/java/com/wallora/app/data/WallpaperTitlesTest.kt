package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class WallpaperTitlesTest {
    @Test
    fun keepsHumanReadableDriveNamesAndCleansSeparators() {
        assertEquals("Golden Sunset", WallpaperTitles.displayTitle("golden_sunset.jpg", "Nature"))
        assertEquals("Midnight Mountains", WallpaperTitles.displayTitle("midnight-mountains.webp", "Nature"))
    }

    @Test
    fun replacesGeneratedCameraAndPinNamesWithCategoryFallbacks() {
        assertEquals("Nature Collection", WallpaperTitles.displayTitle("pin-001.jpg", "Nature"))
        assertEquals("People Collection", WallpaperTitles.displayTitle("IMG_1234.jpeg", "People"))
        assertEquals("Wallpaper", WallpaperTitles.displayTitle("12345678901234567890", null))
    }
}
