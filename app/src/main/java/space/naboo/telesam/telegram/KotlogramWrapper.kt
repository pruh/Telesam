package space.naboo.telesam.telegram

import android.util.Log
import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.mtproto.model.DataCenter

class KotlogramWrapper {

    private val TAG: String = KotlogramWrapper::class.java.simpleName

    private val apiStorage = ApiStorage()
    private val testDc = DataCenter("149.154.167.40", 443)
    private val prodDc = DataCenter("149.154.167.50", 443)
    private val telegramData = TelegramData()

    /** do no access this from main thread as Kotlogram makes network request in constructor */
    val client by lazy {
        Log.v(TAG, "kotlogram client created")
        Kotlogram.getDefaultClient(telegramData.telegramApp, apiStorage, preferredDataCenter = prodDc)
    }

}
