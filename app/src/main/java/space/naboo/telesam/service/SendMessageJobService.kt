package space.naboo.telesam.service

import android.app.job.JobParameters
import android.app.job.JobService
import com.github.badoualy.telegram.tl.api.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.R
import space.naboo.telesam.model.Dialog
import space.naboo.telesam.model.Sms
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

        // check if group exists first and only after it proceed with SMS
        disposable = MyApp.database.dialogDao().load()
                .switchIfEmpty {
                    Timber.i("Dialog not yet saved, clearing SMS messages from DB")
                    MyApp.database.smsDao().deleteAll()
                }
                .zipWith(MyApp.database.smsDao().loadAllSms(), BiFunction<Dialog, List<Sms>, Unit> { dialog, smsList ->
                    smsList.forEach { sms ->
                        if (cancel) {
                            throw InterruptedException("Execution interrupted by Android")
                        }

                        val text = getString(R.string.new_message_template, sms.from, sms.message)
                        val tlAbsUpdates = MyApp.kotlogram.client.messagesSendMessage(true, false, false, false,
                                inputPeerFromDialog(dialog), null, text, Random().nextLong(), null, null)

                        Timber.v("message send result: $tlAbsUpdates")
                        MyApp.database.smsDao().delete(sms)
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({}, {
                    Timber.w(it, "Exception while sending messages")
                    jobFinished(params, true)
                }, {
                    Timber.v("All messages sent")
                    jobFinished(params, false)
                })

        return true
    }

    private fun inputPeerFromDialog(dialog: Dialog): TLAbsInputPeer {
        when (dialog.type) {
            Dialog.TYPE_USER -> return TLInputPeerUser(dialog.id, dialog.accessHash)
            Dialog.TYPE_CHAT -> return TLInputPeerChat(dialog.id)
            Dialog.TYPE_CHANNEL -> return TLInputPeerChannel(dialog.id, dialog.accessHash)
            else -> {
                Timber.w("creating input peer is not possible for dialog: $dialog")
                return TLInputPeerEmpty()
            }
        }
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
