package org.hzontal.shared_ui.submission

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import org.hzontal.shared_ui.databinding.SubmitedListItemBinding

class SubmittedItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: SubmitedListItemBinding = SubmitedListItemBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    private val name: TextView = binding.name
    private val organization: TextView = binding.organization
    private val updated: TextView = binding.updated
    private val icon: ImageView = binding.icon
    val popupMenu: ImageButton = binding.popupMenu

    @DrawableRes
    private var iconRes: Int = -1
    private var popupMenuRes: Int = -1

    @StringRes
    var nameRes: Int = -1
    var organizationRes: Int = -1
    var updatedRes: Int = -1
    var popClickListener: (() -> Unit)? = null

    init {
        extractAttributes(attrs, defStyleAttr)
        popupMenu.setOnClickListener { popClickListener?.invoke() }
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(attrs, org.hzontal.shared_ui.R.styleable.SubmittedItem, defStyleAttr, 0)
            try {
                iconRes = typedArray.getResourceId(org.hzontal.shared_ui.R.styleable.SubmittedItem_iconRes, -1)
                popupMenuRes = typedArray.getResourceId(org.hzontal.shared_ui.R.styleable.SubmittedItem_popupMenuRes, -1)
                nameRes = typedArray.getResourceId(org.hzontal.shared_ui.R.styleable.SubmittedItem_nameRes, -1)
                organizationRes = typedArray.getResourceId(org.hzontal.shared_ui.R.styleable.SubmittedItem_organizationRes, -1)
                updatedRes = typedArray.getResourceId(org.hzontal.shared_ui.R.styleable.SubmittedItem_updatedRes, -1)
            } finally {
                typedArray.recycle()
            }
        }

        bindView()
    }

    private fun bindView() {
        if (iconRes != -1) {
            icon.setBackgroundResource(iconRes)
            icon.isVisible = true
        }
        if (nameRes != -1) {
            name.text = context.getString(nameRes)
            name.isVisible = true
        }
        if (popupMenuRes != -1) {
            popupMenu.setBackgroundResource(popupMenuRes)
            popupMenu.isVisible = true
        }
        if (updatedRes != -1) {
            updated.setBackgroundResource(updatedRes)
            updated.isVisible = true
        }
        if (organizationRes != -1) {
            organization.setBackgroundResource(organizationRes)
            organization.isVisible = true
        }
    }

    fun setName(partName: String) {
        name.text = partName
    }

    fun setOrganization(organizationName: String?) {
        if (organizationName == null) {
            organization.isVisible = false
        } else {
            organization.isVisible = true
            organization.text = organizationName
        }
    }

    fun setIcon(iconId: Int) {
        icon.setImageResource(iconId)
    }

    fun setIconDrawable(drawable: Drawable?) {
        if (drawable == null) {
            icon.isVisible = false
        } else {
            icon.isVisible = true
            icon.setImageDrawable(drawable)
        }
    }

    fun setUpdated(updateTime: String) {
        updated.isVisible = true
        updated.text = updateTime
    }
}
