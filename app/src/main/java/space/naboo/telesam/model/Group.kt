package space.naboo.telesam.model

import android.os.Parcel
import android.os.Parcelable

data class Group(val id: Int, val name: String) : Parcelable {

    companion object {
        val CREATOR = object : Parcelable.Creator<Group> {
            override fun createFromParcel(parcel: Parcel): Group {
                return Group(parcel)
            }

            override fun newArray(size: Int): Array<Group?> {
                return arrayOfNulls(size)
            }
        }
    }

    private constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString())

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(id)
        dest.writeString(name)
    }

    override fun describeContents() = 0

}
