package space.naboo.telesam.telegram

import android.os.Build
import android.util.Base64
import com.github.badoualy.telegram.api.TelegramApp
import space.naboo.telesam.BuildConfig
import space.naboo.telesam.MyApp
import kotlin.experimental.xor

internal class TelegramData {

    val apiKey
        get() = getKey(BuildConfig.apiKey).toInt()

    val apiHash
        get() = getKey(BuildConfig.apiHash)

    val appVersion by lazy { BuildConfig.VERSION_NAME }

    val model: String by lazy {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        if (model.startsWith(manufacturer)) {
            model
        } else {
            "$manufacturer $model"
        }
    }

    val systemVersion by lazy { "Android ${Build.VERSION.RELEASE}" }

    val langCode: String by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            MyApp.instance.resources.configuration.locales.get(0).language
        } else {
            //noinspection deprecation
            MyApp.instance.resources.configuration.locale.language
        }
    }

    val telegramApp = TelegramApp(apiId = apiKey, apiHash = apiHash, deviceModel = model,
            systemVersion = systemVersion, appVersion = appVersion, langCode = langCode)

    private fun getKey(key: Array<String>): String {
        val xorParts0 = Base64.decode(key[0], 0)
        val xorParts1 = Base64.decode(key[1], 0)

        val xorKey = ByteArray(xorParts0.size)
        for (i in xorParts1.indices) {
            xorKey[i] = (xorParts0[i] xor xorParts1[i])
        }

        return String(xorKey)
    }

}
