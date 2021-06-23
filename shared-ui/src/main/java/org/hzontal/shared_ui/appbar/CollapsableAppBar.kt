package org.hzontal.shared_ui.appbar

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.widget.Toolbar
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import org.hzontal.shared_ui.R

class CollapsableAppBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr){

    @StringRes
    private var titleRes: Int = -1
    @DrawableRes
    private var contentScrimRes: Int = -1
    @DrawableRes
    private var backgroundRes: Int = -1

    private lateinit var appBar: AppBarLayout
    private lateinit var collapsableAppBar: CollapsingToolbarLayout
    lateinit var toolbar: Toolbar

    init {
        LayoutInflater.from(context).inflate(R.layout.collapsable_appbar_layout, this, true)
        initView()
        extractAttributes(attrs, defStyleAttr)
    }

    private fun initView() {
        appBar = findViewById(R.id.appbar)
        collapsableAppBar = findViewById(R.id.collapsing_toolbar)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun extractAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        attrs?.let {
            val typedArray = context
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.CollapsableAppBar,
                    defStyleAttr,
                    defStyleAttr
                )

            try {
                titleRes =
                    typedArray.getResourceId(R.styleable.CollapsableAppBar_titleRes, -1)
                contentScrimRes =
                    typedArray.getResourceId(R.styleable.CollapsableAppBar_contentScrimRes, -1)
                backgroundRes =
                    typedArray.getResourceId(R.styleable.CollapsableAppBar_backgroundRes, -1)
            } finally {
                typedArray.recycle()
            }
        }
        bindView()
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun bindView() {
        if (titleRes != -1) {collapsableAppBar.title = context.getString(titleRes)}
        if (contentScrimRes != -1) {collapsableAppBar.contentScrim = ContextCompat.getDrawable(context,contentScrimRes)}
        if (backgroundRes != -1) {collapsableAppBar.background = ContextCompat.getDrawable(context,backgroundRes)}
    }
}