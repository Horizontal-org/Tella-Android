package rs.readahead.washington.mobile.views.custom;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.hzontal.tella_vault.VaultFile;
import com.hzontal.utils.MediaFile;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.databinding.CollectAttachemntPreviewViewBinding;
import rs.readahead.washington.mobile.databinding.CollectRelationshipPreviewViewBinding;
import rs.readahead.washington.mobile.mvp.contract.ICollectAttachmentMediaFilePresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectAttachmentMediaFilePresenter;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziRelationShipEntity;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.views.activity.viewer.AudioPlayActivity;
import rs.readahead.washington.mobile.views.activity.viewer.PhotoViewerActivity;
import rs.readahead.washington.mobile.views.activity.viewer.VideoViewerActivity;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;


public class CollectRelationShipPreviewView extends LinearLayout{
    private final CollectRelationshipPreviewViewBinding binding;

    private UwaziRelationShipEntity relationShip;

    public CollectRelationShipPreviewView(Context context) {
        this(context, null,null);
    }

    public CollectRelationShipPreviewView(Context context, AttributeSet attrs, UwaziRelationShipEntity relationShip) {
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
