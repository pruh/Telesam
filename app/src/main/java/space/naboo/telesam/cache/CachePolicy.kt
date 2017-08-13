package space.naboo.telesam.cache

import java.io.File

interface CachePolicy<V> {
    fun write(outputFile: File, value: V): Boolean

    fun read(inputFile: File): V

    fun size(value: V): Long

}
