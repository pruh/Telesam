package space.naboo.telesam.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import space.naboo.telesam.model.Sms.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class Sms(
        @PrimaryKey(autoGenerate = true) var id: Long = 0,
        @ColumnInfo(name = "from") var from: String = "",
        @ColumnInfo(name = "message") var message: String = "") {

    companion object {
        const val TABLE_NAME = "messages"
    }

}