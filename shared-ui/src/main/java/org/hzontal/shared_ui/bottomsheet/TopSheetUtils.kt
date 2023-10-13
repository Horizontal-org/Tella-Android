package org.hzontal.shared_ui.bottomsheet

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.R

object TopSheetTestUtils {

    fun showBackgroundActivitiesSheet(
        fragmentManager: FragmentManager,
        titleText: String,
        description: String,
    ) {
        val renameFileSheet =
            CustomTopSheetFragment.with(fragmentManager)
                .page(R.layout.background_activities_topsheet)
                .screenTag("BackgroundActivitiesSheet").cancellable(true)
        renameFileSheet.holder(
            BackgroundActivitiesSheetHolder(),
            object : Binder<BackgroundActivitiesSheetHolder> {
                override fun onBind(holder: BackgroundActivitiesSheetHolder) {
                    with(holder) {

                    }
                }
            })
        renameFileSheet.transparentBackground()
        renameFileSheet.launch()
    }

    class BackgroundActivitiesSheetHolder : PageHolder() {

        lateinit var titleTv: TextView
        lateinit var descriptionTv: TextView
        lateinit var activitiesRecyclerView: RecyclerView

        override fun bindView(view: View) {

            titleTv = view.findViewById(R.id.sheet_title_tv)
            descriptionTv = view.findViewById(R.id.sheet_desciption_tv)
            activitiesRecyclerView = view.findViewById(R.id.activities_recycler)
        }
    }

}