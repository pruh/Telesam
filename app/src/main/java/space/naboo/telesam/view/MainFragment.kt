package space.naboo.telesam.view

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.badoualy.telegram.tl.api.TLAbsChat
import space.naboo.telesam.R
import space.naboo.telesam.presenter.MainFragmentPresenter

class MainFragment : Fragment(), MainFragmentView, GroupClickListener {

    private val SELECT_GROUP_REQUEST_CODE = 1

    private val presenter by lazy { MainFragmentPresenter(this) }
    private val requestPermissionButton by lazy { view?.findViewById(R.id.request_permission_button) as Button }
    private val phoneNumberEditText by lazy { view?.findViewById(R.id.phone_number) as EditText }
    private val telegramSignInButton by lazy { view?.findViewById(R.id.telegram_sign_in) as Button }
    private val selectGroupButton by lazy { view?.findViewById(R.id.select_telegram_group) as Button }
    private val codeFromTelegramEditText by lazy { view?.findViewById(R.id.telegram_code) as EditText }
    private val enterCodeButton by lazy { view?.findViewById(R.id.code_enter) as Button }
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

        // to init it here
        presenter
        requestPermissionButton.setOnClickListener { presenter.onGrantPermissionClick(it) }
        selectGroupButton.setOnClickListener { presenter.onSelectGroupClick(it) }
        telegramSignInButton.setOnClickListener { presenter.onTelegramSignInClick(it, phoneNumberEditText.text) }
        enterCodeButton.setOnClickListener { presenter.onCodeEntered(it, codeFromTelegramEditText.text) }
    }

    override fun checkSelfPermission(permissions: Array<String>): Int {
        view?.let { view ->
            permissions.forEach { permission ->
                if (ContextCompat.checkSelfPermission(view.context, permission) == PermissionChecker.PERMISSION_DENIED) {
                    return PermissionChecker.PERMISSION_DENIED
                }
            }
        }

        return PermissionChecker.PERMISSION_GRANTED
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

    override fun onCodeRequested() {

    }

    override fun onGroupsAvailable(groups: List<TLAbsChat>) {
        val f = GroupPickDialogFragment.newInstance(groups)
        f.setTargetFragment(this, SELECT_GROUP_REQUEST_CODE)
        f.show(childFragmentManager, GroupPickDialogFragment.TAG)
    }

    override fun onGroupClick(group: TLAbsChat) {
        presenter.onGroupClick(group)
    }
}

interface MainFragmentView {
    fun checkSelfPermission(permissions: Array<String>): Int
    fun requestPermissions(permissionList: Array<String>, requestCode: Int)
    fun onPermissionGranted(permissionGranted: Boolean)
    fun onCodeRequested()
    fun onGroupsAvailable(groups: List<TLAbsChat>)
}
