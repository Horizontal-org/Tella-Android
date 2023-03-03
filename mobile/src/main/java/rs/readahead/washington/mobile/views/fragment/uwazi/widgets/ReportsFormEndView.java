package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.utils.MediaFile;

import org.hzontal.shared_ui.submission.SubmittingItem;

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
import timber.log.Timber;

@SuppressLint("ViewConstructor")
public class ReportsFormEndView extends FrameLayout {
    //private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;
    LinearLayout partsListView;
    TextView titleView;
    TextView descriptionView;
    TextView formStatusTextView;
    TextView formSizeView;
    String title;
    ProgressBar totalProgress;
    long formSize = 0L;
    private ReportFormInstance instance;
    private boolean previewUploaded;


    public ReportsFormEndView(Context context, String title, String description) {
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
       //VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
       // glide = Glide.with(getContext()).using(glideLoader);
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

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.size;
        }
        setFormSizeLabel(instance, 0);
        uploadProgressVisibity(instance.getStatus(), offline);
    }

    void setFormSizeLabel(@NonNull ReportFormInstance instance, int percent) {
        String title;
        if (instance.getWidgetMediaFiles().size() == 0) {
            title = getStatusLabel(instance.getStatus());
            formSizeView.setText(title);
            return;
        }

        if (instance.getStatus() == EntityStatus.SUBMITTED) {
            title = getStatusLabel(instance.getStatus()) + " " + Util.getDateTimeString(instance.getUpdated()) + "\n" + getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + ", " + FileUtil.getFileSizeString(formSize);
        } else if (instance.getStatus() == EntityStatus.PAUSED) {
            title = getStatusLabel(instance.getStatus()) + "\n" + getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + "," + getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize);
        } else if (instance.getStatus() == EntityStatus.FINALIZED || instance.getStatus() == EntityStatus.SUBMISSION_PENDING || instance.getStatus() == EntityStatus.SUBMISSION_ERROR) {
            title = getStatusLabel(instance.getStatus()) + "\n" + " " + getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + "," + getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize) + " " + getResources().getString(R.string.File_Uploaded);
        } else {
            //TODO CHECK THE PERCENT  (getPercentUploadedSize(instance)*100) + "% " +
            title = (percent) + "% " + getResources().getString(R.string.File_Uploaded) + "\n" + getResources().getQuantityString(R.plurals.upload_main_meta_number_of_files, instance.getWidgetMediaFiles().size(), instance.getWidgetMediaFiles().size()) + "," + getTotalUploadedSize(instance) + "/" + FileUtil.getFileSizeString(formSize) + " " + getResources().getString(R.string.File_Uploaded);
        }

        formSizeView.setText(title);
    }

    String getStatusLabel(EntityStatus status) {
        String title = "";
        if (status == EntityStatus.SUBMITTED) {
            title = getResources().getString(R.string.File_Uploaded_on);
        } else if (status == EntityStatus.PAUSED) {
            title = getResources().getString(R.string.Paused_Report);
        } else if (status == EntityStatus.FINALIZED || instance.getStatus() == EntityStatus.SUBMISSION_PENDING || instance.getStatus() == EntityStatus.SUBMISSION_ERROR) {
            title = getResources().getString(R.string.Report_Waiting_For_Connection);
        }
        return title;
    }

    private void uploadProgressVisibity(EntityStatus status, Boolean isOnline) {
        if (!isOnline) {
            totalProgress.setVisibility(GONE);
            return;
        }

        if (status == EntityStatus.SUBMITTED) {
            totalProgress.setVisibility(GONE);
        } else {
            totalProgress.setVisibility(VISIBLE);
        }
    }

    public void setUploadProgress(ReportFormInstance instance, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        int percentComplete;

        if (instance.getWidgetMediaFiles().size() > 1) {
            percentComplete = getTotalUploadedSizePercent(instance);
        } else {
            percentComplete = (int) (pct * 100);
        }

        Timber.d("***Test*** PCT " + pct + "\n getTotalUploadedSize " + getTotalUploadedSize(instance) + "\n FormSize " + formSize + "\n percentComplete " + percentComplete + " \n Math.round(percentComplete) " + Math.toIntExact(percentComplete));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            totalProgress.setProgress(percentComplete, true);
        } else {
            totalProgress.setProgress(percentComplete);
        }
        setFormSizeLabel(instance, percentComplete);
    }

    private int getTotalUploadedSizePercent(ReportFormInstance instance) {
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

    private String getTotalUploadedSize(ReportFormInstance instance) {
        long totalUploadedSize = 0;
        for (FormMediaFile formMediaFile : instance.getWidgetMediaFiles()) {
            totalUploadedSize += formMediaFile.uploadedSize;
        }
        return FileUtil.getFileSize(totalUploadedSize);
    }

    public void clearPartsProgress(ReportFormInstance instance) {
        setPartsCleared(instance);
    }


    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        ImageView thumbView = item.findViewById(R.id.fileThumb);
        item.setTag(mediaFile.getPartName());

        item.setPartName(mediaFile.name);
        item.setPartSize(mediaFile.size);

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType))) {
            /*glide.load(new VaultFileLoaderModel(mediaFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(thumbView);*/
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

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED && instance.getStatus() == EntityStatus.SUBMITTED) {
                item.setPartUploaded();
            } else {
                item.setPartCleared();
            }
        }
    }
}

