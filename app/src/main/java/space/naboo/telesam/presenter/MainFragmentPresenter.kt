package space.naboo.telesam.presenter

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import com.github.badoualy.telegram.tl.api.messages.TLAbsDialogs
import com.github.badoualy.telegram.tl.core.TLBool
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.Prefs
import space.naboo.telesam.model.Group
import space.naboo.telesam.model.User
import space.naboo.telesam.view.MainView

class MainFragmentPresenter(val mainView: MainView) {

    private val TAG: String = MainFragmentPresenter::class.java.simpleName

    private val PERMISSION_REQ_CODE = 1

    private val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var phoneNumber: String? = null
    private var sentCode: TLSentCode? = null

    init {
        val grant = mainView.checkSelfPermission(permissions)
        if (grant == PackageManager.PERMISSION_GRANTED) {
            mainView.onPermissionGranted(true)
        } else {
            mainView.onPermissionGranted(false)
        }

        val prefs = Prefs()

        checkAuthorization()

        prefs.groupId.let {
            if (it != 0) {
                fetchGroup(it)
            } else {
                mainView.onGroupSelected(null)
            }
        }

        RxJavaPlugins.setErrorHandler { e ->
            Log.w(TAG, "Handled some error", e)
        }
    }

    private fun fetchGroup(groupId: Int) {
        getLoadGroupsObservable()
                .map { it.first { it.id == groupId } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Group fetch result: $it")
                    mainView.onGroupSelected(it)
                }, {
                    Log.e(TAG, "Error while fetching groups", it)
                })
    }

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

    fun onTelegramSignInClick(view: View, phoneNumber: CharSequence) {
        Observable.create<TLSentCode> { it.onNext(MyApp.kotlogram.client
                .authSendCode(false, phoneNumber.toString(), true)) }
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
            Observable.create<TLAuthorization> { it.onNext(MyApp.kotlogram.client
                    .authSignIn(phoneNumber, sentCode.phoneCodeHash, code.toString())) }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.v(TAG, "Sign in result: $it")

                        Prefs().isSignedIn = true

                        mainView.onSignedIn(createUser(it.user.asUser))
                    }, {
                        Log.e(TAG, "Error while singing in", it)
                    })
        } ?: return
    }

    fun loadGroups(view: View) {
        getLoadGroupsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Get dialogs result: $it")

                    mainView.onGroupsAvailable(it)
                }, {
                    Log.e(TAG, "Error while getting dialogs", it)
                })
    }

    private fun getLoadGroupsObservable(): Observable<List<Group>> {
        return Observable.create<TLAbsDialogs> { it.onNext(MyApp.kotlogram.client
                    .messagesGetDialogs(false, 0, 0, TLInputPeerEmpty(), 100500)) }
                .map { it.chats
                        .filterIsInstance<TLChat>()
                        .map { Group(it.id, it.title) } }
    }

    fun logout(view: View) {
        Observable.create<TLBool> { it.onNext(MyApp.kotlogram.client.authLogOut()) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Get dialogs result: $it")

                    val prefs = Prefs()
                    prefs.groupId = 0
                    prefs.isSignedIn = false

                    mainView.onSignedOut()
                }, {
                    Log.e(TAG, "Error while logging out", it)
                })
    }

    private fun checkAuthorization() {
        Observable.create<TLUserFull> { it.onNext(MyApp.kotlogram.client.usersGetFullUser(TLInputUserSelf())) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.v(TAG, "Authorization result: $it")

                    mainView.onSignedIn(createUser(it.user.asUser))
                }, {
                    if (it is RpcErrorException && it.code == 401) {
                        Log.v(TAG, "User not authorized")
                        mainView.onSignedOut()
                    } else {
                        Log.e(TAG, "Error while checking authorization", it)
                    }
                })
    }

    private fun createUser(user: TLUser): User {
        return User(user.firstName, user.lastName)
    }

}
