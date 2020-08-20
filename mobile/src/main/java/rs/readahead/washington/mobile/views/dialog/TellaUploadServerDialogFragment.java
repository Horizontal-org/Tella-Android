package rs.readahead.washington.mobile.views.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.domain.entity.UploadProgressInfo;
import rs.readahead.washington.mobile.mvp.contract.ICheckTUSServerContract;
import rs.readahead.washington.mobile.mvp.presenter.CheckTUSServerPresenter;
import rs.readahead.washington.mobile.util.ViewUtil;


public class TellaUploadServerDialogFragment extends DialogFragment implements
        ICheckTUSServerContract.IView {
    public static final String TAG = TellaUploadServerDialogFragment.class.getSimpleName();

    private static final String TITLE_KEY = "tk";
    private static final String ID_KEY = "ik";
    private static final String OBJECT_KEY = "ok";

    @BindView(R.id.name_layout)
    TextInputLayout nameLayout;
    @BindView(R.id.name)
    EditText name;
    @BindView(R.id.url_layout)
    TextInputLayout urlLayout;
    @BindView(R.id.url)
    EditText url;
    @BindView(R.id.username_layout)
    TextInputLayout usernameLayout;
    @BindView(R.id.username)
    EditText username;
    @BindView(R.id.password_layout)
    TextInputLayout passwordLayout;
    @BindView(R.id.password)
    EditText password;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    @BindView(R.id.internet_error)
    TextView internetError;
    @BindView(R.id.server_input)
    View serverInput;

    private Unbinder unbinder;
    private boolean validated = true;
    private CheckTUSServerPresenter presenter;
    private AlertDialog dialog;

    public interface TellaUploadServerDialogHandler {
        void onTellaUploadServerDialogCreate(TellaUploadServer server);

        void onTellaUploadServerDialogUpdate(TellaUploadServer server);
    }


    public static TellaUploadServerDialogFragment newInstance(@Nullable TellaUploadServer server) {
        TellaUploadServerDialogFragment frag = new TellaUploadServerDialogFragment();

        Bundle args = new Bundle();
        if (server == null) {
            args.putInt(TITLE_KEY, R.string.add_server);
        } else {
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_tus_server_settings);
            args.putSerializable(ID_KEY, server.getId());
            args.putSerializable(OBJECT_KEY, server);
        }

        frag.setArguments(args);

        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        assert getArguments() != null;

        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_tella_upload_server, null);
        unbinder = ButterKnife.bind(this, dialogView);

        presenter = new CheckTUSServerPresenter(this);

        int title = getArguments().getInt(TITLE_KEY);
        final long serverId = getArguments().getLong(ID_KEY, 0);
        Object obj = getArguments().getSerializable(OBJECT_KEY);

        if (obj != null) {
            TellaUploadServer server = (TellaUploadServer) obj;
            name.setText(server.getName());
            url.setText(server.getUrl());
            username.setText(server.getUsername());
            password.setText(server.getPassword());
        }

        dialog = new AlertDialog.Builder(Objects.requireNonNull(getActivity()))
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_ok, null)
                .setNeutralButton(R.string.ra_try_again, null)
                .setNegativeButton(R.string.action_cancel, null)
                .create();

        ViewUtil.setDialogSoftInputModeVisible(dialog);

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(dialog -> {
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                validate();
                if (validated) {
                    presenter.checkServer(copyFields(new TellaUploadServer(serverId)), false);
                }
            });

            button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                validate();
                if (validated) {
                    presenter.checkServer(copyFields(new TellaUploadServer(serverId)), true);
                }
            });
            button.setVisibility(View.GONE);
        });

        internetError.setVisibility(View.GONE);
        return dialog;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        unbinder.unbind();

        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void onServerCheckSuccess(TellaUploadServer server) {
        save(server);
    }

    @Override
    public void onServerCheckFailure(UploadProgressInfo.Status status) {
        if (status == UploadProgressInfo.Status.UNAUTHORIZED) {
            usernameLayout.setError(getString(R.string.ra_collect_server_wrong_credentials));
        } else if (status == UploadProgressInfo.Status.UNKNOWN_HOST) {
            urlLayout.setError(getString(R.string.ra_unknown_host));
        } else {
            urlLayout.setError(getString(R.string.ra_collect_server_connection_error));
        }

        validated = false;
    }

    @Override
    public void onServerCheckError(Throwable error) {
        Toast.makeText(getActivity(), getString(R.string.ra_collect_server_connection_error), Toast.LENGTH_LONG).show();
        validated = false;
    }

    @Override
    public void showServerCheckLoading() {
        progressBar.setVisibility(View.VISIBLE);
        setEnabledViews(false);
    }

    @Override
    public void hideServerCheckLoading() {
        setEnabledViews(true);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onNoConnectionAvailable() {
        internetError.setVisibility(View.VISIBLE);
        internetError.requestFocus();
        validated = false;
    }

    @Override
    public void setSaveAnyway(boolean enabled) {
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(enabled ? View.VISIBLE : View.GONE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(
                getString(enabled ? R.string.ra_save_anyway : R.string.action_ok));
    }

    private void validate() {
        validated = true;
        validateRequired(name, nameLayout);
        validateUrl(url, urlLayout);
        validateRequired(username, usernameLayout);
        validateRequired(password, passwordLayout);

        internetError.setVisibility(View.GONE);
    }

    private void validateRequired(EditText field, TextInputLayout layout) {
        layout.setError(null);

        if (TextUtils.isEmpty(field.getText().toString())) {
            layout.setError(getString(R.string.settings_text_empty_field));
            validated = false;
        }
    }

    private void validateUrl(EditText field, TextInputLayout layout) {
        String url = field.getText().toString();

        layout.setError(null);

        if (TextUtils.isEmpty(url)) {
            layout.setError(getString(R.string.settings_text_empty_field));
            validated = false;
        } else {
            url = url.trim();
            field.setText(url);

            if (!Patterns.WEB_URL.matcher(url).matches()) {
                layout.setError(getString(R.string.not_web_url_field_error));
                validated = false;
            }
        }
    }

    @NonNull
    private TellaUploadServer copyFields(@NonNull TellaUploadServer server) {
        server.setName(name.getText().toString());
        server.setUrl(url.getText().toString().trim());
        server.setUsername(username.getText().toString().trim());
        server.setPassword(password.getText().toString());

        return server;
    }

    private void save(TellaUploadServer server) {
        dialog.dismiss();

        TellaUploadServerDialogHandler activity = (TellaUploadServerDialogHandler) getActivity();
        if (activity == null) {
            return;
        }

        if (server.getId() == 0) {
            activity.onTellaUploadServerDialogCreate(server);
        } else {
            activity.onTellaUploadServerDialogUpdate(server);
        }
    }

    private void setEnabledViews(boolean enabled) {
        nameLayout.setEnabled(enabled);
        urlLayout.setEnabled(enabled);
        usernameLayout.setEnabled(enabled);
        passwordLayout.setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(enabled);
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setEnabled(enabled);
    }
}