package space.naboo.telesam

import android.preference.PreferenceManager

class Prefs {

    private val GROUP_ID_KEY = "GROUP_ID_KEY"

    private val prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.instance)

    fun saveGroupId(id: Int) {
        prefs.edit().putInt(GROUP_ID_KEY, id).apply()
    }

    fun getGroupId() = if (prefs.contains(GROUP_ID_KEY))
        prefs.getInt(GROUP_ID_KEY, 0)
    else
        null

}
