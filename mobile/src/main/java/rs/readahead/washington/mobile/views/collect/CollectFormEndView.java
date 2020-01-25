package rs.readahead.washington.mobile.views.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFile;
import rs.readahead.washington.mobile.domain.entity.collect.FormMediaFileStatus;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.FileUtil;


@SuppressLint("ViewConstructor")
public class CollectFormEndView extends FrameLayout {
    LinearLayout partsListView;
    TextView titleView;
    TextView subTitleView;
    String title;

    private CollectFormInstance instance;

    public CollectFormEndView(Context context, @StringRes int titleResId) {
        super(context);
        inflate(context, R.layout.collect_form_end_view, this);

        title = getResources().getString(titleResId);

        titleView = findViewById(R.id.title);

        subTitleView = findViewById(R.id.subtitle);
    }

    public void setInstance(@NonNull CollectFormInstance instance, boolean offline) {
        this.instance = instance;
        refreshInstance(offline);
    }

    public void refreshInstance(boolean offline) {
        if (this.instance == null) {
            return;
        }

        titleView.setText(title);

        TextView formNameView = findViewById(R.id.formName);
        formNameView.setText(instance.getFormName());

        int formElements = 1;
        long formSize = FormUtils.getFormPayloadSize(instance);

        partsListView = findViewById(R.id.formPartsList);
        partsListView.removeAllViews();

        partsListView.addView(createFormSubmissionPartItemView(instance, formSize, offline));
        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            partsListView.addView(createFormMediaFileItemView(mediaFile, offline));
            formSize += mediaFile.getSize();
            formElements++;
        }

        TextView formElementsView = findViewById(R.id.formElements);
        TextView formSizeView = findViewById(R.id.formSize);

        formElementsView.setText(getResources().getQuantityString(R.plurals.ra_form_elements_count, formElements, formElements));
        formSizeView.setText(FileUtil.getFileSizeString(formSize));
    }

    public void showUploadProgress(String partName) {
        titleView.setText(R.string.ra_submitting);
        subTitleView.setVisibility(GONE);

        ViewGroup layout = findViewWithTag(partName);
        if (layout != null) {
            setPartUploading(layout);
        }
    }

    public void hideUploadProgress(String partName) {
        ViewGroup layout = findViewWithTag(partName);
        if (layout != null) {
            setPartUploaded(layout);
        }
    }

    public void setUploadProgress(String partName, float pct) {
        if (pct < 0 || pct > 1) {
            return;
        }

        ProgressBar bar = findProgressBar(partName);
        if (bar != null) {
            bar.setProgress((int) (bar.getMax() * pct));
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

        nameView.setText(R.string.ra_form_data);
        sizeView.setText(FileUtil.getFileSizeString(size));
        iconView.setImageResource(R.drawable.ic_assignment_black_24dp);

        if (instance.getFormPartStatus() == FormMediaFileStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
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
        CheckBox uploadCheck = layout.findViewById(R.id.partCheckBox);

        nameView.setText(mediaFile.getFileName());
        sizeView.setText(FileUtil.getFileSizeString(mediaFile.getSize()));

        int typeResId = R.drawable.ic_attach_file_black_24dp;

        switch (mediaFile.getType()) {
            case IMAGE:
                typeResId = R.drawable.ic_menu_camera;
                break;

            case VIDEO:
                typeResId = R.drawable.ic_videocam_black_24dp;
                break;

            case AUDIO:
                typeResId = R.drawable.ic_mic_black_24dp;
                break;

            case UNKNOWN:
            default:
                break;
        }

        iconView.setImageResource(typeResId);

        if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
            setPartUploaded(layout);
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

    private void setPartsCleared(CollectFormInstance instance) {
        ViewGroup layout = partsListView.findViewWithTag(C.OPEN_ROSA_XML_PART_NAME);

        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            setPartUploaded(layout);
        } else {
            setPartCleared(layout);
        }

        for (FormMediaFile mediaFile : instance.getWidgetMediaFiles()) {
            layout = partsListView.findViewWithTag(mediaFile.getPartName());

            if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
                setPartUploaded(layout);
            } else {
                setPartCleared(layout);
            }
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
}
