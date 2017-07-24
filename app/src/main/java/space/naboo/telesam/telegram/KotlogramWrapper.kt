package space.naboo.telesam.telegram

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.mtproto.model.DataCenter
import timber.log.Timber

class KotlogramWrapper {

    private val apiStorage = ApiStorage()
    private val testDc = DataCenter("149.154.167.40", 443)
    private val prodDc = DataCenter("149.154.167.50", 443)
    private val telegramData = TelegramData()

    /** do no access this from main thread as Kotlogram makes network request in constructor */
    val client by lazy {
        Timber.d("kotlogram client created")
        Kotlogram.getDefaultClient(telegramData.telegramApp, apiStorage, preferredDataCenter = prodDc)
    }

}
