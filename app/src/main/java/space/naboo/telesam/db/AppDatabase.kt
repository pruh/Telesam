package space.naboo.telesam.db

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import space.naboo.telesam.db.dao.DialogDao
import space.naboo.telesam.db.dao.SmsDao
import space.naboo.telesam.model.Dialog
import space.naboo.telesam.model.Sms

@Database(entities = arrayOf(Sms::class, Dialog::class), version = DatabaseHelper.VERSION)
abstract class AppDatabase : RoomDatabase() {

    abstract fun smsDao(): SmsDao
    abstract fun dialogDao(): DialogDao
}
