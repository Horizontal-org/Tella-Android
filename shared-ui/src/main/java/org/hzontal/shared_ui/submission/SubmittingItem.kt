package org.hzontal.shared_ui.submission

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import org.hzontal.shared_ui.R

class SubmittingItem @JvmOverloads  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {


    private lateinit var partName: TextView
    private lateinit var partIcon: ImageView
    private lateinit var partSize: TextView
    private lateinit var uploadProgress: ProgressBar
    private lateinit var partCheckBox: CheckBox
    private lateinit var partCheckIcon: ImageView

    @DrawableRes
    private var partIconRes: Int = -1
    private var partCheckIconRes : Int = -1
    @StringRes
    var partNameRes: Int = -1
    var partSizeRes: Int = -1
    var checkListener: (() -> Unit)? = null

    private var mChecked = false

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.submit_parts_list_item, this, true)
        initView()
        initListener()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        partName = findViewById(R.id.partName)
        partIcon = findViewById(R.id.partIcon)
        partSize = findViewById(R.id.partSize)
        uploadProgress = findViewById(R.id.uploadProgress)
        partCheckBox = findViewById(R.id.partCheckBox)
        partCheckIcon = findViewById(R.id.partCheckIcon)
    }

    private fun initListener() {
        partCheckBox.setOnClickListener {
            checkListener?.invoke()
        }
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray =
                context.obtainStyledAttributes(attrs, R.styleable.SubmittingItem, defStyleAttr, 0)

            try {
                partNameRes =
                    typedArray.getResourceId(R.styleable.SubmittingItem_partNameRes, -1)
                partIconRes =
                    typedArray.getResourceId(R.styleable.SubmittingItem_partIconRes, -1)
                partSizeRes = typedArray.getResourceId(R.styleable.SubmittingItem_partSizeRes, -1)
                partCheckIconRes = typedArray.getResourceId(R.styleable.SubmittingItem_partCheckIconRes, -1)
            } finally {
                typedArray.recycle()
            }
        }

        bindView()
    }

    private fun bindView() {
        if (partSizeRes != -1) {
            partSize.setBackgroundResource(partSizeRes)
            partSize.isVisible = true
        }
        if (partNameRes != -1){
            partName.text = context.getString(partNameRes)
            partName.isVisible = true
        }
        if (partIconRes != -1){
            partIcon.setBackgroundResource(partIconRes)
            partIcon.isVisible = true
        }
        if (partCheckIconRes != -1){
            partCheckIcon.setBackgroundResource(partCheckIconRes)
            partCheckIcon.isVisible = true
        }
    }

    fun setPartCleared(layout: ViewGroup) {
        layout.findViewById<View>(R.id.uploadProgress).visibility = GONE
        layout.findViewById<View>(R.id.partCheckBox).visibility = GONE
        layout.findViewById<View>(R.id.partCheckIcon).visibility = GONE
    }

    fun setPartPrepared(layout: ViewGroup, offline: Boolean) {
        layout.findViewById<View>(R.id.uploadProgress).visibility = GONE
        layout.findViewById<View>(R.id.partCheckBox).visibility = if (offline) GONE else VISIBLE
        layout.findViewById<View>(R.id.partCheckIcon).visibility = GONE
    }

    fun setPartUploading(layout: ViewGroup) {
        layout.findViewById<View>(R.id.uploadProgress).visibility = VISIBLE
        layout.findViewById<View>(R.id.partCheckBox).visibility = GONE
        layout.findViewById<View>(R.id.partCheckIcon).visibility = GONE
    }

    fun setPartUploaded(layout: ViewGroup) {
        layout.findViewById<View>(R.id.uploadProgress).visibility = GONE
        layout.findViewById<View>(R.id.partCheckBox).visibility = GONE
        layout.findViewById<View>(R.id.partCheckIcon).visibility = VISIBLE
    }

    fun setUploadProgress(partName: String?, pct: Float) {
        if (pct < 0 || pct > 1) {
            return
        }
        if (uploadProgress != null) {
            uploadProgress.progress = (uploadProgress.max * pct).toInt()
        }
    }


}