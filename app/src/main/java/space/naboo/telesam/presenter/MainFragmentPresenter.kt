package space.naboo.telesam.presenter

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import com.github.badoualy.telegram.tl.api.*
import com.github.badoualy.telegram.tl.api.auth.TLAuthorization
import com.github.badoualy.telegram.tl.api.auth.TLSentCode
import com.github.badoualy.telegram.tl.api.messages.TLAbsDialogs
import com.github.badoualy.telegram.tl.core.TLBool
import com.github.badoualy.telegram.tl.exception.RpcErrorException
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.Prefs
import space.naboo.telesam.model.Dialog
import space.naboo.telesam.model.User
import space.naboo.telesam.view.MainView
import timber.log.Timber

class MainFragmentPresenter(val mainView: MainView) {

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

        mainView.onBackgroundModeEnabled(mainView.isBackgroundModeEnabled())

        fetchAllTelegramData()

        RxJavaPlugins.setErrorHandler { e ->
            Timber.w(e, "Caught global exception")
        }
    }

    private fun fetchAllTelegramData() {
        val NO_DIALOG = Dialog()
        Maybe.create<TLUserFull> { it.onSuccess(MyApp.kotlogram.client.usersGetFullUser(TLInputUserSelf())) }
                .zipWith(MyApp.database.dialogDao().load().defaultIfEmpty(NO_DIALOG),
                        BiFunction<TLUserFull, Dialog, Pair<TLUserFull, Dialog?>> { user, dialog ->
                            if (NO_DIALOG == dialog) {
                                Pair(user, null)
                            } else {
                                Pair(user, dialog)
                            }
                        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (user, dialog) ->
                    Timber.d("Authorization result: $user")

                    mainView.onSignedIn(createUser(user.user.asUser))

                    if (dialog != null) {
                        Timber.d("Dialog check result: $dialog")
                        mainView.onDialogSelected(dialog)
                    } else {
                        Timber.d("No dialog selected yet")
                        mainView.onDialogSelected(null)
                    }
                }, {
                    if (it is RpcErrorException && it.code == 401) {
                        Timber.d("User not authorized")
                        mainView.onSignedOut()

                        Timber.d("No dialog selected yet")
                        mainView.onDialogSelected(null)
                    } else {
                        Timber.e(it, "Exception when checking authorization")
                    }
                }, {
                    Timber.d("fetchAllTelegramData complete")
                })
    }

    fun onForeground() {
        // need to check background moe as there is no callback
        mainView.onBackgroundModeEnabled(mainView.isBackgroundModeEnabled())
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
                    Timber.d("Code request result: $it")
                    this.phoneNumber = phoneNumber.toString()
                    sentCode = it
                    mainView.onCodeRequested()
                }, {
                    Timber.e(it, "Exception while requesting code")

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
                        Timber.d("Sign in result: $it")

                        Prefs().isSignedIn = true

                        mainView.onSignedIn(createUser(it.user.asUser))
                    }, {
                        Timber.e(it, "Exception while singin in")
                    })
        } ?: return
    }

    fun loadDialogs(view: View) {
        getLoadDialogsObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Get dialogs result: $it")

                    mainView.onDialogsAvailable(it)
                }, {
                    Timber.e(it, "exception while loading dialogs")
                })
    }

    private fun getLoadDialogsObservable(): Observable<List<Dialog>> {
        return Observable.create<TLAbsDialogs> {
            it.onNext(MyApp.kotlogram.client
                    .messagesGetDialogs(false, 0, 0, TLInputPeerSelf(), 100500)) }
                .map { parseDialogs(it) }
    }

    private fun parseDialogs(dialogs: TLAbsDialogs): List<Dialog> {
        val usersMap by lazy {
            dialogs.users.filterIsInstance<TLUser>().map { Pair(it.id, it) }.toMap()
        }
        val absChatsMap by lazy {
            dialogs.chats.map { Pair(it.id, it) }.toMap()
        }

        return dialogs.dialogs.map { dialog ->
            val peer = dialog.peer
            when (peer) {
                is TLPeerUser -> {
                    val user = usersMap[peer.userId]
                    if (user == null) {
                        Timber.d("user with id: ${peer.userId} not found")
                        return@map null
                    }

                    val title = user.firstName?.let { firstName ->
                        user.lastName?.let { lastName ->
                            "%s %s".format(firstName, lastName)
                        } ?: firstName
                    } ?: user.username

                    Dialog(user.id, user.accessHash, Dialog.TYPE_USER, title)
                }
                is TLPeerChat -> {
                    parseChatOrChannel(absChatsMap, peer)
                }
                else -> {
                    Timber.i("unsupported peer $peer")
                    return@map null
                }
            }
        }.filterNotNull().toList()
    }

    private fun parseChatOrChannel(absChatsMap: Map<Int, TLAbsChat>, peer: TLPeerChat): Dialog? {
        val absChat = absChatsMap[peer.chatId]
        when (absChat) {
            is TLChat -> {
                if (absChat.deactivated || absChat.kicked || absChat.left) {
                    Timber.d("chat with id: ${peer.chatId} deactivated or user not is not in the chat")
                    return null
                }

                return Dialog(absChat.id, 0, Dialog.TYPE_CHAT, absChat.title)
            }
            is TLChannel -> {
                if (!absChat.megagroup || absChat.kicked || absChat.left) {
                    Timber.d("channel with id: ${peer.chatId} is not a mega-group or user not is not in the channel")
                    return null
                }

                return Dialog(absChat.id, absChat.accessHash, Dialog.TYPE_CHANNEL, absChat.title)
            }
            else -> {
                Timber.d("chat with id: ${peer.chatId} not found")
                return null
            }
        }
    }

    fun logout(view: View) {
        Observable.create<TLBool> { it.onNext(MyApp.kotlogram.client.authLogOut()) }
                .doOnNext { if (it == TLBool.TRUE) {
                    MyApp.database.dialogDao().deleteAll()
                    MyApp.database.smsDao().deleteAll()
                } }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Get dialogs result: $it")

                    val prefs = Prefs()
                    prefs.isSignedIn = false

                    mainView.onSignedOut()
                }, {
                    Timber.e(it, "Exception during logout")
                })
    }

    private fun createUser(user: TLUser): User {
        return User(user.firstName, user.lastName)
    }

}
