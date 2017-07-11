package space.naboo.telesam.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import space.naboo.telesam.R
import space.naboo.telesam.telegram.TelegramData
import space.naboo.telesam.presenter.MainFragmentPresenter

class MainFragment : Fragment(), MainFragmentView {

    private val presenter by lazy { MainFragmentPresenter(this) }
    private val requestPermissionButton by lazy { view?.findViewById(R.id.request_permission_button) as Button }
    private val okTextView by lazy { view?.findViewById(R.id.text_view) as TextView }

    companion object {
        val TAG: String = MainFragment::class.java.simpleName

        fun newInstance(): MainFragment {
            return MainFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.main, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.init()
        requestPermissionButton.setOnClickListener { presenter.onGrantPermissionClick(it) }
    }

    override fun checkSelfPermission(permission: String): Int? {
        return view?.let {
            ContextCompat.checkSelfPermission(it.context, permission)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        presenter.onPermissionResult(requestCode, permissions, grantResults)
    }

    override fun onPermissionGranted(permissionGranted: Boolean) {
        if (permissionGranted) {
            okTextView.visibility = View.VISIBLE
            requestPermissionButton.visibility = View.GONE
        } else {
            okTextView.visibility = View.GONE
            requestPermissionButton.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()

        Log.d(TAG, "api key: ${TelegramData.test()}")
    }
}

interface MainFragmentView {
    fun checkSelfPermission(permission: String): Int?
    fun requestPermissions(permissionList: Array<String>, requestCode: Int)
    fun onPermissionGranted(permissionGranted: Boolean)
}
