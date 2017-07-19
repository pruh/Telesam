package space.naboo.telesam

import android.preference.PreferenceManager

class Prefs {

    private val SIGNED_IN_KEY = "SIGNED_IN_KEY"
    private val GROUP_ID_KEY = "GROUP_ID_KEY"

    private val prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.instance)

    var isSignedIn: Boolean
        get() = prefs.getBoolean(SIGNED_IN_KEY, false)
        set(value) = prefs.edit().putBoolean(SIGNED_IN_KEY, value).apply()

    // todo is there any telegram group with id 0
    var groupId: Int
        get() = prefs.getInt(GROUP_ID_KEY, 0)
        set(value) = prefs.edit().putInt(GROUP_ID_KEY, value).apply()

}
