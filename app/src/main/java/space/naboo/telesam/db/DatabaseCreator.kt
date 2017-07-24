package space.naboo.telesam.db

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.migration.Migration
import android.content.Context
import space.naboo.telesam.Prefs
import space.naboo.telesam.model.Dialog
import timber.log.Timber

class DatabaseHelper(context: Context) {

    private val DATABASE_NAME = "app.db"

    val appDatabase: AppDatabase

    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            Prefs().migrate(1, 2)
            database.execSQL("CREATE TABLE `${Dialog.TABLE_NAME}` (`id` INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " `${Dialog.FIELD_ACCESS_HASH}` INTEGER, `${Dialog.FIELD_TYPE}` INTEGER, `${Dialog.FIELD_NAME}` TEXT)")
        }
    }

    companion object {
        const val VERSION = 2
    }

    init {
        appDatabase = createDb(context.applicationContext)
    }

    private fun createDb(context: Context): AppDatabase {
        Timber.d("Creating DB")

        return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }

}
