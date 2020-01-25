package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;


public class ShareDialogFragment extends BottomSheetDialogFragment {
    public static final String TAG = ShareDialogFragment.class.getSimpleName();

    @BindView(R.id.cancel)
    TextView cancel;
    @BindView(R.id.next)
    TextView next;
    @BindView(R.id.share_media_with_metadata)
    RadioButton shareMetadata;

    private Unbinder unbinder;

    public interface IShareDialogFragmentHandler {
        void sharingMediaMetadataSelected();

        void sharingMediaOnlySelected();
    }

    public static ShareDialogFragment newInstance() {
        return new ShareDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_export_dialog, container, false);
        unbinder = ButterKnife.bind(this, view);

        cancel.setOnClickListener(v -> dismiss());

        next.setOnClickListener(v -> startShare());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void startShare() {
        if (getActivity() instanceof IShareDialogFragmentHandler) {
            if (shareMetadata.isChecked()) {
                ((IShareDialogFragmentHandler) getActivity()).sharingMediaMetadataSelected();
            } else {
                ((IShareDialogFragmentHandler) getActivity()).sharingMediaOnlySelected();
            }
        }
    }
}
