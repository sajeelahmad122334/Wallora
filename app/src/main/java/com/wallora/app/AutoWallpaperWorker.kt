package com.wallora.app

import android.app.WallpaperManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device-level automatic changes need a background image fetch and WallpaperManager permission. */
class AutoWallpaperWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (!autoWallpaperEnabled()) return Result.success()
            val app = applicationContext as WalloraApp
            val wallpaper = app.repository.randomWallpaper() ?: return Result.retry()
            val url = wallpaper.localUri ?: wallpaper.originalUrl
            val request = ImageRequest.Builder(applicationContext)
                .data(url)
                .size(Size(1440, 3200))
                .allowHardware(false)
                .build()
            val image = ImageLoader(applicationContext).execute(request).drawable ?: return Result.retry()
            val bitmap = (image as? android.graphics.drawable.BitmapDrawable)?.bitmap ?: return Result.retry()
            if (!autoWallpaperEnabled()) return Result.success()
            withContext(Dispatchers.IO) {
                WallpaperManager.getInstance(applicationContext).setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun autoWallpaperEnabled(): Boolean = applicationContext
        .getSharedPreferences("wallora_settings", Context.MODE_PRIVATE)
        .getBoolean("auto_enabled", false)
}
