package org.horizontal.tella.mobile.views.base_ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import org.horizontal.tella.mobile.views.activity.MetadataActivity
import timber.log.Timber

open class MetadataBaseLockFragment  : Fragment(){

    protected lateinit var metadataActivity: MetadataActivity


    override fun onAttach(context: Context) {
        Timber.d("***** ${this.javaClass.name} onAttach")

        super.onAttach(context)

        metadataActivity = context as MetadataActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("***** ${this.javaClass.name} onCreateView")

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("***** ${this.javaClass.name} onViewCreated")

        super.onViewCreated(view, savedInstanceState)
    }
    override fun onResume() {
        metadataActivity.restrictActivity()
        super.onResume()
    }

    protected open fun nav(): NavController {
        return NavHostFragment.findNavController(this)
    }

    open fun back() {
        nav().navigateUp()
    }

    open fun onBackPressed() = true

    protected fun showToast(message: String) {
        if (isAdded) {
            metadataActivity.showToast(message)
        }
    }
}