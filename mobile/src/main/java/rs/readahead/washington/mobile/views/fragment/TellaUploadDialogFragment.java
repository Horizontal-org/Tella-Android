package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.TellaUploadServer;
import rs.readahead.washington.mobile.mvp.contract.ITellaUploadDialogPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.TellaUploadDialogPresenter;


public class TellaUploadDialogFragment extends BottomSheetDialogFragment implements
        ITellaUploadDialogPresenterContract.IView {
    public static final String TAG = TellaUploadDialogFragment.class.getSimpleName();
    private boolean isMetadata;

    @BindView(R.id.choose_servers)
    View chooseServersView;
    @BindView(R.id.share_metadata)
    View shareMetadataView;
    @BindView(R.id.tu_servers_list)
    RadioGroup listViewTUS;
    @BindView(R.id.cancel)
    TextView cancel;
    @BindView(R.id.next)
    TextView next;
    @BindView(R.id.media_only)
    RadioButton withoutMetadata;
    @BindView(R.id.dialog)
    View dialog;

    private Unbinder unbinder;
    private TellaUploadDialogPresenter presenter;
    private List<TellaUploadServer> servers;
    private TellaUploadServer choosenServer;

    public interface IServerMetadataChosenHandler {
        void uploadOnServer(TellaUploadServer server, boolean metadata);
    }

    private TellaUploadDialogFragment(boolean metadata) {
        this.isMetadata = metadata;
    }

    public static TellaUploadDialogFragment newInstance(boolean metadata) {
        return new TellaUploadDialogFragment(metadata);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View dialogView = inflater.inflate(R.layout.fragment_upload_dialog, container, false);
        unbinder = ButterKnife.bind(this, dialogView);

        cancel.setOnClickListener(v -> dismiss());

        next.setOnClickListener(v -> startUploading());

        servers = new ArrayList<>();

        createPresenter();

        return dialogView;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        withoutMetadata.setChecked(true);

        presenter.loadServers();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void onServersLoaded(List<TellaUploadServer> loadedServers) {
        listViewTUS.removeAllViews();
        this.servers.addAll(loadedServers);

        createServerViews(servers);
    }

    @Override
    public void onServersLoadError(Throwable throwable) {

    }

    private void createPresenter() {
        if (presenter == null) {
            presenter = new TellaUploadDialogPresenter(this);
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void createServerViews(List<TellaUploadServer> servers) {
        for (TellaUploadServer server : servers) {
            RadioButton view = getServerItem(server);
            listViewTUS.addView(view, servers.indexOf(server));
            view.setChecked(true);
            choosenServer = server;
        }

        if (servers.size() < 2 && !isMetadata) {
            startUploading();
        } else {
            setVisibility();
        }
    }

    private void setVisibility() {
        dialog.setVisibility(View.VISIBLE);
        chooseServersView.setVisibility(servers.size() < 2 ? View.GONE : View.VISIBLE);
        shareMetadataView.setVisibility(isMetadata ? View.VISIBLE : View.GONE);
    }

    private RadioButton getServerItem(TellaUploadServer server) {
        RadioButton radioButton = new RadioButton(getContext());

        if (server != null) {
            radioButton.setText(server.getName());

            radioButton.setOnClickListener(v -> choosenServer = server);
        }

        radioButton.setTag(servers.indexOf(server));
        return radioButton;
    }

    private void startUploading() {
        if (getActivity() instanceof TellaUploadDialogFragment.IServerMetadataChosenHandler) {
            isMetadata = !withoutMetadata.isChecked();
            ((TellaUploadDialogFragment.IServerMetadataChosenHandler) getActivity()).uploadOnServer(choosenServer, isMetadata);
            dismiss();
        }
    }
}
