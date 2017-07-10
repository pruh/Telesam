package space.naboo.telesam.presenter

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import space.naboo.telesam.view.MainFragmentView

class MainFragmentPresenter(val mainView: MainFragmentView) {

    private val PERMISSION_REQ_CODE = 1

    fun onGrantPermissionClick(view: View) {
        val permission = Manifest.permission.RECEIVE_SMS
        if (mainView.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            mainView.requestPermissions(arrayOf(permission), PERMISSION_REQ_CODE)
        }
    }

    fun onPermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQ_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mainView.onPermissionGranted(true)
            } else {
                mainView.onPermissionGranted(false)
            }
        }
    }

    fun init() {
        val permission = Manifest.permission.RECEIVE_SMS
        val grant = mainView.checkSelfPermission(permission)
        if (grant == PackageManager.PERMISSION_GRANTED) {
            mainView.onPermissionGranted(true)
        } else {
            mainView.onPermissionGranted(false)
        }
    }
}
