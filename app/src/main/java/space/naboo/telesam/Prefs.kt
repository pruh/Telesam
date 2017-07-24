package space.naboo.telesam

import android.preference.PreferenceManager

class Prefs {

    private val SIGNED_IN_KEY = "SIGNED_IN_KEY"

    private val prefs = PreferenceManager.getDefaultSharedPreferences(MyApp.instance)

    // todo should check making api call
    var isSignedIn: Boolean
        get() = prefs.getBoolean(SIGNED_IN_KEY, false)
        set(value) = prefs.edit().putBoolean(SIGNED_IN_KEY, value).apply()

    fun migrate(startVersion: Int, endVersion: Int) {
        if (startVersion == 1 && endVersion == 2) {
            prefs.edit().remove("GROUP_ID_KEY").apply()
        }
    }

}
