package space.naboo.telesam.db.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import io.reactivex.Maybe
import space.naboo.telesam.model.Dialog

@Dao
interface DialogDao {
    @Query("SELECT * FROM ${Dialog.TABLE_NAME} LIMIT 1")
    fun load(): Maybe<Dialog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(dialog: Dialog)

    @Query("DELETE FROM ${Dialog.TABLE_NAME}")
    fun deleteAll()

    @Query("SELECT Count(*) FROM ${Dialog.TABLE_NAME}")
    fun count(): Int
}
