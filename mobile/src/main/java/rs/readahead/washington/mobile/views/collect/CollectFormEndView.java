package rs.readahead.washington.mobile.views.collect;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.cardview.widget.CardView;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.hzontal.utils.MediaFile;

import java.util.HashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.media.MediaFileHandler;
import rs.readahead.washington.mobile.media.VaultFileUrlLoader;
import rs.readahead.washington.mobile.presentation.entity.VaultFileLoaderModel;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;


@SuppressLint("ViewConstructor")
public class CollectFormEndView extends FrameLayout {
    LinearLayout partsListView;
    TextView titleView;
    TextView subTitleView;
    String title;
    TextView formSizeView;

    HashMap<String, Long> fileSizes = new HashMap<>();
    Long formSize;
    Long submittedSize = 0L;

    private CollectFormInstance instance;
   // private final RequestManager.ImageModelRequest<VaultFileLoaderModel> glide;

    public CollectFormEndView(Context context, @StringRes int titleResId) {
        super(context);
        inflate(context, R.layout.collect_form_end_view, this);
        title = getResources().getString(titleResId);

        titleView = findViewById(R.id.title);

        subTitleView = findViewById(R.id.subtitle);

        MediaFileHandler mediaFileHandler = new MediaFileHandler();
       // VaultFileUrlLoader glideLoader = new VaultFileUrlLoader(getContext().getApplicationContext(), mediaFileHandler);
      //  glide = Glide.with(getContext()).using(glideLoader);
    }

    public void setInstance(@NonNull CollectFormInstance instance, boolean offline) {
        this.instance = instance;
        refreshInstance(offline);
    }

    @SuppressLint("SetTextI18n")
    public void refreshInstance(boolean offline) {
        submittedSize = 0L;
        if (this.instance == null) {
            return;
        }

        titleView.setText(title);

        TextView formNameView = findViewById(R.id.formName);
        formNameView.setText(instance.getFormName());

        //int formElements = 1;
        formSize = FormUtils.getFormPayloadSize(instance);
        fileSizes.put(C.OPEN_ROSA_XML_PART_NAME, FormUtils.getFormPayloadSize(instance));

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        partsListView.addView(createFormSubmissionPartItemView(instance, formSize, offline));
        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            fileSizes.put(mediaFile.getPartName(), mediaFile.size);
            formSize += mediaFile.size;
            //formElements++;
        }

        //TextView formElementsView = findViewById(R.id.formElements);
        formSizeView = findViewById(R.id.formSize);

        //formElementsView.setText(getResources().getQuantityString(R.plurals.collect_end_meta_number_of_elements, formElements, formElements));
        if (submittedSize == 0L) {
            formSizeView.setText(FileUtil.getFileSizeString(formSize));
        } else {
            formSizeView.setText(FileUtil.getFileSizeString(submittedSize) + " / " + FileUtil.getFileSizeString(formSize));
        }
    }

    public void showUploadProgress(String partName) {
        titleView.setText(R.string.collect_end_heading_submitting);
        titleView.setVisibility(GONE);
        subTitleView.setVisibility(GONE);

        ViewGroup layout = findViewWithTag(partName);
        if (layout != null) {
            setPartUploading(layout);
        }
    }

    @SuppressLint("SetTextI18n")
    public void hideUploadProgress(String partName) {
        ViewGroup layout = findViewWithTag(partName);
        if (layout != null) {
            setPartUploaded(layout);
            TextView sizeView = layout.findViewById(R.id.partSize);
            if (fileSizes.get(partName) != null) {
                sizeView.setText(FileUtil.getFileSizeString(fileSizes.get(partName)));
            }
        }

        submittedSize += fileSizes.get(partName);
        formSizeView.setText(FileUtil.getFileSizeString(submittedSize) + " / " + FileUtil.getFileSizeString(formSize));
    }

    @SuppressLint("SetTextI18n")
    public void setUploadProgress(String partName, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        ProgressBar bar = findProgressBar(partName);
        if (bar != null) {
            bar.setProgress((int) (bar.getMax() * pct));
        }

        TextView partSize = findPartSize(partName);
        if (partSize != null) {
            partSize.setText(getUploadedFileSize(pct, fileSizes.get(partName)));
        }
    }

    public void clearPartsProgress(CollectFormInstance instance) {
        setPartsCleared(instance);
    }

    private View createFormSubmissionPartItemView(@NonNull CollectFormInstance instance, long size, boolean offline) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.form_parts_list_item, null);

        layout.setTag(C.OPEN_ROSA_XML_PART_NAME);

        TextView nameView = layout.findViewById(R.id.partName);
        TextView sizeView = layout.findViewById(R.id.partSize);
        ImageView iconView = layout.findViewById(R.id.partIcon);
        CheckBox uploadCheck = layout.findViewById(R.id.partCheckBox);
        CardView cardView = layout.findViewById(R.id.fileThumbCard);

        nameView.setText(R.string.collect_end_item_form_data);
        sizeView.setText(FileUtil.getFileSizeString(size));
        iconView.setImageResource(R.drawable.ic_assignment_white_24dp);
        cardView.setVisibility(GONE);

        if (instance.getFormPartStatus() == FormMediaFileStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            submittedSize += FormUtils.getFormPayloadSize(instance);
            setPartUploaded(layout);
        } else {
            uploadCheck.setChecked(true);
            uploadCheck.setEnabled(false);
            setPartPrepared(layout, offline);
        }

        if (offline || instance.getStatus() == CollectFormInstanceStatus.SUBMITTED) {
            subTitleView.setVisibility(GONE);
        } else {
            subTitleView.setVisibility(VISIBLE);
            titleView.setVisibility(VISIBLE);
        }
        return layout;
    }

    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {
        @SuppressLint("InflateParams")
        LinearLayout layout = (LinearLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.form_parts_list_item, null);

        layout.setTag(mediaFile.getPartName());

        TextView nameView = layout.findViewById(R.id.partName);
        TextView sizeView = layout.findViewById(R.id.partSize);
        ImageView iconView = layout.findViewById(R.id.partIcon);
        ImageView thumbView = layout.findViewById(R.id.fileThumb);
        CheckBox uploadCheck = layout.findViewById(R.id.partCheckBox);
        CardView cardView = layout.findViewById(R.id.fileThumbCard);
        ProgressBar uploadProgress = layout.findViewById(R.id.uploadProgress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            uploadProgress.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
        }

        nameView.setText(mediaFile.name);
        sizeView.setText(FileUtil.getFileSizeString(mediaFile.size));

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType) || (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType))) {
            /*glide.load(new VaultFileLoaderModel(mediaFile, VaultFileLoaderModel.LoadType.THUMBNAIL))
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(thumbView);*/
            iconView.setVisibility(GONE);
        } else if (MediaFile.INSTANCE.isAudioFileType(mediaFile.mimeType)) {
            iconView.setImageResource(R.drawable.ic_headset_white_24dp);
            cardView.setVisibility(GONE);
        } else {
            iconView.setImageResource(R.drawable.ic_attach_file_white_24dp);
            cardView.setVisibility(GONE);
        }

        if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
            setPartUploaded(layout);
            submittedSize += mediaFile.size;
        } else {
            uploadCheck.setChecked(mediaFile.uploading);
            uploadCheck.setEnabled(true);
            uploadCheck.setOnCheckedChangeListener((buttonView, isChecked) ->
                    mediaFile.uploading = isChecked);
            setPartPrepared(layout, offline);
        }

        return layout;
    }

    @Nullable
    private ProgressBar findProgressBar(@NonNull String partName) {
        View layout = findViewWithTag(partName);

        if (layout == null) {
            return null;
        }

        return layout.findViewById(R.id.uploadProgress);
    }

    @Nullable
    private TextView findPartSize(@NonNull String partName) {
        View layout = findViewWithTag(partName);

        if (layout == null) {
            return null;
        }

        return layout.findViewById(R.id.partSize);
    }

    @SuppressLint("SetTextI18n")
    private void setPartsCleared(CollectFormInstance instance) {
        submittedSize = 0L;
        ViewGroup layout = partsListView.findViewWithTag(C.OPEN_ROSA_XML_PART_NAME);

        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            submittedSize += fileSizes.get(C.OPEN_ROSA_XML_PART_NAME);
            setPartUploaded(layout);
        } else {
            setPartCleared(layout);
        }

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            layout = partsListView.findViewWithTag(mediaFile.getPartName());

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                submittedSize += fileSizes.get(mediaFile.getPartName());
                setPartUploaded(layout);
            } else {
                setPartCleared(layout);
            }
        }
        if (submittedSize == 0L) {
            formSizeView.setText(FileUtil.getFileSizeString(formSize));
        } else {
            formSizeView.setText(FileUtil.getFileSizeString(submittedSize) + " / " + FileUtil.getFileSizeString(formSize));
        }
    }

    private void setPartCleared(@NonNull ViewGroup layout) {
        layout.findViewById(R.id.uploadProgress).setVisibility(GONE);
        layout.findViewById(R.id.partCheckBox).setVisibility(GONE);
        layout.findViewById(R.id.partCheckIcon).setVisibility(GONE);
    }

    private void setPartPrepared(@NonNull ViewGroup layout, boolean offline) {
        layout.findViewById(R.id.uploadProgress).setVisibility(GONE);
        layout.findViewById(R.id.partCheckBox).setVisibility(offline ? GONE : VISIBLE);
        layout.findViewById(R.id.partCheckIcon).setVisibility(GONE);
    }

    private void setPartUploading(@NonNull ViewGroup layout) {
        layout.findViewById(R.id.uploadProgress).setVisibility(VISIBLE);
        layout.findViewById(R.id.partCheckBox).setVisibility(GONE);
        layout.findViewById(R.id.partCheckIcon).setVisibility(GONE);
    }

    private void setPartUploaded(@NonNull ViewGroup layout) {
        layout.findViewById(R.id.uploadProgress).setVisibility(GONE);
        layout.findViewById(R.id.partCheckBox).setVisibility(GONE);
        layout.findViewById(R.id.partCheckIcon).setVisibility(VISIBLE);
    }

    private String getUploadedFileSize(float pct, Long size) {
        float uploadedSize = (pct * size);
        return FileUtil.getFileSizeString((long) uploadedSize) + " / " + FileUtil.getFileSizeString(size);
    }

}
