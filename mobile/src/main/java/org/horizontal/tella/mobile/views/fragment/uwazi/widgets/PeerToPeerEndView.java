package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.hzontal.utils.MediaFile;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.domain.entity.EntityStatus;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFile;
import org.horizontal.tella.mobile.domain.entity.collect.FormMediaFileStatus;
import org.horizontal.tella.mobile.domain.entity.peertopeer.PeerToPeerInstance;
import org.horizontal.tella.mobile.util.FileUtil;
import org.horizontal.tella.mobile.util.Util;
import org.hzontal.shared_ui.submission.SubmittingItem;

import java.util.Objects;

/**
 * Created by wafa on 9/7/2025.
 */
public class PeerToPeerEndView extends FrameLayout {
    LinearLayout partsListView;
    TextView titleView;
    TextView formStatusTextView;
    TextView formSizeView;
    String title;
    LinearProgressIndicator totalProgress;
    long formSize = 0L;
    private PeerToPeerInstance instance;
    private boolean previewUploaded;

    public PeerToPeerEndView(Context context, String title) {
        super(context);
        inflate(context, R.layout.reports_form_end_view, this);

        this.title = title;

        titleView = findViewById(R.id.title);
        titleView.setText(title);

        totalProgress = findViewById(R.id.totalProgress);
        formSizeView = findViewById(R.id.formSize);

        formStatusTextView = findViewById(R.id.form_status);
    }

    public void setInstance(@NonNull PeerToPeerInstance instance, boolean offline, boolean previewUploaded) {
        this.instance = instance;
        this.previewUploaded = previewUploaded;
        refreshInstance(offline);
    }

    public void refreshInstance(boolean offline) {
        if (this.instance == null) {
            return;
        }

        TextView formNameView = findViewById(R.id.title);

        formNameView.setText(Objects.requireNonNull(instance.getTitle()));

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.size;
        }
        setFormSizeLabel(instance, 0);
        uploadProgressVisibity(instance, offline);
        setUploadProgress(instance, 0);
    }

    void setFormSizeLabel(@NonNull PeerToPeerInstance instance, int percent) {
        String title;
        if (instance.getWidgetMediaFiles().isEmpty()) {
            title = getStatusLabel(instance.getStatus());
            formSizeView.setText(title);
            return;
        }
        switch (instance.getStatus()) {
            case SUBMITTED:
                title = getStatusLabel(instance.getStatus()) + "\n" +
                        getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files,
                                instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ", " +
                        FileUtil.getFileSizeString(formSize);
                break;
            case PAUSED:
                title = getStatusLabel(instance.getStatus()) + "\n" +
                        getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files,
                                instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ", " +
                        getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize);
                break;
            case FINALIZED:
            case SUBMISSION_PENDING:
            case SUBMISSION_ERROR:
                title = getStatusLabel(instance.getStatus()) + "\n" +
                        getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files,
                                instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ", " +
                        getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize) + " " +
                        getResources().getString(R.string.File_Uploaded);
                break;
            default:
                title = percent + "% " + getResources().getString(R.string.File_Uploaded) + "\n" +
                        getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files,
                                instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ",  " +
                        getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize) + " " +
                        getResources().getString(R.string.File_Uploaded);
                break;
        }

        formSizeView.setText(title);
    }


    String getStatusLabel(EntityStatus status) {
        String title = "";
        if (status == EntityStatus.SUBMITTED) {
            title = "";
            //getResources().getString(R.string.File_Uploaded_on) + " " + Util.getDateTimeString(instance.getUpdated());
        } else if (status == EntityStatus.PAUSED) {
            title = getResources().getString(R.string.Paused_Report);
        } else if (status == EntityStatus.FINALIZED || instance.getStatus() == EntityStatus.SUBMISSION_PENDING || instance.getStatus() == EntityStatus.SUBMISSION_ERROR) {
            title = getResources().getString(R.string.Report_Waiting_For_Connection);
        }
        return title;
    }

    private void uploadProgressVisibity(PeerToPeerInstance instance, Boolean isOnline) {
        if (!isOnline || instance.getWidgetMediaFiles().isEmpty()) {
            totalProgress.setVisibility(GONE);
            return;
        }

        if (instance.getStatus() == EntityStatus.SUBMITTED) {
            totalProgress.setVisibility(GONE);
        } else {
            totalProgress.setVisibility(VISIBLE);
        }
    }

    public void setUploadProgress(PeerToPeerInstance instance, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        int percentComplete;

        if (instance.getWidgetMediaFiles().size() > 1) {
            percentComplete = getTotalUploadedSizePercent(instance);
        } else {
            percentComplete = (int) (pct * 100);
        }

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                SubmittingItem item = partsListView.findViewWithTag(mediaFile.getVaultFile().id);
                item.setPartUploaded();
            }
        }

        //Timber.d("***Test*** PCT " + pct + "\n getTotalUploadedSize " + getTotalUploadedSize(instance) + "\n FormSize " + formSize + "\n percentComplete " + percentComplete + " \n Math.round(percentComplete) " + Math.toIntExact(percentComplete));
        totalProgress.setProgressCompat(percentComplete, true);
        setFormSizeLabel(instance, percentComplete);
    }

    private int getTotalUploadedSizePercent(PeerToPeerInstance instance) {
        int totalUploadedSize = 0;
        for (FormMediaFile formMediaFile : instance.getWidgetMediaFiles()) {
            totalUploadedSize += formMediaFile.uploadedSize;
        }
        if (totalUploadedSize > 0) {
            float percent = ((float) (totalUploadedSize * 1.0) / formSize);
            return Math.round(percent * 100);

        } else {
            return 0;
        }
    }

    private String getTotalUploadedSize(PeerToPeerInstance instance) {
        long totalUploadedSize = 0;
        for (FormMediaFile formMediaFile : instance.getWidgetMediaFiles()) {
            totalUploadedSize += formMediaFile.uploadedSize;
        }
        return FileUtil.getFileSize(totalUploadedSize);
    }

    public void clearPartsProgress(PeerToPeerInstance instance) {
        setPartsCleared(instance);
    }


    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        ImageView thumbView = item.findViewById(R.id.fileThumb);
        item.setTag(mediaFile.getPartName());

        item.setPartName(mediaFile.name);
        item.setPartSize(mediaFile.size);

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType))) {
            Glide.with(getContext())
                    .load(mediaFile.thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(thumbView);
        } else if (MediaFile.INSTANCE.isAudioFileType(mediaFile.mimeType)) {
            item.setPartIcon(R.drawable.ic_headset_white_24dp);
        } else {
            item.setPartIcon(R.drawable.ic_attach_file_white_24dp);
        }

        if (mediaFile.status == FormMediaFileStatus.SUBMITTED || previewUploaded) {
            item.setPartUploaded();
        } else {
            item.setPartPrepared(offline);
        }

        return item;
    }

    private void setPartsCleared(PeerToPeerInstance instance) {
        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            SubmittingItem item = partsListView.findViewWithTag(mediaFile.getVaultFile().id);

            if (instance.getStatus() == EntityStatus.SUBMITTED) {
                item.setPartUploaded();
            } else {
                item.setPartCleared();
            }
        }
    }
}


