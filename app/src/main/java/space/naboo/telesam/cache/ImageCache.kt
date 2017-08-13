package space.naboo.telesam.cache

import android.graphics.Bitmap
import android.util.LruCache
import space.naboo.telesam.MyApp
import timber.log.Timber
import java.io.File

/**
 * Memory image cache.
 */
// todo unit test it
class LruImageCache {

    var memoryCacheCountLimit = 10
    var diskCacheCountLimit = 50

    private val diskCache = LruDiskCache<String, Bitmap>(BitmapCachePolicy(), 0, diskCacheCountLimit,
            File(MyApp.instance.cacheDir, "image_cache"))
    private val inMemoryCache = CustomLruCache(memoryCacheCountLimit, diskCache)

    fun add(key: String, bitmap: Bitmap) {
        Timber.d("Adding to memory cache $key")
        inMemoryCache.put(key, bitmap)
    }

    fun get(key: String): Bitmap? {
        Timber.d("Looking for $key in cache")
        inMemoryCache.get(key)?.let {
            Timber.d("$key found in memory cache")
            return it
        }

        diskCache.get(key)?.let {
            Timber.d("$key found in disk cache")
            return it
        }

        Timber.d("$key not found in cache")
        return null
    }

}

private class CustomLruCache<K: Any, V: Any>(maxSize: Int, private val diskCache: LruDiskCache<K, V>) : LruCache<K, V>(maxSize) {

    override fun entryRemoved(evicted: Boolean, key: K, oldValue: V, newValue: V?) {
        Timber.d("Moving $key to disk cache")
        diskCache.put(key, oldValue)
    }

}
