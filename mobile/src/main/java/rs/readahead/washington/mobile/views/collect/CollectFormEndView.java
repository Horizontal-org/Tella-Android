package rs.readahead.washington.mobile.views.collect;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.hzontal.utils.MediaFile;

import org.hzontal.shared_ui.submission.SubmittingItem;

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
            formSize += mediaFile.size;
            formElements++;
        }

        TextView formElementsView = findViewById(R.id.formElements);
        TextView formSizeView = findViewById(R.id.formSize);

        formElementsView.setText(getResources().getQuantityString(R.plurals.collect_end_meta_number_of_elements, formElements, formElements));
        formSizeView.setText(FileUtil.getFileSizeString(formSize));
    }

    public void showUploadProgress(String partName) {
        titleView.setText(R.string.collect_end_heading_submitting);
        subTitleView.setVisibility(GONE);

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

    public void clearPartsProgress(CollectFormInstance instance) {
        setPartsCleared(instance);
    }

    private View createFormSubmissionPartItemView(@NonNull CollectFormInstance instance, long size, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);

        item.setTag(C.OPEN_ROSA_XML_PART_NAME);

        item.setPartName(R.string.collect_end_item_form_data);
        item.setPartSize(FileUtil.getFileSizeString(size));
        item.setPartIcon(R.drawable.ic_assignment_white_24dp);

        if (instance.getFormPartStatus() == FormMediaFileStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMITTED || // back compatibility down
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
            item.setPartUploaded();
        } else {
            item.setChecked(true);
            item.setCheckEnabled(false);
            item.setPartPrepared(offline);
        }

        if (offline || instance.getStatus() == CollectFormInstanceStatus.SUBMITTED) {
            subTitleView.setVisibility(GONE);
        } else {
            subTitleView.setVisibility(VISIBLE);
        }

        return item;
    }

    private View createFormMediaFileItemView(@NonNull FormMediaFile mediaFile, boolean offline) {

        SubmittingItem item = new SubmittingItem(getContext(), null, 0);
        item.setTag(mediaFile.getPartName());

        item.setPartName(mediaFile.name);
        item.setPartSize(FileUtil.getFileSizeString(mediaFile.size));

        int typeResId = R.drawable.ic_attach_file_white_24dp;

        if (MediaFile.INSTANCE.isImageFileType(mediaFile.mimeType)) {
            typeResId = R.drawable.ic_menu_camera;
        } else if (MediaFile.INSTANCE.isVideoFileType(mediaFile.mimeType)) {
            typeResId = R.drawable.ic_videocam;
        } else if (MediaFile.INSTANCE.isAudioFileType(mediaFile.mimeType)) {
            typeResId = R.drawable.ic_mic_white_small;
        }

        item.setPartIcon(typeResId);

        if (mediaFile.status == FormMediaFileStatus.SUBMITTED) {
            item.setPartUploaded();
        } else {
            item.setChecked(mediaFile.uploading);
            item.setCheckEnabled(true);
            item.partCheckBox.setOnCheckedChangeListener((buttonView, isChecked) ->
                    mediaFile.uploading = isChecked);
            item.setPartPrepared(offline);
        }

        return item;
    }

    private void setPartsCleared(CollectFormInstance instance) {
        SubmittingItem item = partsListView.findViewWithTag(C.OPEN_ROSA_XML_PART_NAME);

        if (instance.getStatus() == CollectFormInstanceStatus.SUBMITTED ||
                instance.getStatus() == CollectFormInstanceStatus.SUBMISSION_PARTIAL_PARTS) {
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
