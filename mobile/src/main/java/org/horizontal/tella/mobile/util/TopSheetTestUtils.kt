package org.horizontal.tella.mobile.util

import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.bottomsheet.Binder
import org.hzontal.shared_ui.bottomsheet.CustomTopSheetFragment
import org.hzontal.shared_ui.bottomsheet.PageHolder
import org.horizontal.tella.mobile.views.fragment.vault.home.background_activities.BackgroundActivitiesAdapter

object TopSheetTestUtils {

    fun showBackgroundActivitiesSheet(
        fragmentManager: FragmentManager,
        titleText: String,
        description: String,
        backgroundActivitiesAdapter: BackgroundActivitiesAdapter,
        descriptionText: LiveData<String>,
        lifecycleOwner: LifecycleOwner,
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
                        titleTv.text = titleText
                        descriptionTv.text = description

                        holder.activitiesRecyclerView.apply {
                            adapter = backgroundActivitiesAdapter
                            layoutManager = LinearLayoutManager(context)
                        }
                    }
                    descriptionText.observe(lifecycleOwner) {
                        holder.descriptionTv.text = it
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