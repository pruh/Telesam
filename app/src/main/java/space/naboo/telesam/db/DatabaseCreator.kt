package space.naboo.telesam.db

import android.arch.persistence.room.Room
import android.content.Context
import timber.log.Timber

class DatabaseHelper(context: Context) {

    private val DATABASE_NAME = "app.db"

    val appDatabase = createDb(context.applicationContext)

    private fun createDb(context: Context): AppDatabase {
        Timber.d("Creating DB")

        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME).build()
    }

    companion object {
        const val VERSION = 1
    }

}
