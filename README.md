# Wallora

Wallora is a native Android wallpaper app. Its catalog metadata is bundled in `app/src/main/assets/wallpapers.json`; wallpaper images are delivered directly from the Unsplash image CDN. App launch and category browsing do not call a wallpaper API.

## Build

Open the project in Android Studio or run `gradlew.bat :app:assembleDebug`. The build uses Android Gradle Plugin 9.4.1, Gradle 9.6.0, JDK 17+, and Android SDK 36. Unit tests can be run with `gradlew.bat :app:testDebugUnitTest`.

## App behavior

- The bundled catalog provides 23 wallpaper categories and paginated local feeds.
- Coil caches image previews in memory and on disk. Unsplash's CDN supplies uncached image bytes.
- Home, categories, favorites, downloads, recent items, settings, and wallpaper editing are available in the app.
- AdMob banner and interstitial placements remain enabled.
- Favorites, downloads index, and recently viewed entries stay in local app storage. Saved image bytes go to `Pictures/Wallora`.
- The app opens directly to Home without a startup video.

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.
