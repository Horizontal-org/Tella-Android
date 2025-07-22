package org.hzontal.shared_ui.textviews

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.hzontal.shared_ui.R

class CollapsibleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleText: TextView
    private val collapsibleText: TextView
    private val arrowIcon: ImageView
    private var isExpanded = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_collapsible_text, this, true)

        orientation = VERTICAL

        titleText = findViewById(R.id.titleText)
        collapsibleText = findViewById(R.id.collapsibleText)
        arrowIcon = findViewById(R.id.arrowIcon)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.CollapsibleTextView, 0, 0)
            val title = typedArray.getString(R.styleable.CollapsibleTextView_collapsibleTitle)
            val body = typedArray.getString(R.styleable.CollapsibleTextView_collapsibleBody)
            val expandedByDefault = typedArray.getBoolean(R.styleable.CollapsibleTextView_defaultExpanded, false)
            typedArray.recycle()

            titleText.text = title
            collapsibleText.text = body
            isExpanded = expandedByDefault
            collapsibleText.visibility = if (isExpanded) View.VISIBLE else View.GONE
            arrowIcon.rotation = if (isExpanded) 180f else 0f
        }

        findViewById<View>(R.id.header).setOnClickListener {
            toggleText()
        }
    }

    private fun toggleText() {
        isExpanded = !isExpanded
        collapsibleText.visibility = if (isExpanded) View.VISIBLE else View.GONE
        val rotation = if (isExpanded) 180f else 0f
        arrowIcon.animate().rotation(rotation).start()
    }

    fun setTitle(text: String) {
        titleText.text = text
    }

    fun setContent(text: String) {
        collapsibleText.text = text
    }
}
