package space.naboo.telesam.presenter

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import com.github.badoualy.telegram.tl.api.TLAbsChat
import com.github.badoualy.telegram.tl.api.TLInputPeerEmpty
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import com.github.badoualy.telegram.tl.api.messages.TLAbsDialogs
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.Prefs
import space.naboo.telesam.view.MainFragmentView

class MainFragmentPresenter(val mainView: MainFragmentView) {

    private val TAG: String = MainFragmentPresenter::class.java.simpleName

    private val PERMISSION_REQ_CODE = 1

    private val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var phoneNumber: String? = null
    private var sentCode: TLSentCode? = null

    fun onGrantPermissionClick(view: View) {
        if (mainView.checkSelfPermission(permissions) != PackageManager.PERMISSION_GRANTED) {
            mainView.requestPermissions(permissions, PERMISSION_REQ_CODE)
        }
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQ_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainView.onPermissionGranted(true)
            } else {
                mainView.onPermissionGranted(false)
            }
        }
    }

    init {
        val grant = mainView.checkSelfPermission(permissions)
        if (grant == PackageManager.PERMISSION_GRANTED) {
            mainView.onPermissionGranted(true)
        } else {
            mainView.onPermissionGranted(false)
        }

        RxJavaPlugins.setErrorHandler { e ->
            Log.w(TAG, "Handled some error", e)
        }
    }

    fun onTelegramSignInClick(view: View, phoneNumber: CharSequence) {
        Observable.create<TLSentCode> {
            val client = MyApp.kotlogram.client

            try {
                it.onNext(client.authSendCode(false, phoneNumber.toString(), true))

                // todo always ok to proceed?
            } catch (e: Exception) {
                it.onError(e)
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Code request result: $it")
                    this.phoneNumber = phoneNumber.toString()
                    sentCode = it
                    mainView.onCodeRequested()
                }, {
                    Log.e(TAG, "Error while requesting code", it)

                    this.phoneNumber = null
                    sentCode = null
                })
    }

    fun onCodeEntered(view: View, code: CharSequence) {
        sentCode?.let { sentCode ->
            Observable.create<TLAuthorization> {
                val client = MyApp.kotlogram.client

                try {
                    // Auth with the received code
                    it.onNext(client.authSignIn(phoneNumber, sentCode.phoneCodeHash, code.toString()))
                } catch (e: Exception) {
                    it.onError(e)
                }
            }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.v(TAG, "Sign in result: $it")

                        val self = it.user.asUser
                        Log.v(TAG, "You are now signed in as " + self.firstName + " " + self.lastName + " @" + self.username)
                    }, {
                        Log.e(TAG, "Error while singing in", it)
                    })
        } ?: return
    }

    fun onSelectGroupClick(view: View) {
        Observable.create<TLAbsDialogs> {
            val client = MyApp.kotlogram.client

            try {
                // Auth with the received code
                it.onNext(client.messagesGetDialogs(false, 0, 0, TLInputPeerEmpty(), 100500))
            } catch (e: Exception) {
                it.onError(e)
            }
        }
                .map { it.chats }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Get dialogs result: $it")

                    mainView.onGroupsAvailable(it)
                }, {
                    Log.e(TAG, "Error while getting dialogs", it)
                })
    }

    fun onGroupClick(group: TLAbsChat) {
        Log.v(TAG, "Group click: $group")

        Prefs().saveGroupId(group.id)
    }
}

