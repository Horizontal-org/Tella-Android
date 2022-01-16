package org.hzontal.shared_ui.submission

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import org.hzontal.shared_ui.R
import java.text.DecimalFormat


class SubmittingItem @JvmOverloads  constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var partName: TextView
    private lateinit var partIcon: ImageView
    private lateinit var partSize: TextView
    private lateinit var uploadProgress: ProgressBar
    private lateinit var partCheckIcon: ImageView
    private var filePartSize: Long = 0

    @DrawableRes
    private var partIconRes: Int = -1
    private var partCheckIconRes : Int = -1
    @StringRes
    var partNameRes: Int = -1
    var partSizeRes: Int = -1

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.submit_parts_list_item, this, true)
        initView()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        partName = findViewById(R.id.partName)
        partIcon = findViewById(R.id.partIcon)
        partSize = findViewById(R.id.partSize)
        uploadProgress = findViewById(R.id.uploadProgress)
        partCheckIcon = findViewById(R.id.partCheckIcon)
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

    fun setPartCleared() {
        uploadProgress.visibility = GONE
        partCheckIcon.visibility = GONE
    }

    fun setPartPrepared(offline: Boolean) {
        uploadProgress.visibility = GONE
        partCheckIcon.visibility = GONE
    }

    fun setPartUploading() {
        uploadProgress.visibility = VISIBLE
        partCheckIcon.visibility = GONE
    }

    fun setPartUploaded() {
        uploadProgress.visibility = GONE
        partCheckIcon.visibility = VISIBLE
        partSize.setText(getFileSizeString(filePartSize))
    }

    fun setUploadProgress(pct: Float) {
        if (pct < 0 || pct > 1) {
            return
        }
        uploadProgress.progress = (uploadProgress.max * pct).toInt()
        partSize.setText(getUploadedFileSize(pct, filePartSize))
    }

    fun setPartName(nameId: Int){
        partName.setText(nameId)
    }

    fun setPartName(name: String){
        partName.setText(name)
    }

    fun setPartSize(size: Long){
        filePartSize = size
        partSize.setText(getUploadedFileSize(0.0f, size))
    }

    fun setPartIcon(iconId: Int){
        partIcon.setImageResource(iconId)
    }

    private fun getUploadedFileSize(pct: Float, size: Long ) : String {
        val uploadedSize = (pct * size)
        val fileSize = getFileSizeString (uploadedSize.toLong()) +" / " + getFileSizeString (size)
        return fileSize
    }

    private fun getFileSizeString(size: Long ) : String {
        val fileSize: String
        val m = size / 1000000.0
        val k = size / 1000.0
        val dec = DecimalFormat("0.00")
        fileSize = if (m > 1) {
            dec.format(m) + " MB"
        } else {
            dec.format(k) + " KB"
        }
        return fileSize
    }
}