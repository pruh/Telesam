package space.naboo.telesam.cache

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class BitmapCachePolicy(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG, quality: Int = 100) : CachePolicy<Bitmap> {
    private var mFormat: Bitmap.CompressFormat? = format
    private var mQuality: Int = quality

    override fun write(outputFile: File, value: Bitmap): Boolean {
        var success = false
        FileOutputStream(outputFile).use {
            value.compress(mFormat, mQuality, it)
            success = true
        }
        return success
    }

    override fun read(inputFile: File): Bitmap {
        return BitmapFactory.decodeFile(inputFile.path)
    }

    override fun size(value: Bitmap): Long {
        return value.byteCount.toLong()
    }
}
