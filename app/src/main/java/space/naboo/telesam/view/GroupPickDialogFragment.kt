package space.naboo.telesam.view

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.R
import space.naboo.telesam.model.Dialog
import timber.log.Timber

typealias AndroidDialog = android.app.Dialog

class GroupPickDialogFragment : DialogFragment() {

    companion object {
        val TAG: String = GroupPickDialogFragment::class.java.simpleName
        private val DIALOGS_KEY = "DIALOGS_KEY"

        fun newInstance(dialogs: List<Dialog>): GroupPickDialogFragment {
            val f = GroupPickDialogFragment()
            val args = Bundle()
            if (dialogs is ArrayList) {
                args.putParcelableArrayList(DIALOGS_KEY, dialogs)
            } else {
                args.putParcelableArrayList(DIALOGS_KEY, ArrayList(dialogs))
            }
            f.arguments = args
            return f
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AndroidDialog {
        val context = activity
        val recyclerView = View.inflate(context, R.layout.group_pick_layout, null) as RecyclerView
        val alertDialog = AlertDialog.Builder(context)
                .setTitle(R.string.select_telegram_group)
                .setView(recyclerView)
                .create()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = GroupsAdapter(arguments.getParcelableArrayList<Dialog>(DIALOGS_KEY), object : DialogClickListener {
            override fun onDialogSelected(dialog: Dialog) {
                Timber.v("selected dialog: $dialog")

                storeSelectedDialog(dialog)
                (targetFragment as MainView).onDialogSelected(dialog)

                dismiss()
            }
        })

        return alertDialog
    }

    fun storeSelectedDialog(dialog: Dialog) {
        Timber.d("Saving dialog: $dialog")

        Observable.fromCallable {
            MyApp.database.dialogDao().deleteAll()
            MyApp.database.dialogDao().insert(dialog) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Timber.d("Dialog saved")
                }, {
                    Timber.e(it, "Exception while saving dialog to database")
                })
    }

}

private class GroupsAdapter(val dialogs: List<Dialog>, val dialogClickListener: DialogClickListener)
        : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = dialogs[position]

        holder.textView.text = group.name
        holder.itemView.setOnClickListener { dialogClickListener.onDialogSelected(group) }
    }

    override fun getItemCount() = dialogs.size

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView by lazy { view as TextView }
    }
}

private interface DialogClickListener {
    fun onDialogSelected(dialog: Dialog)
}
