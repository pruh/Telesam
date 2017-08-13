package space.naboo.telesam.cache

import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

/**
 * Modification of disk cache implementation found at http://gabesechansoftware.com/disk-cacheing-on-android/
 */
class LruDiskCache<in K: Any, V: Any>(private val diskCachePolicy: CachePolicy<V>,
        private val maxSize: Long,
        private val maxCount: Int,
        private val cacheDirectory: File) : Cache<K, V> {

    private var currentSize = 0L
    private var filesInCache = LinkedHashMap<Int, CacheEntry>(16, .75f, true)

    init {
        initializeFromDisk()
    }

    private fun initializeFromDisk() {
        cacheDirectory.mkdirs()
        val files = cacheDirectory.listFiles()
        currentSize = 0
        val allFiles = ArrayList<CacheEntry>()
        // Store all the files in a list, then sort them on reverse modification time. The idea is
        // that an older file still in the cache either is so tiny it doesn't matter or is used a lot
        for (file in files) {
            if (file.isDirectory) {
                continue
            }

            val hashedValue = try {
                Integer.valueOf(file.name)
            } catch (e: NumberFormatException) {
                Timber.w("Cannot parse cached file name: ${file.name}")

                file.delete()

                continue
            }

            val length = file.length()
            val entry = CacheEntry(file, length, hashedValue, file.lastModified())
            allFiles.add(entry)
        }
        Collections.sort(allFiles, { (lfile), (rfile) ->
            // We can't just return a cast of diff because of overflow.
            val diff = rfile.lastModified() - lfile.lastModified()
            when {
                diff > 0 -> 1
                diff < 0 -> -1
                else -> 0
            }
        })
        for (entry in allFiles) {
            addEntryToHash(entry)
        }
        ensureCapacity()
    }

    private fun removeFromHash(entry: CacheEntry) {
        entry.file.delete()
        filesInCache.remove(entry.hash)
        currentSize -= entry.sizeBytes
    }

    @Synchronized override fun get(key: K): V? {
        Timber.d("Reading from disk disk cache")

        val hash = key.hashCode()
        val cachedData = filesInCache[hash]
        if (cachedData != null) {
            try {
                return diskCachePolicy.read(cachedData.file)
            } catch (ex: IOException) {
                // If we can't read the file, lets pretend it wasn't there
                return null
            }
        }
        return null
    }

    @Synchronized override fun put(key: K, value: V): Boolean {
        Timber.d("Putting to disk cache")

        val size = diskCachePolicy.size(value)
        val hash = key.hashCode()

        // If the object is already cached, remove it
        var cachedData = filesInCache[hash]
        if (cachedData != null) {
            removeFromHash(cachedData)
        }

        // Perform the actual add
        val outputFile = File(cacheDirectory, Integer.toString(hash))
        try {
            // Write the file. If we can't, tell them we couldn't cache it.
            diskCachePolicy.write(outputFile, value)
        } catch (ex: IOException) {
            return false
        }

        cachedData = CacheEntry(outputFile, size, hash, System.currentTimeMillis())
        addEntryToHash(cachedData)

        ensureCapacity()

        return true
    }

    private fun ensureCapacity() {
        if (maxCount != 0) {
            while (filesInCache.size > maxCount && filesInCache.size != 0) {
                removeEldest()
            }
        }

        if (maxSize != 0L) {
            while (currentSize > maxSize && filesInCache.size != 0) {
                removeEldest()
            }
        }
    }

    private fun removeEldest() {
        Timber.d("Removing eldest entry")
        val it = filesInCache.values.iterator()
        val entry = it.next()
        removeFromHash(entry)
    }

    private fun addEntryToHash(entry: CacheEntry) {
        filesInCache.put(entry.hash, entry)
        currentSize += entry.sizeBytes
    }

    @Synchronized override fun remove(key: K) {
        val hash = key.hashCode()
        val cachedData = filesInCache[hash]
        if (cachedData != null) {
            removeFromHash(cachedData)
        }
    }

    @Synchronized override fun clear() {
        filesInCache.entries.forEach { entry ->
            val cachedData = entry.value
            cachedData.file.delete()
        }
        filesInCache.clear()
        currentSize = 0
    }

    private data class CacheEntry(val file: File, val sizeBytes: Long, val hash: Int, val modTime: Long)

}
