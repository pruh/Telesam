package space.naboo.telesam.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import space.naboo.telesam.Prefs
import space.naboo.telesam.R
import space.naboo.telesam.model.Group

class GroupPickDialogFragment : DialogFragment() {

    companion object {
        val TAG: String = GroupPickDialogFragment::class.java.simpleName
        private val GROUPS_KEY = "GROUPS_KEY"

        fun newInstance(groups: List<Group>): GroupPickDialogFragment {
            val f = GroupPickDialogFragment()
            val args = Bundle()
            if (groups is ArrayList) {
                args.putParcelableArrayList(GROUPS_KEY, groups)
            } else {
                args.putParcelableArrayList(GROUPS_KEY, ArrayList(groups))
            }
            f.arguments = args
            return f
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = activity
        val recyclerView = View.inflate(context, R.layout.group_pick_layout, null) as RecyclerView
        val dialog = AlertDialog.Builder(context)
                .setTitle(R.string.select_telegram_group)
                .setView(recyclerView)
                .create()

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = GroupsAdapter(arguments.getParcelableArrayList<Group>(GROUPS_KEY), object : GroupClickListener {
            override fun onGroupSelected(group: Group?) {
                Log.v(TAG, "selected group: $group")

                dismiss()

                Prefs().groupId = group?.id ?: 0
                (targetFragment as GroupClickListener).onGroupSelected(group)
            }
        })

        return dialog
    }

}

private class GroupsAdapter(val groups: List<Group>, val groupClickListener: GroupClickListener)
        : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]

        holder.textView.text = group.name
        holder.itemView.setOnClickListener { groupClickListener.onGroupSelected(group) }
    }

    override fun getItemCount() = groups.size

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView by lazy { view as TextView }
    }
}

interface GroupClickListener {
    fun onGroupSelected(group: Group?)
}
