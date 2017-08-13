package space.naboo.telesam.view

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.github.badoualy.telegram.tl.api.TLInputFileLocation
import com.github.badoualy.telegram.tl.api.upload.TLFile
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import space.naboo.telesam.MyApp
import space.naboo.telesam.R
import space.naboo.telesam.cache.LruImageCache
import space.naboo.telesam.model.Dialog
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

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
                Timber.d("selected dialog: $dialog")

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

private class GroupsAdapter(private val dialogs: List<Dialog>, private val dialogClickListener: DialogClickListener)
        : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    // should be divisible by 1KB
    private val IMAGE_LIMIT = 512 * 1024

    private val downloads = HashMap<ImageView, Disposable>()

    // let it init in background thread
    private val imageCache by lazy { LruImageCache() }

    init {
        Observable
                .interval(1000, TimeUnit.MILLISECONDS)
                .subscribe { Timber.d("Jobs in map: ${downloads.size}") }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false))
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val dialog = dialogs[position]

        holder.textView.text = dialog.name
        holder.itemView.setOnClickListener { dialogClickListener.onDialogSelected(dialog) }

        loadDialogPhoto(holder.imageView, dialog)
    }

    private fun loadDialogPhoto(imageView: ImageView, dialog: Dialog) {
        imageView.setImageBitmap(null)

        downloads[imageView]?.dispose()
        downloads.remove(imageView)

        val fileLocation = dialog.fileLocation
        if (fileLocation == null) {
            Timber.i("Dialog ${dialog.name} has no image")
            return
        }

        val tlInputFileLocation = TLInputFileLocation(fileLocation.volumeId, fileLocation.localId, fileLocation.secret)

        val showingActual = AtomicBoolean(false)
        val d = Observable.merge<SourceBitmapData>(getFromCache(tlInputFileLocation), getFromNetwork(tlInputFileLocation))
                .filter { data ->
                    val needUpdate = !showingActual.get()
                    if (!data.fromCache) {
                        Timber.d("Received image from internet")
                        showingActual.set(true)
                    } else {
                        Timber.d("Received image from cache")
                    }
                    needUpdate
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ (bitmap) ->
                    Timber.d("setting image for ${dialog.name}")
                    imageView.setImageBitmap(bitmap)
                }, {
                    Timber.w(it, "Cannot download dialog image for ${dialog.name}")
                    downloads.remove(imageView)
                }, {
                    downloads.remove(imageView)
                })

        downloads.put(imageView, d)
    }

    private fun getFromCache(tlInputFileLocation: TLInputFileLocation): Observable<SourceBitmapData> {
        return Observable
                .fromCallable {
                    Timber.d("Loading from cache")
                    val key = getCacheImageName(tlInputFileLocation)
                    imageCache.get(key)
                }
                .map { SourceBitmapData(it, true) }
                .onErrorResumeNext(Observable.empty<SourceBitmapData>())
    }

    private fun getFromNetwork(tlInputFileLocation: TLInputFileLocation): Observable<SourceBitmapData> {
        return Observable
                .fromCallable {
                    Timber.d("Loading from internet")
                    val client = MyApp.kotlogram.client
                    val file = client.uploadGetFile(tlInputFileLocation, 0, IMAGE_LIMIT)

                    file as? TLFile ?: throw IllegalArgumentException("Not a TLFile instance")
                }
                .map { file ->
                    BitmapFactory.decodeByteArray(file.bytes.data, file.bytes.offset, file.bytes.length)
                }
                .doOnNext { bitmap ->
                    val key = getCacheImageName(tlInputFileLocation)
                    imageCache.add(key, bitmap)
                }
                .map { SourceBitmapData(it, false) }
    }

    private fun getCacheImageName(tlInputFileLocation: TLInputFileLocation): String {
        var result = tlInputFileLocation.localId.hashCode()
        result = 31 * result + tlInputFileLocation.secret.hashCode()
        result = 31 * result + tlInputFileLocation.volumeId.hashCode()
        return result.toString()
    }

    override fun getItemCount() = dialogs.size

    override fun onViewDetachedFromWindow(holder: GroupViewHolder) {
        downloads[holder.imageView]?.dispose()
        downloads.remove(holder.imageView)
    }

    class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView by lazy { view.findViewById(R.id.title) as TextView }
        val imageView by lazy { view.findViewById(R.id.image) as ImageView }
    }

    data class SourceBitmapData(val bitmap: Bitmap, val fromCache: Boolean)
}

private interface DialogClickListener {
    fun onDialogSelected(dialog: Dialog)
}
