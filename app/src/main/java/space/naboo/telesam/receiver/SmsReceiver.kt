package space.naboo.telesam.receiver

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Telephony
import io.reactivex.Maybe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.model.Sms
import space.naboo.telesam.service.SendMessageJobService
import timber.log.Timber

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            Timber.w("wrong action: $intent")
            return
        }

        Timber.i("received SMS message intent: $intent")

        val messages = reamMessages(intent)

        saveMessages(context, messages)
    }

    private fun reamMessages(intent: Intent): List<Sms> {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent)
                .filter { it != null }
                .map { Sms(from = it.displayOriginatingAddress, message = it.displayMessageBody) }
                .toList()
    }

    private fun saveMessages(context: Context, messages: List<Sms>) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            val ps = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            Timber.d("isDeviceIdleMode: ${ps.isDeviceIdleMode}")
        }

        Maybe.fromCallable<Int> {
            if (MyApp.database.dialogDao().count() > 0) {
                Timber.d("Saving message to database")
                MyApp.database.smsDao().insertAll(messages)
                messages.count()
            } else {
                Timber.d("No dialog saved yet, skip current message(s)")
                null
            }}
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    scheduleJob(context)
                }, {
                    Timber.w(it, "Exception while saving messages to database")
                }, {
                    Timber.d("No messages saved, do not schedule a job")
                })
    }

    private fun scheduleJob(context: Context) {
        Timber.d("Scheduling job")
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        val result = jobScheduler.schedule(
                JobInfo.Builder(SendMessageJobService.JOB_ID, ComponentName(context, SendMessageJobService::class.java))
                        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        .setPersisted(true)
                        .build())

        if (result != JobScheduler.RESULT_SUCCESS) {
            Timber.w("Failed to schedule SMS send")
        }
    }

}
