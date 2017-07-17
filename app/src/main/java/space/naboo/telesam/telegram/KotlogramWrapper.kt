package space.naboo.telesam.telegram

import com.github.badoualy.telegram.api.Kotlogram
import com.github.badoualy.telegram.mtproto.model.DataCenter

class KotlogramWrapper {

    private val apiStorage = ApiStorage()
    private val testDc = DataCenter("149.154.167.40", 443)
    private val prodDc = DataCenter("149.154.167.50", 443)
    private val telegramData = TelegramData()

    /** do no access this from main thread as Kotlogram makes network request in constructor */
    val client by lazy { Kotlogram.getDefaultClient(telegramData.telegramApp, apiStorage, preferredDataCenter = prodDc) }
}
