package org.horizontal.tella.mobile.views.custom

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class SpaceAtEndDecoration(private val space: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)

        // Get the position of the current item
        val position = parent.getChildAdapterPosition(view)
        val itemCount = state.itemCount

        // Add space only to the last item
        if (position == itemCount - 1) {
            outRect.bottom = space
        }
    }
}
