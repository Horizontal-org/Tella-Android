package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.utils.MediaFile;

import org.hzontal.shared_ui.submission.SubmittingItem;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.EntityStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.domain.entity.reports.ReportFormInstance;
import rs.readahead.washington.mobile.domain.entity.uwazi.UwaziEntityInstance;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.FileUtil;

@SuppressLint("ViewConstructor")
public class ReportsFormEndView extends FrameLayout {
    private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;
    LinearLayout partsListView;
    TextView titleView;
    TextView descriptionView;
    TextView formStatusTextView;
    String title;
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

        long formSize = instance.getTitle().getBytes(StandardCharsets.UTF_8).length +
                instance.getMetadata().toString().getBytes(StandardCharsets.UTF_8).length;
       //  + instance.getType().getBytes(StandardCharsets.UTF_8).length;

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        partsListView.addView(createFormSubmissionPartItemView(instance, formSize, offline));
        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.size;
        }

        // TextView formElementsView = findViewById(R.id.formElements);
        TextView formSizeView = findViewById(R.id.formSize);

        //    formElementsView.setText(getResources().getQuantityString(R.plurals.collect_end_meta_number_of_elements, formElements, formElements));
        formSizeView.setText(FileUtil.getFileSizeString(formSize));
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

    public void setUploadProgress(String partName, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        SubmittingItem item = findViewWithTag(partName);
        if (item != null) {
            item.setUploadProgress(pct);
        }
    }

    public void clearPartsProgress(UwaziEntityInstance instance) {
        setPartsCleared(instance);
    }

    private View createFormSubmissionPartItemView(@NonNull ReportFormInstance instance, long size, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);

        item.setPartName(R.string.collect_end_item_form_data);
        item.setPartSize(size);
        item.setPartIcon(R.drawable.ic_assignment_white_24dp);

        if (instance.getStatus() == EntityStatus.SUBMITTED ||
                instance.getStatus() == EntityStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == EntityStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setPartPrepared(offline);
        }

        if (offline || instance.getStatus() == EntityStatus.SUBMITTED) {
            //   subTitleView.setVisibility(GONE);
        } else {
            // subTitleView.setVisibility(VISIBLE);
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

    private void setPartsCleared(UwaziEntityInstance instance) {
        SubmittingItem item = partsListView.findViewWithTag("UWAZI_RESPONSE");

        if (instance.getStatus() == EntityStatus.SUBMITTED ||
                instance.getStatus() == EntityStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setPartCleared();
        }

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            item = partsListView.findViewWithTag(mediaFile.getPartName());

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                item.setPartUploaded();
            } else {
                item.setPartCleared();
            }
        }
    }
}

