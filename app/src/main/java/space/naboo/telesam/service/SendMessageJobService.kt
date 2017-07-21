package space.naboo.telesam.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.github.badoualy.telegram.tl.api.TLInputPeerChat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.Prefs
import space.naboo.telesam.R
import timber.log.Timber
import java.util.*

class SendMessageJobService : JobService() {

    companion object {
        val JOB_ID = 1
    }

    private var disposable: Disposable? = null
    @Volatile private var cancel = false

    override fun onStartJob(params: JobParameters?): Boolean {
        Timber.v("onStartJob")

        val groupId = Prefs().groupId

        if (groupId == 0) {
            Timber.v("group not yet set")
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

                        Timber.v("message send result: $tlAbsUpdates")
                        MyApp.database.smsDao().delete(sms)
                    }
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, {
                    Timber.w(it)
                    jobFinished(params, true)
                }, {
                    Timber.v("All messages sent")
                    jobFinished(params, false)
                })

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Timber.v("onStopJob")

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
