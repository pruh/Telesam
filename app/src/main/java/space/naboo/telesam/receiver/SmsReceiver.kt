package space.naboo.telesam.receiver

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.model.Sms
import space.naboo.telesam.service.SendMessageJobService

class SmsReceiver : BroadcastReceiver() {

    private val TAG: String = SmsReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION != intent.action) {
            Log.w(TAG, "wrong action: $intent")
            return
        }

        Log.i(TAG, "received SMS message intent: $intent")

        val messages = reamMessages(intent)

        Log.v(TAG, "Internet is not available, sending will be postponed")
        scheduleSending(context, messages)
    }

    private fun reamMessages(intent: Intent): List<Sms> {
        return Telephony.Sms.Intents.getMessagesFromIntent(intent)
                .filter { it != null }
                .map { Sms(from = it.displayOriginatingAddress, message = it.displayMessageBody) }
                .toList()
    }

    private fun scheduleSending(context: Context, messages: List<Sms>) {
        Observable.create<Unit> {
            it.onNext(MyApp.database.smsDao().insertAll(messages))
            it.onComplete()
        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, {
                    Log.w(TAG, "Failed to store messages to send", it)
                }, {
                    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                    val result = jobScheduler.schedule(
                            JobInfo.Builder(SendMessageJobService.JOB_ID, ComponentName(context, SendMessageJobService::class.java))
                                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                    .setPersisted(true)
                                    .build())

                    if (result != JobScheduler.RESULT_SUCCESS) {
                        Log.w(TAG, "Failed to schedule SMS send")
                    }
                })
    }

}
