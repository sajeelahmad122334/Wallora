package com.wallora.app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.wallora.app.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Apps Script backed catalog. Small result pages and categories are cached in app-private storage. */
class WalloraRepository(context: Context) {
    private val cacheFile = File(context.filesDir, "wallora_drive_catalog.json")
    private val gson = Gson()
    private val requestLocks = ConcurrentHashMap<String, Mutex>()
    private val pageCache = linkedMapOf<String, WallpaperPage>()
    private val pageTokens = mutableMapOf<String, String?>()
    private val pageVersions = mutableMapOf<String, Long>()
    private var categoriesVersion = 0L
    @Volatile private var categoryCache: List<Category> = emptyList()

    val sourceName: String = "Google Drive"

    init { readCache() }

    fun cachedWallpapers(page: Int = 1, categoryId: Long? = null, perPage: Int = 24): WallpaperPage? =
        pageCache[pageKey(page, categoryId, perPage, "newest")]

    fun cachedCategories(): List<Category> = categoryCache

    suspend fun wallpapers(
        page: Int = 1,
        categoryId: Long? = null,
        search: String? = null,
        sort: String? = null,
        perPage: Int = 30,
        forceRefresh: Boolean = false
    ): WallpaperPage {
        val key = pageKey(page, categoryId, perPage, sort)
        val cachedAtStart = pageCache[key]
        val versionAtStart = pageVersions[key] ?: 0L
        if (!forceRefresh) cachedAtStart?.let { return it }
        return lockFor(key).withLock {
            pageCache[key]?.let { updated ->
                if (!forceRefresh || (pageVersions[key] ?: 0L) > versionAtStart) return@withLock updated
            }
            val tokenKey = tokenKey(categoryId, perPage, sort)
            // Apps Script versions may use either Drive page tokens or ordinary page numbers.
            val pageToken = if (page <= 1) null else pageTokens[tokenPageKey(tokenKey, page)]
            val response = request(
                action = "wallpapers",
                page = page,
                categoryId = categoryId,
                search = search,
                perPage = perPage,
                sort = sort,
                pageToken = pageToken
            )
            val items = parseWallpapers(response)
            val nextToken = response.firstString("nextPageToken", "next_page_token", "nextToken", "next_token")
            pageTokens[tokenPageKey(tokenKey, 1)] = null
            pageTokens[tokenPageKey(tokenKey, page.coerceAtLeast(1))] = pageToken
            if (nextToken != null) pageTokens[tokenPageKey(tokenKey, page + 1)] = nextToken
            val lastPage = response.optInt("lastPage", response.optInt("last_page", response.optInt("totalPages", response.optInt("total_pages", if (nextToken != null) page + 1 else page))))
            WallpaperPage(items, page.coerceAtLeast(1), lastPage.coerceAtLeast(page)).also {
                pageCache[key] = it
                pageVersions[key] = (pageVersions[key] ?: 0L) + 1L
                writeCache()
            }
        }
    }

    suspend fun categoryWallpapers(category: Category, page: Int = 1, perPage: Int = 30): WallpaperPage =
        wallpapers(page = page, categoryId = category.id, perPage = perPage)

    suspend fun randomWallpaper(): Wallpaper? {
        val cached = pageCache.values.asSequence().flatMap { it.items.asSequence() }.toList()
        if (cached.isNotEmpty()) return cached.randomOrNull()
        return wallpapers(perPage = 24).items.randomOrNull()
    }

    suspend fun categories(): List<Category> {
        if (categoryCache.isNotEmpty()) return categoryCache
        return refreshCategories()
    }

    suspend fun refreshCategories(): List<Category> {
        val versionAtStart = categoriesVersion
        return lockFor("categories").withLock {
            if (categoriesVersion > versionAtStart) return@withLock categoryCache
            val response = request(action = "categories", page = 1, categoryId = null, search = null, perPage = 500, sort = null, pageToken = null)
            parseCategories(response).also {
                categoryCache = it
                categoriesVersion++
                writeCache()
            }
        }
    }

    private suspend fun request(
        action: String,
        page: Int,
        categoryId: Long?,
        search: String?,
        perPage: Int,
        sort: String?,
        pageToken: String?
    ): JSONObject {
        val categoryRemoteId = categoryId?.let { id ->
            categoryCache.firstOrNull { it.id == id }?.slug ?: id.toString()
        }
        val params = listOfNotNull(
            "action" to action,
            categoryRemoteId?.let { "categoryId" to it },
            categoryRemoteId?.let { "category_id" to it },
            search?.takeIf(String::isNotBlank)?.let { "search" to it },
            "page" to page.toString(),
            "pageSize" to perPage.coerceIn(1, 100).toString(),
            "perPage" to perPage.coerceIn(1, 100).toString(),
            "limit" to perPage.coerceIn(1, 100).toString(),
            pageToken?.let { "pageToken" to it },
            "orderBy" to when (sort) { "newest", "latest" -> "createdTime desc"; else -> "name_natural" }
        ).joinToString("&") { (name, value) -> "${urlEncode(name)}=${urlEncode(value)}" }
        val baseUrl = BuildConfig.DRIVE_BACKEND_URL
        val separator = if ('?' in baseUrl) '&' else '?'
        val requestUrl = URL("$baseUrl$separator$params")
        val raw = withContext(Dispatchers.IO) {
            val connection = requestUrl.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 12_000
                connection.readTimeout = 25_000
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException("Wallpaper service returned HTTP $code")
                connection.inputStream.bufferedReader().use { it.readText() }
                    .takeIf(String::isNotBlank)
                    ?: throw IllegalStateException("Wallpaper service returned an empty response")
            } finally { connection.disconnect() }
        }
        val parsed = try { JsonParser.parseString(raw) } catch (error: Exception) {
            throw IllegalStateException("Wallpaper service returned invalid JSON", error)
        }
        if (parsed.isJsonArray) return JSONObject().put(if (action == "categories") "categories" else "wallpapers", JSONArray(parsed.asJsonArray.toString()))
        if (!parsed.isJsonObject) throw IllegalStateException("Wallpaper service response was not a JSON object")
        val root = parsed.asJsonObject
        if (root.has("error") && !root.get("error").isJsonNull) {
            throw IllegalStateException(root.get("error").asString)
        }
        val objectValue = root.toString().let(::JSONObject)
        return objectValue.optJSONObject("data") ?: objectValue.optJSONObject("result") ?: objectValue
    }

    private fun parseCategories(root: JSONObject): List<Category> = root.firstArray(
        "categories", "categoryList", "category_list", "folders", "driveFolders", "collections", "items", "files", "data", "result"
    )
        ?.let { array -> buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val name = item.firstString("name", "title", "categoryName", "category_name") ?: continue
                val rawId = item.firstString("id", "categoryId", "category_id", "folderId", "folder_id") ?: "${name.hashCode().toLong().and(0x7fffffff)}"
                val id = rawId.toLongOrNull() ?: (rawId.hashCode().toLong().and(0x7fffffff)).coerceAtLeast(1L)
                add(Category(id, name, slug = rawId, coverImageUrl = item.firstString("coverImageUrl", "cover_image_url", "thumbnailLink", "thumbnail_url")?.let(DriveImageUrls::thumbnail)))
            }
        }.distinctBy { it.id }.sortedBy { it.name.lowercase() } }
        .orEmpty()

    private fun parseWallpapers(root: JSONObject): List<Wallpaper> {
        val array = root.firstArray("wallpapers", "items", "files", "results", "data", "result") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.firstString("fileId", "file_id", "sourceId", "id") ?: continue
                val title = item.firstString("title", "name", "fileName", "file_name") ?: "Wallpaper"
                val originalRaw = item.firstString("imageUrl", "image_url", "url", "webContentLink", "web_content_link", "downloadUrl", "download_url")
                    ?: item.firstString("thumbnailUrl", "thumbnail_url", "thumbnailLink", "thumbnail_link")
                    ?: id
                val thumbnailRaw = item.firstString("thumbnailUrl", "thumbnail_url", "thumbnailLink", "thumbnail_link", "imageUrl", "image_url") ?: originalRaw
                val rawCategoryId = item.firstString("categoryId", "category_id")
                val parentIds = item.optJSONArray("parents")?.let { parents -> (0 until parents.length()).mapNotNull { parents.optString(it).takeIf(String::isNotBlank) } }.orEmpty()
                val matchedCategory = (listOfNotNull(rawCategoryId) + parentIds).firstNotNullOfOrNull { raw -> categoryCache.firstOrNull { it.slug == raw || it.id.toString() == raw } }
                val categoryName = item.firstString("categoryName", "category_name", "category", "folderName", "folder_name") ?: matchedCategory?.name
                val resourceKey = item.firstString("resourceKey", "resource_key")
                val original = DriveImageUrls.original(originalRaw, id, resourceKey)
                val thumb = DriveImageUrls.thumbnail(thumbnailRaw, id, resourceKey)
                if (original.isBlank() || thumb.isBlank()) continue
                val imageMetadata = item.optJSONObject("imageMediaMetadata")
                    ?: item.optJSONObject("image_metadata")
                    ?: item.optJSONObject("imageMetadata")
                    ?: item.optJSONObject("metadata")
                val dimensionText = item.firstString("resolution", "dimensions")
                val dimensionParts = dimensionText?.split(Regex("\\s*[×xX]\\s*"))
                val width = item.intValue("width", "imageWidth", "image_width")
                    .takeIf { it > 0 } ?: imageMetadata?.intValue("width", "imageWidth", "image_width")?.takeIf { it > 0 }
                    ?: dimensionParts?.getOrNull(0)?.toIntOrNull()
                val height = item.intValue("height", "imageHeight", "image_height")
                    .takeIf { it > 0 } ?: imageMetadata?.intValue("height", "imageHeight", "image_height")?.takeIf { it > 0 }
                    ?: dimensionParts?.getOrNull(1)?.toIntOrNull()
                if (!WallpaperQuality.isSuitable(width, height)) continue
                add(Wallpaper(
                    id = id.toLongOrNull() ?: id.hashCode().toLong().and(0x7fffffff).coerceAtLeast(1L),
                    title = WallpaperTitles.displayTitle(title, categoryName),
                    imageUrl = original,
                    thumbnailUrl = thumb,
                    categoryId = rawCategoryId?.toLongOrNull(),
                    categoryName = categoryName,
                    resolution = "$width × $height",
                    tags = item.optJSONArray("tags")?.let { tags -> (0 until tags.length()).mapNotNull { tags.optString(it).takeIf(String::isNotBlank) } },
                    sourceName = "Google Drive",
                    sourceUrl = item.firstString("webViewLink", "web_view_link", "sourceUrl", "source_url"),
                    sourceId = id
                ))
            }
        }.distinctBy { it.sourceId }
    }

    private fun readCache() {
        runCatching {
            if (!cacheFile.isFile) return
            val root = JSONObject(cacheFile.readText())
            categoryCache = parseCategories(root)
            val pages = root.optJSONObject("pages") ?: return
            pages.keys().forEach { key ->
                val pageObj = pages.optJSONObject(key) ?: return@forEach
                pageCache[key] = WallpaperPage(
                    parseWallpapers(JSONObject().put("wallpapers", pageObj.optJSONArray("items") ?: JSONArray())),
                    pageObj.optInt("page", 1), pageObj.optInt("lastPage", 1)
                )
                val token = pageObj.optString("nextPageToken").takeIf(String::isNotBlank)
                if (token != null) {
                    val parts = key.split('|')
                    val cat = parts.getOrNull(1)?.takeUnless { it == "all" }?.toLongOrNull()
                    val size = parts.getOrNull(2)?.toIntOrNull() ?: 24
                    val sort = parts.getOrNull(3).orEmpty().takeUnless { it == "null" }
                    pageTokens[tokenPageKey(tokenKey(cat, size, sort), pageObj.optInt("page", 1) + 1)] = token
                }
            }
        }.onFailure { Log.w(TAG, "Ignoring invalid wallpaper cache", it) }
    }

    private fun writeCache() {
        runCatching {
            val root = JSONObject().put("categories", JSONArray().also { arr -> categoryCache.forEach { arr.put(JSONObject(gson.toJson(it))) } })
            val pages = JSONObject()
            pageCache.forEach { (key, page) ->
                val token = pageTokens[tokenPageKey(tokenKeyFromPageKey(key), page.page + 1)]
                pages.put(key, JSONObject().put("page", page.page).put("lastPage", page.lastPage).put("items", JSONArray().also { arr ->
                    page.items.forEach { arr.put(JSONObject(gson.toJson(it))) }
                }).put("nextPageToken", token))
            }
            root.put("pages", pages)
            val temp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
            temp.writeText(root.toString())
            if (!temp.renameTo(cacheFile)) { cacheFile.writeText(temp.readText()); temp.delete() }
        }.onFailure { Log.w(TAG, "Unable to persist wallpaper cache", it) }
    }

    private fun tokenKeyFromPageKey(key: String): String {
        val parts = key.split('|')
        val category = parts.getOrNull(1) ?: "all"
        val perPage = parts.getOrNull(2) ?: "24"
        val sort = parts.getOrNull(3) ?: "newest"
        return "$category|$perPage|$sort"
    }

    private fun pageKey(page: Int, categoryId: Long?, perPage: Int, sort: String?) = "$page|${categoryId ?: "all"}|$perPage|${sort ?: "newest"}"
    private fun tokenKey(categoryId: Long?, perPage: Int, sort: String?) = "${categoryId ?: "all"}|$perPage|${sort ?: "newest"}"
    private fun tokenPageKey(key: String, page: Int) = "$key|$page"
    private fun lockFor(key: String) = requestLocks.computeIfAbsent(key) { Mutex() }
    private fun urlEncode(value: String) = URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun JSONObject.firstArray(vararg keys: String): JSONArray? {
        keys.forEach { key -> optJSONArray(key)?.let { return it } }
        return null
    }
    private fun JSONObject.firstString(vararg keys: String): String? {
        keys.forEach { key ->
            val value = opt(key)
            if (value is String && value.isNotBlank() && value != "null") return value
            if (value is Number) return value.toString()
        }
        return null
    }
    private fun JSONObject.intValue(vararg keys: String): Int {
        keys.forEach { key ->
            val value = opt(key)
            if (value is Number) return value.toInt()
            (value as? String)?.toIntOrNull()?.let { return it }
        }
        return 0
    }

    private companion object {
        const val TAG = "WalloraRepository"
    }
}

/** Coil must receive a public image resource URL, not a Drive preview HTML page. */
object DriveImageUrls {
    private val idPattern = Regex("(?:/d/|[?&]id=|[?&]fileId=)([A-Za-z0-9_-]{10,})")

    fun thumbnail(value: String, fallbackId: String? = null, resourceKey: String? = null): String =
        direct(value, fallbackId, resourceKey, thumb = true)

    fun original(value: String, fallbackId: String? = null, resourceKey: String? = null): String =
        direct(value, fallbackId, resourceKey, thumb = false)

    private fun direct(value: String, fallbackId: String?, resourceKey: String?, thumb: Boolean): String {
        val raw = value.trim()
        val id = idPattern.find(raw)?.groupValues?.getOrNull(1)
            ?: raw.takeIf { !it.startsWith("http", true) && it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
            ?: fallbackId?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{10,}")) }
        if (id == null) return raw
        val key = resourceKey ?: runCatching { URI(raw).rawQuery.orEmpty().split('&').firstOrNull { it.startsWith("resourcekey=") }?.substringAfter('=') }.getOrNull()
        val base = if (thumb) "https://lh3.googleusercontent.com/d/$id=w1000" else "https://drive.google.com/uc?export=download&id=$id"
        return if (key.isNullOrBlank()) base else "$base${if (thumb) "?" else "&"}resourcekey=$key"
    }
}
