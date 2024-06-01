package com.lassanit.smotify.display.holder

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lassanit.smotify.R
import com.lassanit.smotify.bases.SmartBase
import com.lassanit.smotify.display.adapter.AppHeaderAdapter
import com.lassanit.smotify.display.adapter.AppMessageAdapter
import com.lassanit.smotify.display.data.AppHeader
import de.hdodenhof.circleimageview.CircleImageView

class AppHeaderHolder(private val view: View) : RecyclerView.ViewHolder(view),
    AppMessageAdapter.Callbacks {
    companion object {
        const val TAG = "AppHeader-Holder-OPEN"
        fun getHolder(context: Context, parent: ViewGroup): AppHeaderHolder {
            return AppHeaderHolder(
                DataHolder.inflate(
                    context, parent, R.layout.holder_row_app_header
                )
            )
        }
    }

    private val logoI: CircleImageView = view.findViewById(R.id.holder_row_app_header_logo)
    private val nameT: TextView = view.findViewById(R.id.holder_row_app_header_title)
    private val countT: TextView = view.findViewById(R.id.holder_row_app_header_count)
    private val extendI: ImageView = view.findViewById(R.id.holder_row_app_header_extend)
    private val goto: FloatingActionButton = view.findViewById(R.id.holder_row_app_header_goto)

    private val selectionC: CheckBox = view.findViewById(R.id.holder_row_app_header_selection)

    private val listR: RecyclerView = view.findViewById(R.id.holder_row_app_header_extended_list)
    private val extensionLay: ConstraintLayout =
        view.findViewById(R.id.holder_row_app_header_extended_layout)
    private lateinit var adapter: AppMessageAdapter

    var onItemClick: ((View, AppHeader) -> Unit)? = null
    var onItemExtend: ((View, AppHeader) -> Unit)? = null
    var onItemLongClick: ((View, AppHeader) -> Unit)? = null
    var onItemSelectionClick: ((View, AppHeader, Boolean) -> Unit)? = null
    var onItemDataChanged: ((String) -> Unit)? = null

    fun handle(
        activity: FragmentActivity,
        data: AppHeader,
        db: SmartBase,
        isSelectionOn: Boolean = false,
        isSelected: Boolean = false,
        itemExtension: AppHeaderAdapter.ItemVisibilityHandler
    ) {

        runCatching { updateData(data) }

        // Selection View
        runCatching {
            selectionC.isChecked = isSelected
            selectionC.setOnClickListener {
                onItemSelectionClick?.let { it(this.itemView, data, selectionC.isChecked) }
            }
            selectionC.visibility = if (isSelectionOn) View.VISIBLE else View.GONE
        }

        // Extended Button
        runCatching {
            setExtensionState(itemExtension.isAppOpened(data.pkg).and(isSelectionOn.not()))
            extendI.setOnClickListener { onItemExtend?.let { it(this.itemView, data) } }
            extendI.visibility = if (isSelectionOn) View.GONE else View.VISIBLE
            setExtensionChildren(data, db, activity)
        }

        // Rest of the View
        runCatching {
            this.view.setOnLongClickListener {
                if (isSelectionOn.not()) {
                    onItemLongClick?.let { it(this.itemView, data) }
                    true
                } else
                    false
            }
            this.view.setOnClickListener {
                if (isSelectionOn) toggleSelectionState()
                else extendI.callOnClick()
            }
        }
    }

    fun updateData(data: AppHeader) {
        runCatching {
            nameT.text = data.pkgName
            val logo = runCatching {
                view.context.packageManager.getApplicationIcon(data.pkg)
            }.getOrElse {
                ResourcesCompat.getDrawable(
                    view.context.resources, R.drawable.android, null
                )
            }
            Glide.with(view).asDrawable().load(logo).into(logoI)
            countT.text = when {
                data.unread > 9 -> {
                    countT.visibility = View.VISIBLE
                    "9+"
                }

                data.unread <= 0 -> {
                    countT.visibility = View.INVISIBLE
                    "0"
                }

                else -> {
                    countT.visibility = View.VISIBLE
                    data.unread.toString()
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun toggleSelectionState() {
        selectionC.isChecked = selectionC.isChecked.not()
        selectionC.callOnClick()
    }

    private fun setExtensionState(show: Boolean) {
        runCatching {
            if (show) {
                extensionLay.visibility = View.VISIBLE
                extendI.rotation = 180F
            } else {
                extendI.rotation = 0F
                extensionLay.visibility = View.GONE
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun setExtensionChildren(data: AppHeader, db: SmartBase, activity: FragmentActivity) {
        runCatching {
            listR.setHasFixedSize(false)
            listR.layoutManager = LinearLayoutManager(activity)
            adapter = AppMessageAdapter(activity, data.pkg, db, this)
            listR.adapter = adapter
            goto.setOnClickListener { v -> onItemClick?.let { it(v, data) } }
        }.onFailure { it.printStackTrace() }
    }

    fun toggleExtendedView() {
        setExtensionState(extensionLay.visibility == View.GONE)
    }

    override fun listDataUpdated(pkg: String) {
        runCatching { onItemDataChanged?.let { it(pkg) } }.onFailure { it.printStackTrace() }
    }
}