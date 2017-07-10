package space.naboo.telesam.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    private val TAG: String = SmsReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.i(TAG, "received SMS message")
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            // todo start job
        }
    }

}
