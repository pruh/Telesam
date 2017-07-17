package space.naboo.telesam.db.dao

import android.arch.persistence.room.*
import io.reactivex.Flowable
import space.naboo.telesam.model.Sms

@Dao
interface SmsDao {
    @Query("SELECT * FROM ${Sms.TABLE_NAME}")
    fun loadAllSms(): Flowable<List<Sms>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(products: List<Sms>)

    @Delete
    fun delete(sms: Sms)
}
