package space.naboo.telesam.db.dao

import android.arch.persistence.room.*
import io.reactivex.Maybe
import space.naboo.telesam.model.Sms

@Dao
interface SmsDao {
    @Query("SELECT * FROM ${Sms.TABLE_NAME}")
    fun loadAllSms(): Maybe<List<Sms>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(sms: List<Sms>)

    @Delete
    fun delete(sms: Sms)

    @Query("DELETE FROM ${Sms.TABLE_NAME}")
    fun deleteAll()
}
