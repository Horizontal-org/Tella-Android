package rs.readahead.washington.mobile.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.TrustedPerson;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.views.activity.OnTrustedPersonInteractionListener;
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
                .setCancelable(true)
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

    public static ProgressDialog showProgressDialog(Context context, String text) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setIndeterminate(true);
        dialog.setMessage(text);
        dialog.setCancelable(false);
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
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Preferences.setAnonymousMode(!metadataSwitch.isChecked());
                        metadataCameraButton.displayDrawable();
                    }
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
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

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

    public static void showTrustedContactDialog(int title, final Context context, final TrustedPerson trustedPerson, final OnTrustedPersonInteractionListener listener) {
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
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    }
                });
            }
        });
        alertDialog.show();
    }

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
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }

    public static AlertDialog showExportMediaDialog(@NonNull Context context, @NonNull DialogInterface.OnClickListener listener) {
        return new AlertDialog.Builder(context)
                .setTitle(R.string.ra_save_to_device_storage)
                .setMessage(R.string.ra_saving_outside_tella_message)
                .setPositiveButton(R.string.save, listener)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setCancelable(true)
                .show();
    }
}
