package space.naboo.telesam.telegram

import com.github.badoualy.telegram.api.TelegramApiStorage
import com.github.badoualy.telegram.mtproto.auth.AuthKey
import com.github.badoualy.telegram.mtproto.model.DataCenter
import com.github.badoualy.telegram.mtproto.model.MTSession
import space.naboo.telesam.MyApp
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

internal class ApiStorage : TelegramApiStorage {

    private val rootDir = File(MyApp.instance.filesDir, "kotlogram")
    private val authKeyFile = File(rootDir, "auth.key")
    private val nearestDcFile = File(rootDir, "dc.save")
    
    override fun saveAuthKey(authKey: AuthKey) {
        rootDir.mkdirs()

        FileOutputStream(authKeyFile).use {
            it.write(authKey.key)
        }
    }

    override fun loadAuthKey(): AuthKey? {
        if (!authKeyFile.exists()) {
            return null
        }

        FileInputStream(authKeyFile).use {
            return AuthKey(it.readBytes())
        }
    }

    override fun saveDc(dataCenter: DataCenter) {
        rootDir.mkdirs()

        FileOutputStream(nearestDcFile).use {
            it.write("${dataCenter.ip}:${dataCenter.port}".toByteArray())
        }
    }

    override fun loadDc(): DataCenter? {
        if (!nearestDcFile.exists()) {
            return null
        }

        FileInputStream(nearestDcFile).use {
            val raw = String(it.readBytes()).split(":")
            return DataCenter(raw[0], raw[1].toInt())
        }
    }

    override fun deleteAuthKey() {
        authKeyFile.delete()
    }

    override fun deleteDc() {
        nearestDcFile.delete()
    }

    override fun saveSession(session: MTSession?) {

    }

    override fun loadSession(): MTSession? {
        return null
    }
}