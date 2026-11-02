package com.wallora.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperQualityTest {
    @Test
    fun acceptsHighResolutionPortraitWallpaper() {
        assertTrue(WallpaperQuality.isSuitable(1080, 1920))
        assertTrue(WallpaperQuality.isSuitable(1440, 2560))
    }

    @Test
    fun rejectsSmallLandscapeAndNonPortraitImages() {
        assertFalse(WallpaperQuality.isSuitable(256, 256))
        assertFalse(WallpaperQuality.isSuitable(1920, 1080))
        assertFalse(WallpaperQuality.isSuitable(600, 900))
        assertFalse(WallpaperQuality.isSuitable(null, 1920))
    }
}
