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

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.peertopeer.model.P2PFileStatus;
import org.horizontal.tella.mobile.data.peertopeer.model.ProgressFile;
import org.horizontal.tella.mobile.data.peertopeer.model.SessionStatus;
import org.horizontal.tella.mobile.util.FileUtil;
import org.hzontal.shared_ui.submission.SubmittingItem;

import java.util.List;

public class PeerToPeerEndView extends FrameLayout {
    private final LinearProgressIndicator totalProgress;
    private final TextView titleView;
    private final TextView formSizeView;
    private LinearLayout partsListView;

    private final String title;
    private boolean previewUploaded;

    public PeerToPeerEndView(Context context, String title) {
        super(context);
        inflate(context, R.layout.reports_form_end_view, this);

        this.title = title;

        titleView = findViewById(R.id.title);
        titleView.setText(title);

        totalProgress = findViewById(R.id.totalProgress);
        formSizeView = findViewById(R.id.formSize);
    }

    public void setFiles(List<ProgressFile> progressFiles, boolean offline, boolean previewUploaded) {
        this.previewUploaded = previewUploaded;

        titleView.setText(title);

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        for (ProgressFile file : progressFiles) {
            partsListView.addView(createProgressFileItemView(file, offline));
        }

        uploadProgressVisibility(progressFiles, true);
        setUploadProgress(progressFiles, 0f);
    }

    public void setUploadProgress(List<ProgressFile> progressFiles, float pct) {
        if (pct < 0 || pct > 1) return;

        int percentComplete = getTotalUploadedSizePercent(progressFiles);

        for (ProgressFile file : progressFiles) {
            String tagId = file.getVaultFile() != null && file.getVaultFile().id != null
                    ? file.getVaultFile().id
                    : file.getFile().getId();

            if (isUploadedStatus(file.getStatus())) {
                SubmittingItem item = partsListView.findViewWithTag(tagId);
                if (item != null) item.setPartUploaded();
            }
        }

        totalProgress.setProgressCompat(percentComplete, true);
        setFormSizeLabel(progressFiles, percentComplete);
    }

    public void clearPartsProgress(List<ProgressFile> progressFiles, SessionStatus sessionStatus) {
        for (ProgressFile file : progressFiles) {
            String tagId = file.getVaultFile() != null && file.getVaultFile().id != null
                    ? file.getVaultFile().id
                    : file.getFile().getId();

            SubmittingItem item = partsListView.findViewWithTag(tagId);
            if (item != null) {
                if (sessionStatus == SessionStatus.FINISHED || isUploadedStatus(file.getStatus())) {
                    item.setPartUploaded();
                } else {
                    item.setPartCleared();
                }
            }
        }
    }

    private int getTotalUploadedSizePercent(List<ProgressFile> progressFiles) {
        long totalUploadedSize = 0;
        long totalSize = 0;

        for (ProgressFile file : progressFiles) {
            totalUploadedSize += file.getBytesTransferred();

            long size = file.getVaultFile() != null && file.getVaultFile().size > 0
                    ? file.getVaultFile().size
                    : file.getFile().getSize();

            totalSize += size;
        }

        return totalSize > 0 ? Math.round((totalUploadedSize * 1f / totalSize) * 100) : 0;
    }

    private void setFormSizeLabel(List<ProgressFile> files, int percent) {
        int count = files.size();
        long totalSize = 0;
        long totalUploaded = 0;

        for (ProgressFile file : files) {
            long size = file.getVaultFile() != null && file.getVaultFile().size > 0
                    ? file.getVaultFile().size
                    : file.getFile().getSize();

            totalSize += size;
            totalUploaded += file.getBytesTransferred();
        }

        String label = percent + "% " + getContext().getString(R.string.File_Uploaded) + "\n" +
                getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, count, count) + ", " +
                FileUtil.getFileSize(totalUploaded) + "/" + FileUtil.getFileSize(totalSize);

        formSizeView.setText(label);
    }

    private void uploadProgressVisibility(List<ProgressFile> files, boolean isOnline) {
        if (!isOnline || files.isEmpty()) {
            totalProgress.setVisibility(GONE);
        } else {
            totalProgress.setVisibility(VISIBLE);
        }
    }

    private View createProgressFileItemView(@NonNull ProgressFile file, boolean offline) {
        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        ImageView thumbView = item.findViewById(R.id.fileThumb);

        String tagId = file.getVaultFile() != null && file.getVaultFile().id != null
                ? file.getVaultFile().id
                : file.getFile().getId();
        item.setTag(tagId);

        String name = file.getVaultFile() != null && file.getVaultFile().name != null
                ? file.getVaultFile().name
                : file.getFile().getFileName();
        item.setPartName(name);

        long size = file.getVaultFile() != null && file.getVaultFile().size > 0
                ? file.getVaultFile().size
                : file.getFile().getSize();
        item.setPartSize(size);

        byte[] thumb = file.getVaultFile() != null && file.getVaultFile().thumb != null
                ? file.getVaultFile().thumb
                : file.getFile().getThumbnail();

        if (thumb != null) {
            Glide.with(getContext())
                    .load(thumb)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(thumbView);
        }

        if (isUploadedStatus(file.getStatus())) {
            item.setPartUploaded();
        } else {
            if (previewUploaded) {
                item.setPartUploaded();
            } else {
                item.setPartPrepared(offline);
            }
        }

        return item;
    }

    private boolean isUploadedStatus(P2PFileStatus status) {
        return status == P2PFileStatus.FINISHED;
    }
}
