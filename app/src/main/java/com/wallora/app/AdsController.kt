package com.wallora.app

import android.app.Activity
import android.content.Context
import android.util.Log
import com.wallora.app.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback

/** Small fail-open AdMob coordinator: all ad errors continue the original user action. */
object AdsController {
    private const val TAG = "WalloraAds"
    private const val TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private val interstitialUnitId: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_UNIT_ID else BuildConfig.LIVE_INTERSTITIAL_AD_UNIT_ID

    @Volatile private var loadedAd: InterstitialAd? = null
    @Volatile private var loading = false

    fun preload(context: Context) {
        synchronized(this) {
            if (loading || loadedAd != null) return
            loading = true
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "Debug build uses Google's test interstitial unit; release uses the Wallora live unit")
        try {
            InterstitialAd.load(
                AdRequest.Builder(interstitialUnitId).build(),
                object : AdLoadCallback<InterstitialAd> {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        synchronized(this@AdsController) {
                            loadedAd = ad
                            loading = false
                        }
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        synchronized(this@AdsController) { loading = false }
                        Log.w(TAG, "Interstitial failed to load (code=${adError.code}, message=${adError.message}): $adError")
                    }
                }
            )
        } catch (error: Exception) {
            synchronized(this) { loading = false }
            Log.w(TAG, "Unable to preload interstitial", error)
        }
    }

    fun showThen(activity: Activity?, context: Context, continueAction: () -> Unit) {
        val ad = synchronized(this) { loadedAd.also { loadedAd = null } }
        if (activity == null || ad == null) {
            try {
                continueAction()
            } catch (error: Exception) {
                Log.e(TAG, "Action continued without an interstitial but failed", error)
            } finally {
                preload(context)
            }
            return
        }
        var continued = false
        fun continueOnce() {
            if (continued) return
            continued = true
            try {
                continueAction()
            } catch (error: Exception) {
                Log.e(TAG, "Action after interstitial failed", error)
            } finally {
                preload(context)
            }
        }
        try {
            ad.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdDismissedFullScreenContent() = continueOnce()
                override fun onAdFailedToShowFullScreenContent(error: com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError) {
                    Log.w(TAG, "Interstitial failed to show: $error")
                    continueOnce()
                }
            }
            ad.show(activity)
        } catch (error: Exception) {
            Log.w(TAG, "Unable to show interstitial", error)
            runCatching { ad.destroy() }
            continueOnce()
        }
    }
}
