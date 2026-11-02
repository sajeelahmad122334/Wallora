package com.wallora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DriveImageUrlsTest {
    @Test
    fun drivePreviewUrlsBecomeDirectThumbnailAndOriginalUrls() {
        val id = "1AbCdEfGhIjKlMnOpQrStUvWxYz"
        val url = "https://drive.google.com/file/d/$id/view?usp=sharing"

        assertEquals("https://lh3.googleusercontent.com/d/$id=w1000", DriveImageUrls.thumbnail(url))
        assertEquals("https://drive.google.com/uc?export=download&id=$id", DriveImageUrls.original(url))
    }

    @Test
    fun keepsDriveResourceKeyOnDirectLinks() {
        val id = "1AbCdEfGhIjKlMnOpQrStUvWxYz"
        val url = "https://drive.google.com/uc?id=$id&resourcekey=key123"

        assertEquals("https://lh3.googleusercontent.com/d/$id=w1000?resourcekey=key123", DriveImageUrls.thumbnail(url))
        assertEquals("https://drive.google.com/uc?export=download&id=$id&resourcekey=key123", DriveImageUrls.original(url))
    }
}
