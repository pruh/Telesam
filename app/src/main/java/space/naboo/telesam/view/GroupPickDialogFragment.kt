package space.naboo.telesam.view

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.badoualy.telegram.tl.api.TLAbsChat
import com.github.badoualy.telegram.tl.api.TLChat
import space.naboo.telesam.R
import java.io.Serializable

class GroupPickDialogFragment : DialogFragment() {

    private val recyclerView by lazy { view?.findViewById(R.id.recycler_view) as RecyclerView }
    private val groupClickListener by lazy { targetFragment as GroupClickListener }

    companion object {
        val TAG: String = GroupPickDialogFragment::class.java.simpleName
        private val GROUPS_KEY = "GROUPS_KEY"

        fun newInstance(groups: List<TLAbsChat>): GroupPickDialogFragment {
            val f = GroupPickDialogFragment()
            val args = Bundle()
            args.putSerializable(GROUPS_KEY, groups as Serializable)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.group_pick_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView.layoutManager = LinearLayoutManager(view.context)

        @Suppress("UNCHECKED_CAST")
        recyclerView.adapter = GroupsAdapter(arguments.getSerializable(GROUPS_KEY) as List<TLAbsChat>, object : GroupClickListener {
            override fun onGroupClick(group: TLAbsChat) {
                dismiss()
                groupClickListener.onGroupClick(group)
            }
        })
    }

}

private class GroupsAdapter(val list: List<TLAbsChat>,
        val groupClickListener: GroupClickListener) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        list[position].let {
            when (it) {
                is TLChat -> holder.textView.text = it.title
                else -> holder.textView.text = holder.textView.context.getString(R.string.unknown_group)
            }
        }

        holder.itemView.setOnClickListener { groupClickListener.onGroupClick(list[position]) }
    }

    override fun getItemCount() = list.size

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView by lazy { view as TextView }
    }
}

interface GroupClickListener {
    fun onGroupClick(group: TLAbsChat)
}
