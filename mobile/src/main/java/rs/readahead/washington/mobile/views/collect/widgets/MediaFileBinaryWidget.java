package rs.readahead.washington.mobile.views.collect.widgets;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.hzontal.shared_ui.bottomsheet.CustomBottomSheetFragment;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.MediaFileBinaryWidgetCleared;

public abstract class MediaFileBinaryWidget extends QuestionWidget {
    private String filename;

    public interface MediaFileBinaryWidgetAction {
        void run();
    }

    public MediaFileBinaryWidget(Context context, @NonNull FormEntryPrompt formEntryPrompt) {
        super(context, formEntryPrompt);
    }

    @Override
    public void clearAnswer() {
        MyApplication.bus().post(new MediaFileBinaryWidgetCleared(formEntryPrompt.getIndex(), filename));
    }

    @Override
    public IAnswerData getAnswer() {
        return TextUtils.isEmpty(getFilename()) ? null : new StringData(getFilename());
    }

    protected String getFilename() {
        return filename;
    }

    protected void setFilename(String filename) {
        this.filename = filename;
    }

    protected void showDeleteBottomSheet(@NonNull final MediaFileBinaryWidgetAction takeFrom,
                                         @NonNull final MediaFileBinaryWidgetAction chooseFromVault,
                                         @Nullable final MediaFileBinaryWidgetAction chooseFromDevice) {
        final FragmentActivity activity = (FragmentActivity) getContext(); // todo: watch for this with CollectFormEntryActivity (Fragment?)

        final CustomBottomSheetFragment bottomSheet = CustomBottomSheetFragment.Companion.with(activity.getSupportFragmentManager())
                .page(R.layout.collect_media_widget_btm_sheet)
                .cancellable(true);

        bottomSheet.holder(new FormAttachmentsHolder(), holder -> {
            holder.takeFrom.setVisibility(VISIBLE);
            holder.recordAudio.setVisibility(INVISIBLE);

            if (this instanceof ImageWidget) {
                holder.title.setText("Upload photo"); // todo: resources for all these
                holder.description.setText("Choose how you want to upload your photo");
            } else if (this instanceof VideoWidget) {
                holder.title.setText("Upload video");
                holder.description.setText("Choose how you want to upload your video");
            } else if (this instanceof AudioWidget) {
                holder.title.setText("Upload audio recording");
                holder.description.setText("Choose how you want to upload your audio recording");
                holder.takeFrom.setVisibility(INVISIBLE);
                holder.recordAudio.setVisibility(VISIBLE);
                holder.chooseFromDevice.setVisibility(GONE);

            } else {
                throw new RuntimeException("Unknown MediaFileBinaryWidget subclass");
            }

            holder.takeFrom.setOnClickListener(v -> {
                takeFrom.run();
                bottomSheet.dismiss();
            });

            holder.recordAudio.setOnClickListener(v -> {
                takeFrom.run();
                bottomSheet.dismiss();
            });

            holder.chooseFromVault.setOnClickListener(v -> {
                chooseFromVault.run();
                bottomSheet.dismiss();
            });

            if (chooseFromDevice != null) {
                holder.chooseFromDevice.setOnClickListener(v -> {
                    chooseFromDevice.run();
                    bottomSheet.dismiss();
                });
            }
        }).transparentBackground().launch();
    }

    protected static class FormAttachmentsHolder extends CustomBottomSheetFragment.PageHolder {
        public TextView title;
        public TextView description;
        public TextView takeFrom;
        public TextView recordAudio;
        public TextView chooseFromVault;
        public TextView chooseFromDevice;

        @Override
        public void bindView(@NonNull View view) {
            title = view.findViewById(R.id.title);
            description = view.findViewById(R.id.description);
            takeFrom = view.findViewById(R.id.take_from);
            recordAudio = view.findViewById(R.id.record_audio);
            chooseFromVault = view.findViewById(R.id.choose_from_vault);
            chooseFromDevice = view.findViewById(R.id.choose_from_device);
        }
    }
}
