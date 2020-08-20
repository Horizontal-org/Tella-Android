package rs.readahead.washington.mobile.views.dialog;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
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

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.net.UnknownHostException;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.http.HttpStatus;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectServer;
import rs.readahead.washington.mobile.mvp.contract.ICheckOdkServerContract;
import rs.readahead.washington.mobile.mvp.presenter.CheckOdkServerPresenter;
import rs.readahead.washington.mobile.util.ViewUtil;
import timber.log.Timber;


public class CollectServerDialogFragment extends DialogFragment implements
        ICheckOdkServerContract.IView {
    public static final String TAG = CollectServerDialogFragment.class.getSimpleName();

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
    private CheckOdkServerPresenter presenter;
    private AlertDialog dialog;
    private boolean securityProviderUpgradeAttempted = false;

    public interface CollectServerDialogHandler {
        void onCollectServerDialogCreate(CollectServer server);
        void onCollectServerDialogUpdate(CollectServer server);
    }


    public static CollectServerDialogFragment newInstance(@Nullable CollectServer server) {
        CollectServerDialogFragment frag = new CollectServerDialogFragment();

        Bundle args = new Bundle();
        if (server == null) {
            args.putInt(TITLE_KEY, R.string.add_server);
        } else {
            args.putInt(TITLE_KEY, R.string.settings_docu_dialog_title_odk_server_settings);
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
        View dialogView = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_collect_server, null);
        unbinder = ButterKnife.bind(this, dialogView);

        presenter = new CheckOdkServerPresenter(this);

        int title = getArguments().getInt(TITLE_KEY);
        final long serverId = getArguments().getLong(ID_KEY, 0);
        Object obj = getArguments().getSerializable(OBJECT_KEY);

        if (obj != null) {
            CollectServer server = (CollectServer) obj;
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
                    checkServer(copyFields(new CollectServer(serverId)), false);
                }
            });

            button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEUTRAL);
            button.setOnClickListener(view -> {
                validate();
                if (validated) {
                    checkServer(copyFields(new CollectServer(serverId)), true);
                }
            });
            button.setVisibility(View.GONE);
        });

        internetError.setVisibility(View.GONE);
        return dialog;
    }

    @Override
    public void onDismiss(@NotNull DialogInterface dialog) {
        super.onDismiss(dialog);
        unbinder.unbind();
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    @Override
    public void onServerCheckSuccess(CollectServer server) {
        save(server);
    }

    @Override
    public void onServerCheckFailure(IErrorBundle errorBundle) {
        if (errorBundle.getCode() == HttpStatus.UNAUTHORIZED_401) {
            usernameLayout.setError(getString(R.string.ra_collect_server_wrong_credentials));
        } else if (errorBundle.getException() instanceof UnknownHostException) {
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

    @Override
    public Context getContext() {
        return getActivity();
    }

    private void checkServer(@NonNull CollectServer server,  boolean connectionRequired) {
        // lets go with sync solution as this will not influence UX too much here
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 &&
                !securityProviderUpgradeAttempted && getContext() != null) {
            try {
                ProviderInstaller.installIfNeeded(getContext());
            } catch (GooglePlayServicesRepairableException e) {
                GoogleApiAvailability.getInstance()
                        .showErrorNotification(getContext(), e.getConnectionStatusCode());
                securityProviderUpgradeAttempted = true;
                return;
            } catch (GooglePlayServicesNotAvailableException e) {
                Timber.d(e);
            }
        }

        if (presenter != null) {
            presenter.checkServer(server, connectionRequired);
        }
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
    private CollectServer copyFields(@NonNull CollectServer server) {
        server.setName(name.getText().toString());
        server.setUrl(url.getText().toString().trim());
        server.setUsername(username.getText().toString().trim());
        server.setPassword(password.getText().toString());

        return server;
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

    private void save(CollectServer server) {
        dialog.dismiss();

        CollectServerDialogHandler activity = (CollectServerDialogHandler) getActivity();
        if (activity == null) {
            return;
        }

        if (server.getId() == 0) {
            activity.onCollectServerDialogCreate(server);
        } else {
            activity.onCollectServerDialogUpdate(server);
        }
    }
}
