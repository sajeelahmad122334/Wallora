package com.wallora.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.work.*
import com.wallora.app.data.WalloraRepository
import com.wallora.app.data.UserLibraryStore
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.initialization.OnAdapterInitializationCompleteListener
import java.util.concurrent.TimeUnit

class WalloraApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            val config = InitializationConfig.Builder("ca-app-pub-1370798994284605~2244621936").build()
            MobileAds.initialize(this, config, OnAdapterInitializationCompleteListener {
                Log.i("WalloraAdMob", "Mobile Ads initialized; adapter statuses=${it.adapterStatusMap}, latencyMs=${it.totalLatency}")
                AdsController.preload(this)
            })
        }.onFailure { Log.e("WalloraAds", "MobileAds initialization failed; app will continue without ads", it) }
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(java.io.File(cacheDir, "wallora_image_cache"))
                .maxSizePercent(0.02)
                .build()
        }
        .crossfade(false)
        .build()

    val repository by lazy {
        WalloraRepository(this)
    }
    val library by lazy { UserLibraryStore(this) }
    fun scheduleAutoWallpaper(intervalMinutes: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<AutoWallpaperWorker>(
            intervalMinutes.coerceIn(15, 7L * 24 * 60), TimeUnit.MINUTES
        ).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("wallora_auto_wallpaper", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
    fun cancelAutoWallpaper() = WorkManager.getInstance(this).cancelUniqueWork("wallora_auto_wallpaper")
}
