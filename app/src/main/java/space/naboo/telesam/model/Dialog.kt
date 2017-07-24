package space.naboo.telesam.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import space.naboo.telesam.model.Dialog.Companion.TYPE_CHANNEL
import space.naboo.telesam.model.Dialog.Companion.TYPE_CHAT
import space.naboo.telesam.model.Dialog.Companion.TYPE_USER

@Entity(tableName = Dialog.TABLE_NAME)
data class Dialog(
        @PrimaryKey(autoGenerate = true) var id: Int = 0,
        @ColumnInfo(name = FIELD_ACCESS_HASH) var accessHash: Long = 0,
        @ColumnInfo(name = FIELD_TYPE) @DialogType var type: Long = 0, // kotlin error: https://youtrack.jetbrains.com/issue/KT-16506
        @ColumnInfo(name = FIELD_NAME) var name: String = "") : Parcelable {

    companion object {
        const val TABLE_NAME = "dialogs"
        const val FIELD_ACCESS_HASH = "access_hash"
        const val FIELD_TYPE = "type"
        const val FIELD_NAME = "name"

        const val TYPE_USER = 1L
        const val TYPE_CHAT = 2L
        const val TYPE_CHANNEL = 3L

        val CREATOR = object : Parcelable.Creator<Dialog> {
            override fun createFromParcel(parcel: Parcel): Dialog {
                return Dialog(parcel)
            }

            override fun newArray(size: Int): Array<Dialog?> {
                return arrayOfNulls(size)
            }
        }
    }

    private constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readLong(),
            parcel.readLong(),
            parcel.readString())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(accessHash)
        dest.writeLong(type)
        dest.writeString(name)
    }

    override fun describeContents() = 0

}

@Retention(AnnotationRetention.SOURCE)
@IntDef(TYPE_USER, TYPE_CHAT, TYPE_CHANNEL)
annotation class DialogType
