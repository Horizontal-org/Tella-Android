package rs.readahead.washington.mobile.views.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputLayout;

import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.C;


public class PanicMessageFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {
    @BindView(R.id.panic_message)
    EditText mPanicMessageView;
    @BindView(R.id.geolocation)
    SwitchCompat mGeolocationView;
    @BindView(R.id.panic_message_layout)
    TextInputLayout mPanicMessageLayout;

    private Unbinder unbinder;
    private boolean validated = true;

    public PanicMessageFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_panic_message, container, false);
        unbinder = ButterKnife.bind(this, view);
        setViews();
        mGeolocationView.setOnCheckedChangeListener(this);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.panic_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            return redirectBack();
        }

        if (id == R.id.action_select) {
            validate();
            if (validated) {
                mPanicMessageView.clearFocus();
                Preferences.setPanicMessage(mPanicMessageView.getText().toString());
                Toast.makeText(getActivity(), R.string.panic_message_saved, Toast.LENGTH_SHORT).show();
                return redirectBack();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean redirectBack() {
        if (getActivity() != null) {
            getActivity().onBackPressed();
            return true;
        }

        return false;
    }

    private void setViews() {
        mPanicMessageView.setText(TextUtils.isEmpty(Preferences.getPanicMessage()) ? getString(R.string.default_panic_message) : Preferences.getPanicMessage());
        mGeolocationView.setChecked(Preferences.isPanicGeolocationActive());
    }

    @Override
    public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        if (v.getId() == R.id.geolocation) {
            Preferences.setPanicGeolocationActive(isChecked);
            mGeolocationView.setChecked(isChecked);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case C.REQUEST_CODE_ASK_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        mGeolocationView.performClick();
                    }
                }
            }
        }
    }

    private void validate() {
        validated = true;
        validateRequired(mPanicMessageView, mPanicMessageLayout);
    }

    private void validateRequired(EditText field, TextInputLayout layout) {
        layout.setError(null);

        if (TextUtils.isEmpty(field.getText().toString())) {
            layout.setError(getString(R.string.empty_field_error));
            validated = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}
