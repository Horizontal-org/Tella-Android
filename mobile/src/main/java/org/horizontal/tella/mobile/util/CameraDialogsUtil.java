package org.horizontal.tella.mobile.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.camera.video.QualitySelector;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.presentation.entity.VideoResolutionOption;

import java.util.ArrayList;

public class CameraDialogsUtil {

    public interface VideoQualityConsumer {
        void accept(@NonNull QualitySelector qualitySelector);
    }

    @NonNull
    public static AlertDialog showVideoResolutionDialog(
            @NonNull Context context,
            @NonNull VideoQualityConsumer consumer,
            @NonNull VideoResolutionManager videoResolutionManager
    ) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        String checkedKey = videoResolutionManager.getVideoQualityOptionKey();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.video_resolution_setting_dialog, null);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);

        ArrayList<VideoResolutionOption> optionKeys = videoResolutionManager.getOptionsList();
        for (int i = 0; i < optionKeys.size(); i++) {
            AppCompatRadioButton button = (AppCompatRadioButton) inflater.inflate(R.layout.dialog_radio_button_item, null);
            button.setTag(optionKeys.get(i).getVideoQualityKey());
            button.setText(optionKeys.get(i).getVideoQualityStringResourceId());
            radioGroup.addView(button);
            if (checkedKey.equals(optionKeys.get(i).getVideoQualityKey())) {
                button.setChecked(true);
            }
        }

        builder.setView(view)
                .setPositiveButton(R.string.action_next, (dialog, which) -> {
                    int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                    AppCompatRadioButton radioButton = radioGroup.findViewById(checkedRadioButtonId);
                    String key = (String) radioButton.getTag();
                    videoResolutionManager.putVideoQualityOption(key);
                    consumer.accept(videoResolutionManager.getQualitySelector(key));
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }
}
