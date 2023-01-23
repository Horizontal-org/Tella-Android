package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.utils.MediaFile;

import org.hzontal.shared_ui.submission.SubmittingItem;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.EntityStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.FileUtil;
import rs.readahead.washington.mobile.util.Util;

@SuppressLint("ViewConstructor")
public class ReportsFormEndView extends FrameLayout {
    private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;
    LinearLayout partsListView;
    TextView titleView;
    TextView descriptionView;
    TextView formStatusTextView;
    TextView formSizeView;
    String title;
    ProgressBar totalProgress;
    long formSize = 0L;
    Float uploadedFormPct = 0F;
    HashMap<String, Long> formParts = new HashMap();
    HashMap<String, Float> uploadedPartsPct = new HashMap();
    private ReportFormInstance instance;
    private boolean previewUploaded;


    public ReportsFormEndView(Context context, @StringRes int titleResId) {
        super(context);
        inflate(context, R.layout.reports_form_end_view, this);

        title = getResources().getString(titleResId);

        titleView = findViewById(R.id.title);

        titleView.setText(title);

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);
    }

    public ReportsFormEndView(Context context, String title, String description, String status) {
        super(context);
        inflate(context, R.layout.reports_form_end_view, this);

        this.title = title;

        titleView = findViewById(R.id.title);
        titleView.setText(title);

        descriptionView = findViewById(R.id.form_description);
        descriptionView.setText(description);

        totalProgress = findViewById(R.id.totalProgress);
        formSizeView = findViewById(R.id.formSize);

        formStatusTextView = findViewById(R.id.form_status);

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
        VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
        glide = Glide.with(getContext()).using(glideLoader);
    }

    public void setInstance(@NonNull ReportFormInstance instance, boolean offline, boolean previewUploaded) {
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

        formSize = instance.getTitle().getBytes(StandardCharsets.UTF_8).length +
                instance.getMetadata().toString().getBytes(StandardCharsets.UTF_8).length;
        //  + instance.getType().getBytes(StandardCharsets.UTF_8).length;

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        //  partsListView.addView(createFormSubmissionPartItemView(instance, formSize, offline));

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.size;
            formParts.put(mediaFile.getPartName(), mediaFile.size);
            uploadedPartsPct.put(mediaFile.getPartName(), 0F);
        }
        setFormSizeLabel(instance, formSize);
        uploadProgressVisibity(instance.getStatus());
    }

    private void setFormSizeLabel(@NonNull ReportFormInstance instance, long formSize) {
        String title;
        if (instance.getStatus() == EntityStatus.SUBMITTED) {
            title = getResources().getString(R.string.File_Uploaded_on) + " " + Util.getDateTimeString(instance.getUpdated()) + "\n" + getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ", " + FileUtil.getFileSizeString(formSize);
            formSizeView.setText(title);
        } else {
            //TODO (AHLEM) IN CASE OF SUBMITTING THE FILE
        }
    }

    private void uploadProgressVisibity(EntityStatus status) {
        if (status == EntityStatus.SUBMITTED) {
            totalProgress.setVisibility(GONE);
        } else {
            totalProgress.setVisibility(VISIBLE);
        }
    }

    public void showUploadProgress(String partName) {
        titleView.setText(R.string.collect_end_heading_submitting);
        //  subTitleView.setVisibility(GONE);

        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setPartUploading();
        }
    }

    public void hideUploadProgress(String partName) {
        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setPartUploaded();
        }
    }

    public void setUploadProgress(ReportFormInstance instance, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        float percentComplete = ((pct + getTotalUploadedSize(instance) * 100) / formSize);
        totalProgress.setProgress(Math.round(percentComplete));
        formSizeView.setText(FileUtil.getUploadedFileSize(percentComplete, formSize));
    }

    private int getTotalUploadedSize(ReportFormInstance instance) {
        int totalUploadedSize = 0;
        for (FormMediaFile formMediaFile : instance.getWidgetMediaFiles()) {
            if (formMediaFile.status == FormMediaFileStatus.SUBMITTED) {
                totalUploadedSize += formMediaFile.size;
            }
        }
        return totalUploadedSize;
    }

    public void clearPartsProgress(ReportFormInstance instance) {
        setPartsCleared(instance);
    }

    private View createFormSubmissionPartItemView(@NonNull ReportFormInstance instance, long size, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);

        item.setTag("REPORT_RESPONSE");

        item.setPartName(R.string.collect_end_item_form_data);
        item.setPartSize(size);
        item.setPartIcon(R.drawable.ic_assignment_white_24dp);

        if (instance.getStatus() == EntityStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == EntityStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setPartPrepared(offline);
        }

        if (offline || instance.getStatus() == EntityStatus.SUBMITTED) {
            totalProgress.setVisibility(View.GONE);
            //   subTitleView.setVisibility(GONE);
        } else {
            // subTitleView.setVisibility(VISIBLE);
            totalProgress.setVisibility(View.VISIBLE);
        }

        return item;
    }

    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        ImageView thumbView = item.findViewById(R.id.fileThumb);
        item.setTag(mediaFile.getPartName());

        item.setPartName(mediaFile.name);
        item.setPartSize(mediaFile.size);

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType))) {
            glide.load(new VaultFileLoaderModel(mediaFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
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

    private void setPartsCleared(ReportFormInstance instance) {

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            SubmittingItem item = partsListView.findViewWithTag(mediaFile.getVaultFile().id);

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                item.setPartUploaded();
            } else {
                item.setPartCleared();
            }
        }
    }
}

