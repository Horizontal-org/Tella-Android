package rs.readahead.washington.mobile.util;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.AppCompatRadioButton;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.otaliastudios.cameraview.size.SizeSelector;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.Server;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.ServerType;
import rs.readahead.washington.mobile.presentation.entity.VideoResolutionOption;
import rs.readahead.washington.mobile.views.custom.CameraPreviewAnonymousButton;


public class DialogsUtil {

    static void showMessageOKCancel(Context context, String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context, R.style.PurpleBackgroundLightLettersDialogTheme);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.action_ok, okListener);
        builder.setNegativeButton(R.string.action_cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static AlertDialog showMessageOKCancelWithTitle(Context context, String message, String title, String positiveButton, String negativeButton,
                                                           DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context, R.style.PurpleBackgroundLightLettersDialogTheme)
                .setMessage(message)
                .setTitle(title)
                .setPositiveButton(positiveButton, okListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(true)
                .show();
    }

    public static AlertDialog showThreeOptionDialogWithTitle(Context context, String message, String title, String positiveButton, String neutralButton, String negativeButton,
                                                             DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener neutralListener, DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context)
                .setMessage(message)
                .setTitle(title)
                .setPositiveButton(positiveButton, okListener)
                .setNeutralButton(neutralButton, neutralListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(false)
                .show();
    }

    public static AlertDialog showDialog(
            Context context,
            String message,
            String positiveButton,
            String negativeButton,
            DialogInterface.OnClickListener okListener,
            DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context, R.style.PurpleBackgroundLightLettersDialogTheme)
                .setMessage(message)
                .setPositiveButton(positiveButton, okListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(true)
                .show();
    }

    public static ProgressDialog showLightProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context, R.style.PurpleBackgroundLightLettersDialogTheme);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(true);
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context, R.style.PurpleBackgroundLightLettersDialogTheme);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(true);
        dialog.show();
        return dialog;
    }

    public static AlertDialog showMetadataSwitchDialog(final Context context, final CameraPreviewAnonymousButton metadataCameraButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.enable_metadata_dialog_layout, null);
        builder.setView(view);

        final SwitchCompat metadataSwitch = view.findViewById(R.id.anonymous_switch);
        metadataSwitch.setChecked(!Preferences.isAnonymousMode());

        builder.setView(view)
                .setPositiveButton(R.string.action_save, (dialog, which) -> {
                    Preferences.setAnonymousMode(!metadataSwitch.isChecked());
                    metadataCameraButton.displayDrawable();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public interface ServerChosenListener {
        void onChoice(ServerType serverType);
    }

    public static AlertDialog showServerChoosingDialog(final Context context, ServerChosenListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.choose_server_dialog_layout, null);
        builder.setView(view);

        RadioButton odk = view.findViewById(R.id.radio_odk);
        RadioButton directUpload = view.findViewById(R.id.radio_direct);

        odk.setChecked(true);
        directUpload.setChecked(false);

        builder.setView(view)
                .setTitle(R.string.settings_serv_add_server_selection_dialog_title)
                .setPositiveButton(R.string.action_next, (dialog, which) -> {
                    listener.onChoice(odk.isChecked() ? ServerType.ODK_COLLECT : ServerType.TELLA_UPLOAD);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog showExitWithSubmitDialog(Context context, DialogInterface.OnClickListener okListener,
                                                       DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                context.getString(R.string.collect_end_exit_dialog_expl),
                context.getString(R.string.action_ok),
                context.getString(R.string.action_cancel),
                okListener,
                cancelListener);
    }

    public static AlertDialog showExitFileUploadDialog(Context context, DialogInterface.OnClickListener okListener,
                                                       DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                "Gallery upload",
                context.getString(R.string.action_ok),
                context.getString(R.string.action_cancel),
                okListener,
                cancelListener);
    }

    public static AlertDialog showExitOnFinalDialog(Context context, DialogInterface.OnClickListener okListener,
                                                    DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                context.getString(R.string.collect_end_exit_dialog_expl),
                context.getString(R.string.action_exit),
                context.getString(R.string.action_cancel),
                okListener,
                cancelListener);
    }

    public interface PreferenceConsumer {
        void accept(boolean t);
    }

    public static AlertDialog showMetadataProgressBarDialog(Context context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.metadata_dialog_layout, null);
        builder.setView(view)
                .setNegativeButton(R.string.action_skip, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog showFormUpdatingDialog(Context context, DialogInterface.OnClickListener listener, int stringRes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.form_updating_dialog_layout, null);
        TextView text = view.findViewById(R.id.progress_text);
        text.setText(stringRes);
        builder.setView(view)
                .setNegativeButton(R.string.action_cancel, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(arg0 -> alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(context.getResources().getColor(R.color.dark_purple)));
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.purple_background));

        return alertDialog;
    }

    public static AlertDialog showCollectRefreshProgressDialog(Context context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.collect_refresh_dialog_layout, null);
        builder.setView(view)
                .setNegativeButton(R.string.action_cancel, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();

        alertDialog.setOnShowListener(arg0 -> alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setBackgroundColor(context.getResources().getColor(R.color.dark_purple)));
        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(context.getResources().getDrawable(R.drawable.purple_background));

        return alertDialog;
    }

    public static AlertDialog showFormInstanceDeleteDialog(
            @NonNull Context context,
            CollectFormInstanceStatus status,
            @NonNull DialogInterface.OnClickListener listener) {
        int msgResId;

        if (status == CollectFormInstanceStatus.SUBMITTED) {
            msgResId = R.string.collect_dialog_text_delete_sent_form;
        } else {
            msgResId = R.string.collect_dialog_text_delete_draft_form;
        }

        return new AlertDialog.Builder(context, R.style.PurpleBackgroundLightLettersDialogTheme)
                .setMessage(msgResId)
                .setPositiveButton(R.string.action_delete, listener)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }


    public static AlertDialog showExportMediaDialog(@NonNull Context context, @NonNull DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.gallery_save_to_device_dialog_title)
                .setMessage(R.string.gallery_save_to_device_dialog_expl)
                .setPositiveButton(R.string.action_save, listener)
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    public interface VideoSizeConsumer {
        void accept(SizeSelector size);
    }

    public static AlertDialog showVideoResolutionDialog(Context context, VideoSizeConsumer consumer, VideoResolutionManager videoResolutionManager) {
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
                    consumer.accept(videoResolutionManager.getVideoSize(key));
                })
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public interface autoUploadServerConsumer {
        void accept(Server server);
    }

    public static AlertDialog chooseAutoUploadServerDialog(Context context, autoUploadServerConsumer consumer, List<TellaUploadServer> tellaUploadServers, @NonNull DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.choose_auto_upload_server_dialog, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);
        TextView errorMessage = dialogView.findViewById(R.id.error_message);

        for (int i = 0; i < tellaUploadServers.size(); i++) {
            AppCompatRadioButton button = (AppCompatRadioButton) inflater.inflate(R.layout.dialog_radio_button_item, null);
            button.setTag(i);
            button.setText(tellaUploadServers.get(i).getName());
            radioGroup.addView(button);
        }

        final AlertDialog alertDialog = builder.setView(dialogView)
                .setPositiveButton(R.string.action_ok, null)
                .setNegativeButton(R.string.action_cancel, listener)
                .setCancelable(false)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId > 0) {
                    AppCompatRadioButton radioButton = radioGroup.findViewById(checkedRadioButtonId);

                    TellaUploadServer tellaUploadServer = tellaUploadServers.get((int) radioButton.getTag());
                    consumer.accept(tellaUploadServer);
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                }
            });
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> errorMessage.setVisibility(View.GONE));
        });

        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog chooseAutoUploadServerDialog1(Context context, autoUploadServerConsumer consumer, List<Server> tellaUploadServers, @NonNull DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.choose_auto_upload_server_dialog, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.radio_group);
        TextView errorMessage = dialogView.findViewById(R.id.error_message);

        for (int i = 0; i < tellaUploadServers.size(); i++) {
            AppCompatRadioButton button = (AppCompatRadioButton) inflater.inflate(R.layout.dialog_radio_button_item, null);
            button.setTag(i);
            button.setText(tellaUploadServers.get(i).getName());
            radioGroup.addView(button);
        }

        final AlertDialog alertDialog = builder.setView(dialogView)
                .setPositiveButton(R.string.action_ok, null)
                .setNegativeButton(R.string.action_cancel, listener)
                .setCancelable(false)
                .create();

        alertDialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId > 0) {
                    AppCompatRadioButton radioButton = radioGroup.findViewById(checkedRadioButtonId);

                    Server tellaUploadServer = tellaUploadServers.get((int) radioButton.getTag());
                    consumer.accept(tellaUploadServer);
                } else {
                    errorMessage.setVisibility(View.VISIBLE);
                }
            });
            radioGroup.setOnCheckedChangeListener((group, checkedId) -> errorMessage.setVisibility(View.GONE));
        });

        alertDialog.show();

        return alertDialog;
    }
}
