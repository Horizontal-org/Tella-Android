package org.horizontal.tella.mobile.views.fragment.peertopeer.senderflow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.horizontal.tella.mobile.MyApplication
import org.horizontal.tella.mobile.databinding.FragmentUploadFilesBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment
import org.horizontal.tella.mobile.views.fragment.uwazi.widgets.ReportsFormEndView

class UploadFilesFragment :
    BaseBindingFragment<FragmentUploadFilesBinding>(FragmentUploadFilesBinding::inflate) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }


    private fun showFormEndView() {
        /*   if (reportInstance == null) {
               return
           }

           reportInstance?.let { reportFormInstance ->

               endView = ReportsFormEndView(
                   activity,
                   reportFormInstance.title,
                   reportFormInstance.description,
               )
               endView.setInstance(
                   reportFormInstance, MyApplication.isConnectedToInternet(baseActivity), false
               )
               binding.endViewContainer.removeAllViews()
               binding.endViewContainer.addView(endView)
               endView.clearPartsProgress(reportFormInstance)
           }*/
    }

}