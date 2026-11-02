package com.wallora.app.data

import com.google.gson.annotations.SerializedName

/** Clean generated labels without guessing what's in the image. */
internal object WallpaperTitles {
    private val extension = Regex("(?i)\\.(jpe?g|png|webp|avif|heic|heif)$")
    private val generatedName = Regex("(?i)^(pin|wallpaper|wall|img|image|dsc|pxl|photo|screenshot|file)[ _-]*[0-9]+$")
    private val digitsOnly = Regex("^[0-9]{6,}$")
    private val idLike = Regex("^[A-Za-z0-9_-]{24,}$")

    fun displayTitle(fileName: String?, categoryName: String?): String {
        val base = fileName.orEmpty().trim().replace(extension, "")
        val looksGenerated = base.isBlank() || generatedName.matches(base) || digitsOnly.matches(base) || idLike.matches(base) || base.length > 48
        if (looksGenerated) return categoryName?.takeIf(String::isNotBlank)?.let { "$it Collection" } ?: "Wallpaper"

        val cleaned = base.replace(Regex("[_-]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', '.', '_', '-')
        if (cleaned.isBlank() || cleaned.length > 48) return categoryName?.takeIf(String::isNotBlank)?.let { "$it Collection" } ?: "Wallpaper"
        return cleaned.split(' ').joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { char -> char.titlecase() }
        }
    }
}

data class Wallpaper(
    val id: Long,
    val title: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("thumbnail_url") val thumbnailUrl: String? = null,
    @SerializedName("category_id") val categoryId: Long? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    val resolution: String? = null,
    @SerializedName("is_premium") val isPremium: Boolean = false,
    val tags: List<String>? = null,
    @SerializedName("is_featured") val isFeatured: Boolean = false,
    @SerializedName("is_trending") val isTrending: Boolean = false,
    @SerializedName("downloads_count") val downloadsCount: Int = 0,
    val localUri: String? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val artist: String? = null,
    val license: String? = null,
    val licenseUrl: String? = null,
    val attribution: String? = null,
    val sha1: String? = null,
    val sourceId: String? = null
) {
    val imageAspectRatio: Float get() {
        val dimensions = resolution?.split(Regex("\\s*[×xX]\\s*"))
        val width = dimensions?.getOrNull(0)?.toFloatOrNull()
        val height = dimensions?.getOrNull(1)?.toFloatOrNull()
        return if (width != null && height != null && width > 0f && height > 0f) {
            width / height
        } else 0.62f
    }

    /** Keep catalog grids on cached thumbnails even when a full-size local download exists. */
    val previewUrl: String get() = thumbnailUrl?.takeIf(String::isNotBlank)
        ?: localUri?.takeIf(String::isNotBlank)
        ?: imageUrl.orEmpty()
    val originalUrl: String get() = imageUrl?.takeIf(String::isNotBlank) ?: previewUrl
}
data class WallpaperPage(val items: List<Wallpaper>, val page: Int, val lastPage: Int)

/** Only keep full-size portrait images that can serve as phone wallpapers. */
internal object WallpaperQuality {
    fun isSuitable(width: Int?, height: Int?): Boolean {
        if (width == null || height == null || width <= 0 || height <= 0) return false
        if (width < 600 || height < 1_000 || height <= width) return false
        val widthToHeight = width.toFloat() / height.toFloat()
        return widthToHeight in 0.40f..0.82f
    }

    fun isSuitable(wallpaper: Wallpaper): Boolean {
        val parts = wallpaper.resolution?.split(Regex("\\s*[×xX]\\s*")) ?: return false
        return isSuitable(parts.getOrNull(0)?.toIntOrNull(), parts.getOrNull(1)?.toIntOrNull())
    }
}

data class Category(
    val id: Long,
    val name: String,
    val slug: String? = null,
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("wallpaper_count") val wallpaperCount: Int? = null,
    @SerializedName("is_premium") val isPremium: Boolean = false,
)

