package rs.readahead.washington.mobile.views.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.CollectRelationshipPreviewViewBinding;
import rs.readahead.washington.mobile.domain.entity.uwazi.NestedSelectValue;


public class CollectRelationShipPreviewView extends LinearLayout {
    private final CollectRelationshipPreviewViewBinding binding;

    private NestedSelectValue relationShip;

    public CollectRelationShipPreviewView(Context context) {
        this(context, null, null);
    }

    public CollectRelationShipPreviewView(Context context, AttributeSet attrs, NestedSelectValue relationShip) {
        this(context, attrs, 0);
        this.relationShip = relationShip;
        showMediaFileInfo();

    }

    public CollectRelationShipPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        binding = CollectRelationshipPreviewViewBinding.inflate(LayoutInflater.from(context), this, true);
    }


    private void showMediaFileInfo() {
        binding.fileName.setText(relationShip.getLabel());
        binding.thumbView.setImageResource(R.drawable.relation_ship_icon);
    }
}
