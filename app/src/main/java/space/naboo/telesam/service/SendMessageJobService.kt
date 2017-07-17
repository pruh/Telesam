package space.naboo.telesam.service

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import com.github.badoualy.telegram.tl.api.TLInputPeerChat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.Prefs
import space.naboo.telesam.R
import java.util.*

class SendMessageJobService : JobService() {

    private val TAG: String = SendMessageJobService::class.java.simpleName

    companion object {
        val JOB_ID = 1
    }

    private var disposable: Disposable? = null
    @Volatile private var cancel = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.v(TAG, "onStartJob")

        val groupId = Prefs().getGroupId()

        if (groupId == null) {
            Log.v(TAG, "group not yet set")
            return false
        }

        disposable = MyApp.database.smsDao().loadAllSms()
                .map { list ->
                    list.forEach { sms ->
                        if (cancel) {
                            throw InterruptedException("Execution interrupted by Android")
                        }

                        val text = getString(R.string.new_message_template, sms.from, sms.message)
                        val tlAbsUpdates = MyApp.kotlogram.client.messagesSendMessage(true, false, false, false,
                                TLInputPeerChat(groupId), null, text, Random().nextLong(), null, null)
                        // todo check tlAbsUpdates
                        MyApp.database.smsDao().delete(sms)
                    }
                }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, {
                    Log.w(TAG, "Exception while sending messages", it)
                    jobFinished(params, true)
                }, {
                    Log.v(TAG, "All messages sent")
                    jobFinished(params, false)
                })

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.v(TAG, "onStopJob")

        disposable?.let {
            if (it.isDisposed) {
                return false
            } else {
                cancel = true
                disposable?.dispose()
                return true
            }
        } ?: return false
    }

}
