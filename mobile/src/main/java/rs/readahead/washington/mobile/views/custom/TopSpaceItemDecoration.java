package rs.readahead.washington.mobile.views.custom;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class TopSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;

    public TopSpaceItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.top = (parent.getChildAdapterPosition(view) > 0 ? space : 0);
    }
}
