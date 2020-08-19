package rs.readahead.washington.mobile.views.adapters;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class SectionItemOffset extends RecyclerView.ItemDecoration {
    private int spacing;

    public SectionItemOffset(int spacing) {
        this.spacing = spacing;
    }

    @Override
    public void getItemOffsets(@NotNull Rect outRect, @NotNull View view, RecyclerView parent, @NotNull RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view); // item position
        SectionedRecyclerViewAdapter adapter = (SectionedRecyclerViewAdapter) parent.getAdapter();

        if (adapter == null) return;

        if (adapter.getSectionItemViewType(position) != SectionedRecyclerViewAdapter.VIEW_TYPE_FOOTER &&
                adapter.getSectionItemViewType(position) != SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER) {
            outRect.right = spacing / 10;
            outRect.top = spacing / 10;
            outRect.bottom = spacing / 10;
            outRect.left = spacing / 10;
        } else {
            outRect.left = -spacing;
            outRect.right = -spacing;
        }
    }

}



