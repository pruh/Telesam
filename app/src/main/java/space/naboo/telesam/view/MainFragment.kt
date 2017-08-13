package space.naboo.telesam.view

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.content.PermissionChecker
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import space.naboo.telesam.R
import space.naboo.telesam.model.Dialog
import space.naboo.telesam.model.User
import space.naboo.telesam.presenter.MainFragmentPresenter

class MainFragment : Fragment(), MainView {

    private val SELECT_GROUP_REQUEST_CODE = 1

    private val presenter by lazy { MainFragmentPresenter(this) }

    private val requestPermissionButton by lazy { view?.findViewById(R.id.request_permission_button) as Button }
    private val permissionsOkText by lazy { view?.findViewById(R.id.permissions_ok) as View }
    private val phoneNumberContainer by lazy { view?.findViewById(R.id.phone_number_container) as View }
    private val phoneNumberEditText by lazy { view?.findViewById(R.id.phone_number) as EditText }
    private val telegramSignInButton by lazy { view?.findViewById(R.id.telegram_sign_in) as Button }
    private val codeContainer by lazy { view?.findViewById(R.id.code_container) as View }
    private val signInText by lazy { view?.findViewById(R.id.signed_in_text) as TextView }
    private val signOutContainer by lazy { view?.findViewById(R.id.sign_out_container) as View }
    private val logout by lazy { view?.findViewById(R.id.logout) as Button }
    private val codeFromTelegramEditText by lazy { view?.findViewById(R.id.telegram_code) as EditText }
    private val enterCodeButton by lazy { view?.findViewById(R.id.code_enter) as Button }
    private val selectGroupText by lazy { view?.findViewById(R.id.telegram_group) as TextView }
    private val selectGroupButton by lazy { view?.findViewById(R.id.select_telegram_group) as Button }
    private val groupContainer by lazy { view?.findViewById(R.id.group_container) as View }
    private val allowInBackgroundButton by lazy { view?.findViewById(R.id.allow_in_background) as Button }

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

        presenter // init presenter
        requestPermissionButton.setOnClickListener { presenter.onGrantPermissionClick(it) }
        allowInBackgroundButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val context = it.context
                val packageName = context.packageName
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:" + packageName))
                startActivity(intent)
            }
        }
        selectGroupButton.setOnClickListener { presenter.loadDialogs(it) }
        telegramSignInButton.setOnClickListener { presenter.onTelegramSignInClick(it, phoneNumberEditText.text) }
        enterCodeButton.setOnClickListener { presenter.onCodeEntered(it, codeFromTelegramEditText.text) }
        logout.setOnClickListener { presenter.logout(it) }
    }

    override fun onResume() {
        super.onResume()

        // need to check background moe as there is no callback
        presenter.updateBackgroundMode()
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

    override fun onSignedIn(user: User) {
        phoneNumberContainer.visibility = View.GONE
        codeContainer.visibility = View.GONE

        signOutContainer.visibility = View.VISIBLE
        signInText.visibility = View.VISIBLE
        signInText.text = getString(R.string.signed_in_text, user.firstName, user.lastName)
        logout.visibility = View.VISIBLE

        groupContainer.visibility = View.VISIBLE
    }

    override fun onSignedOut() {
        phoneNumberContainer.visibility = View.VISIBLE
        codeContainer.visibility = View.GONE

        signOutContainer.visibility = View.GONE
        signInText.visibility = View.GONE
        logout.visibility = View.GONE

        groupContainer.visibility = View.GONE
    }

    override fun onPermissionGranted(permissionGranted: Boolean) {
        if (permissionGranted) {
            permissionsOkText.visibility = View.VISIBLE
            requestPermissionButton.visibility = View.GONE
        } else {
            permissionsOkText.visibility = View.GONE
            requestPermissionButton.visibility = View.VISIBLE
        }
    }

    override fun onCodeRequested() {
        phoneNumberContainer.visibility = View.GONE
        codeContainer.visibility = View.VISIBLE
    }

    override fun onDialogsAvailable(dialogs: List<Dialog>) {
        val f = GroupPickDialogFragment.newInstance(dialogs)
        f.setTargetFragment(this, SELECT_GROUP_REQUEST_CODE)
        f.show(childFragmentManager, GroupPickDialogFragment.TAG)
    }

    override fun onDialogSelected(dialog: Dialog?) {
        if (dialog != null) {
            selectGroupText.visibility = View.VISIBLE

            val ssb = SpannableStringBuilder()
                    .append(getString(R.string.group_set_message))
                    .append(" ")
                    .append(dialog.name, StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            selectGroupText.text = ssb

            selectGroupButton.text = getString(R.string.select_another_telegram_group)
        } else {
            selectGroupText.visibility = View.GONE
            selectGroupButton.text = getString(R.string.select_telegram_group)
        }
    }

    override fun isBackgroundModeEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true

        val context = activity
        val packageName = context.packageName
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + packageName))
        if (intent.resolveActivity(context.packageManager) == null) {
            return true
        }

        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    override fun onBackgroundModeEnabled(enabled: Boolean) {
        allowInBackgroundButton.visibility = if (enabled) View.GONE else View.VISIBLE
    }
}

interface MainView {
    fun checkSelfPermission(permissions: Array<String>): Int
    fun requestPermissions(permissionList: Array<String>, requestCode: Int)
    fun onPermissionGranted(permissionGranted: Boolean)
    fun onCodeRequested()
    fun onDialogsAvailable(dialogs: List<Dialog>)
    fun onSignedIn(user: User)
    fun onSignedOut()
    fun onDialogSelected(dialog: Dialog?)
    fun isBackgroundModeEnabled(): Boolean
    fun onBackgroundModeEnabled(enabled: Boolean)
}
