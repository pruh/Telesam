package space.naboo.telesam.view

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(android.R.id.content, MainFragment.newInstance(), MainFragment.TAG)
                    .commit()
        }
    }
}

