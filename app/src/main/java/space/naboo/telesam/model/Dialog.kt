package space.naboo.telesam.model

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Ignore
import android.arch.persistence.room.PrimaryKey
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import com.github.badoualy.telegram.tl.api.TLAbsFileLocation
import space.naboo.telesam.model.Dialog.Companion.TYPE_CHANNEL
import space.naboo.telesam.model.Dialog.Companion.TYPE_CHAT
import space.naboo.telesam.model.Dialog.Companion.TYPE_USER

@Entity(tableName = Dialog.TABLE_NAME)
data class Dialog(
        @PrimaryKey(autoGenerate = true) var id: Int = 0,
        @ColumnInfo(name = FIELD_ACCESS_HASH) var accessHash: Long = 0,
        @ColumnInfo(name = FIELD_TYPE) @DialogType var type: Long = 0, // kotlin error: https://youtrack.jetbrains.com/issue/KT-16506
        @ColumnInfo(name = FIELD_NAME) var name: String = "",
        @Ignore val fileLocation: FileLocation? = null) : Parcelable {

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
            parcel.readString(),
            parcel.readParcelable<FileLocation>(FileLocation::class.java.classLoader))

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeLong(accessHash)
        dest.writeLong(type)
        dest.writeString(name)
        dest.writeParcelable(fileLocation, flags)
    }

    override fun describeContents() = 0

}

@Retention(AnnotationRetention.SOURCE)
@IntDef(TYPE_USER, TYPE_CHAT, TYPE_CHANNEL)
annotation class DialogType

data class FileLocation(val dcId: Int, val volumeId: Long, val localId: Int, val secret: Long) : Parcelable {

    private constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readLong(),
            parcel.readInt(),
            parcel.readLong())

    companion object CREATOR : Parcelable.Creator<FileLocation> {
        override fun createFromParcel(parcel: Parcel): FileLocation {
            return FileLocation(parcel)
        }

        override fun newArray(size: Int): Array<FileLocation?> {
            return arrayOfNulls(size)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(dcId)
        parcel.writeLong(volumeId)
        parcel.writeInt(localId)
        parcel.writeLong(secret)
    }

    override fun describeContents() = 0

}