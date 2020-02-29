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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.domain.entity.ServerType;
import rs.readahead.washington.mobile.presentation.entity.VideoResolutionOption;
import rs.readahead.washington.mobile.views.custom.CameraPreviewAnonymousButton;


public class DialogsUtil {
    /*public static void showInternetErrorDialog(Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(R.string.error);
        builder.setMessage(R.string.internet_error);
        builder.setNegativeButton(R.string.close, null);
        builder.setCancelable(true);
        builder.show();
    }*/

    /*public static AlertDialog showInfoDialog(Context context, String title, String message) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNegativeButton(R.string.close, null);
        builder.setCancelable(true);
        return builder.show();
    }*/

    /*public static void showOrbotDialog(final Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);

        @SuppressLint("InflateParams") TextView text = (TextView) LayoutInflater.from(context).inflate(R.layout.orbot_dialog, null);
        text.setText(Html.fromHtml(context.getString(R.string.orbot_install_info)));
        text.setLinksClickable(true);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setHighlightColor(ContextCompat.getColor(context, R.color.wa_light_gray));

        builder.setView(text);
        builder.setTitle(R.string.warning);
        builder.setNegativeButton(R.string.close_orbot, null);
        builder.setPositiveButton(R.string.orbot_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                context.startActivity(OrbotHelper.getOrbotInstallIntent(context));
                dialog.dismiss();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }*/

    static void showMessageOKCancel(Context context, String message, DialogInterface.OnClickListener okListener) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, okListener);
        builder.setNegativeButton(R.string.cancel, null);
        builder.setCancelable(true);
        builder.show();
    }

    public static AlertDialog showMessageOKCancelWithTitle(Context context, String message, String title, String positiveButton, String negativeButton,
                                                           DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        return new AlertDialog.Builder(context)
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
        return new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(positiveButton, okListener)
                .setNegativeButton(negativeButton, cancelListener)
                .setCancelable(true)
                .show();
    }

    public static ProgressDialog showLightProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(true);
        dialog.show();
        return dialog;
    }

    public static ProgressDialog showProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(true);
        dialog.show();
        return dialog;
    }

    public static AlertDialog showMetadataSwitchDialog(final Context context, final CameraPreviewAnonymousButton metadataCameraButton) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.enable_metadata_dialog_layout, null);
        builder.setView(view);

        final SwitchCompat metadataSwitch = view.findViewById(R.id.anonymous_switch);
        metadataSwitch.setChecked(!Preferences.isAnonymousMode());

        builder.setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    Preferences.setAnonymousMode(!metadataSwitch.isChecked());
                    metadataCameraButton.displayDrawable();
                })
                .setNegativeButton(R.string.cancel, null)
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
                .setTitle(R.string.what_type_of_server)
                .setPositiveButton(R.string.next_section, (dialog, which) -> {
                    listener.onChoice(odk.isChecked() ? ServerType.ODK_COLLECT : ServerType.TELLA_UPLOAD);
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog showExitWithSubmitDialog(Context context, DialogInterface.OnClickListener okListener,
                                                       DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                context.getString(R.string.ra_exit_will_stop_submission),
                context.getString(R.string.ok),
                context.getString(R.string.cancel),
                okListener,
                cancelListener);
    }

    public static AlertDialog showExitFileUploadDialog(Context context, DialogInterface.OnClickListener okListener,
                                                       DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                context.getString(R.string.exit_cancel_upload),
                context.getString(R.string.ok),
                context.getString(R.string.cancel),
                okListener,
                cancelListener);
    }

    public static AlertDialog showExitOnFinalDialog(Context context, DialogInterface.OnClickListener okListener,
                                                    DialogInterface.OnClickListener cancelListener) {
        return DialogsUtil.showDialog(context,
                context.getString(R.string.ra_exit_will_stop_submission),
                context.getString(R.string.ra_exit),
                context.getString(R.string.cancel),
                okListener,
                cancelListener);
    }

    public interface PreferenceConsumer {
        void accept(boolean t);
    }

    public static AlertDialog showOfflineSwitchDialog(final Context context, PreferenceConsumer consumer) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.enable_offline_dialog_layout, null);

        final SwitchCompat offlineSwitch = view.findViewById(R.id.offline_mode_switch);
        offlineSwitch.setChecked(Preferences.isOfflineMode());

        builder.setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    boolean offline = offlineSwitch.isChecked();
                    Preferences.setOfflineMode(offline);
                    consumer.accept(offline);
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog showMetadataProgressBarDialog(Context context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.metadata_dialog_layout, null);
        builder.setView(view)
                .setNegativeButton(R.string.skip, listener)
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
                .setNegativeButton(R.string.cancel, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

    public static AlertDialog showCollectRefreshProgressDialog(Context context, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        LayoutInflater inflater = LayoutInflater.from(context);

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.collect_refresh_dialog_layout, null);
        builder.setView(view)
                .setNegativeButton(R.string.cancel, listener)
                .setCancelable(false);


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }

/*    public static void showTrustedContactDialog(int title, final Context context, final TrustedPerson trustedPerson, final OnTrustedPersonInteractionListener listener) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        Activity activity = (Activity) context;
        LayoutInflater inflater = activity.getLayoutInflater();

        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.add_new_trusted_person_dialog, null);
        builder.setView(dialogView);
        builder.setTitle(title);
        builder.setPositiveButton(R.string.save, null);
        builder.setNegativeButton(R.string.cancel, null);

        final TextInputLayout titleLayout = dialogView.findViewById(R.id.new_person_title_layout);
        final TextInputLayout phoneLayout = dialogView.findViewById(R.id.new_person_phone_layout);
        final EditText name = dialogView.findViewById(R.id.person_name);
        final EditText phoneNumber = dialogView.findViewById(R.id.person_phone);


        name.setText(trustedPerson.getName());
        phoneNumber.setText(trustedPerson.getPhoneNumber());

        assert titleLayout != null;
        titleLayout.setError(null);


        final AlertDialog alertDialog = builder.create();
        ViewUtil.setDialogSoftInputModeVisible(alertDialog);

        alertDialog.setCancelable(true);
        alertDialog.setOnShowListener(dialog -> {
            Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String nameText = name.getText().toString();
                phoneLayout.setError(null);
                titleLayout.setError(null);

                String phoneNumberText = phoneNumber.getText().toString();
                if (nameText.length() > 0) {
                    if (phoneNumberText.length() > 0) {

                        TrustedPerson person = new TrustedPerson();
                        if (!TextUtils.isEmpty(trustedPerson.getName())) {
                            person.setColumnId(trustedPerson.getColumnId());
                        }
                        person.setName(nameText);
                        person.setPhoneNumber(phoneNumberText);
                        listener.onTrustedPersonInteraction(person);

                        alertDialog.dismiss();
                    } else {
                        phoneLayout.setError(context.getString(R.string.empty_field_error));
                        phoneLayout.requestFocus();
                    }
                } else {
                    titleLayout.setError(context.getString(R.string.empty_field_error));
                    titleLayout.requestFocus();
                }
            });
        });
        alertDialog.show();
    }*/

    public static AlertDialog showFormInstanceDeleteDialog(
            @NonNull Context context,
            CollectFormInstanceStatus status,
            @NonNull DialogInterface.OnClickListener listener) {
        int msgResId;

        switch (status) {
            case SUBMITTED:
                msgResId = R.string.ra_delete_cloned_form;
                break;

            case DRAFT:
                msgResId = R.string.ra_delete_draft_form;
                break;

            default:
                msgResId = R.string.ra_delete_form;
                break;
        }

        return new AlertDialog.Builder(context)
                .setMessage(msgResId)
                .setPositiveButton(R.string.delete, listener)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }


    public static AlertDialog showExportMediaDialog(@NonNull Context context, @NonNull DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.ra_save_to_device_storage)
                .setMessage(R.string.ra_saving_outside_tella_message)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    public static AlertDialog showVideoResolutionDialog(Context context, @NonNull DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BrightBackgroundDarkLettersDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(context);

        VideoResolutionManager videoResolutionManager = VideoResolutionManager.getInstance();

        String checkedKey = videoResolutionManager.getVideoQualityOptionKey();

        @SuppressLint("InflateParams")
        View view = inflater.inflate(R.layout.video_resolution_setting_dialog, null);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group);

        ArrayList<VideoResolutionOption> optionKeys = videoResolutionManager.getOptionsList();
        for (int i = 0; i < optionKeys.size(); i++) {
            AppCompatRadioButton button = (AppCompatRadioButton) inflater.inflate(R.layout.video_resolution_dialog_radio_button_item, null);
            button.setTag(optionKeys.get(i).getVideoQualityKey());
            button.setText(optionKeys.get(i).getVideoQualityStringResourceId());
            radioGroup.addView(button);
            if (checkedKey.equals(optionKeys.get(i).getVideoQualityKey())) {
                button.setChecked(true);
            }
        }

        builder.setView(view)
                .setPositiveButton(R.string.next_section, (dialog, which) -> {
                    int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                    AppCompatRadioButton radioButton = radioGroup.findViewById(checkedRadioButtonId);
                    String key = (String) radioButton.getTag();
                    videoResolutionManager.putVideoQualityOption(key);
                    listener.onClick(dialog, which);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                })
                .setCancelable(false);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }
}
