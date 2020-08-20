package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.javarosa.core.model.FormDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ToggleBlankFormPinnedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListPresenter;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.views.activity.CollectMainActivity;
import timber.log.Timber;


public class BlankFormsListFragment extends FormListFragment implements
        ICollectBlankFormListPresenterContract.IView {
    @BindView(R.id.blankFormView)
    View blankFormView;
    @BindView(R.id.blankForms)
    LinearLayout availableFormsListView;
    @BindView(R.id.downloadedForms)
    LinearLayout downloadedFormsListView;
    @BindView(R.id.avaivable_forms_title)
    TextView availableFormsTitle;
    @BindView(R.id.downloaded_forms_title)
    TextView downloadedFormsTitle;
    @BindView(R.id.blank_forms_info)
    TextView blankFormsInfo;
    @BindView(R.id.banner)
    TextView banner;

    private CollectBlankFormListPresenter presenter;
    private Unbinder unbinder;
    private List<CollectForm> availableForms;
    private List<CollectForm> downloadedForms;
    private AlertDialog alertDialog;
    private int noUpdatedForms = 0;
    private boolean silentFormUpdates = false;

    public static BlankFormsListFragment newInstance() {
        return new BlankFormsListFragment();
    }

    @Override
    public Type getFormListType() {
        return Type.BLANK;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        availableForms = new ArrayList<>();
        downloadedForms = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_blank_forms_list, container, false);
        unbinder = ButterKnife.bind(this, rootView);
        createPresenter();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listBlankForms();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        hideAlertDialog();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void showBlankFormRefreshLoading() {
        if (alertDialog != null) return;
        if (getActivity() != null) {
            ((CollectMainActivity) getActivity()).hideFab();
        }
        if (!silentFormUpdates) {
            alertDialog = DialogsUtil.showCollectRefreshProgressDialog(getContext(), (dialog, which) -> presenter.userCancel());
        }
    }

    private void showBlankFormDownloadingDialog(int progressText) {
        if (alertDialog != null) return;
        if (getActivity() != null) {
            ((CollectMainActivity) getActivity()).hideFab();
        }
        alertDialog = DialogsUtil.showFormUpdatingDialog(getContext(), (dialog, which) -> presenter.userCancel(), progressText);
    }

    @Override
    public void hideBlankFormRefreshLoading() {
        Preferences.setLastCollectRefresh(System.currentTimeMillis());
        if (silentFormUpdates) {
            silentFormUpdates = false;
        }
        hideAlertDialog();
    }

    @Override
    public void onDownloadBlankFormDefSuccess(CollectForm collectForm) {
        updateForm(collectForm);
    }

    @Override
    public void onDownloadBlankFormDefStart() {
        showBlankFormDownloadingDialog(R.string.collect_dialog_text_download_progress);
    }

    @Override
    public void onDownloadBlankFormDefEnd() {
        hideAlertDialog();
        Toast.makeText(getActivity(), R.string.collect_toast_download_completed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdateBlankFormDefStart() {
        showBlankFormDownloadingDialog(R.string.update_in_progress);
    }

    @Override
    public void onUpdateBlankFormDefEnd() {
        hideAlertDialog();
        Toast.makeText(getActivity(), R.string.update_completed, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBlankFormDefRemoved() {
        updateFormViews();
    }

    @Override
    public void onBlankFormDefRemoveError(Throwable error) {
    }

    @Override
    public void onUpdateBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        noUpdatedForms -= 1;
        showBanner();
        updateDownloadedFormList();
    }

    @Override
    public void onUserCancel() {
        hideAlertDialog();
        Toast.makeText(getActivity(), R.string.canceled, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFormDefError(Throwable error) {
        String errorMessage = FormUtils.getFormDefErrorMessage(Objects.requireNonNull(getContext()), error);
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBlankFormsListResult(ListFormResult listFormResult) {
        updateFormLists(listFormResult);
        showBanner();
        updateFormViews();
        if (getContext() != null && MyApplication.isConnectedToInternet(getContext()) && checkIfDayHasPassed()) {
            silentFormUpdates = true;
            refreshBlankForms();
        }
    }

    private void updateFormLists(ListFormResult listFormResult) {
        noUpdatedForms = 0;
        blankFormView.setVisibility(View.VISIBLE);
        downloadedForms.clear();
        availableForms.clear();
        blankFormsInfo.setVisibility(listFormResult.getForms().isEmpty() ? View.VISIBLE : View.GONE);
        for (CollectForm form : listFormResult.getForms()) {
            if (form.isDownloaded()) {
                downloadedForms.add(form);
                if (form.isUpdated()) {
                    noUpdatedForms += 1;
                }
            } else {
                availableForms.add(form);
            }
        }
        // todo: make this multiply errors friendly
        if (!silentFormUpdates) {
            for (IErrorBundle error : listFormResult.getErrors()) {
                Toast.makeText(getActivity(), String.format("%s %s", getString(R.string.collect_blank_toast_fail_updating_form_list), error.getServerName()), Toast.LENGTH_SHORT).show();
                Timber.d(error.getException(), getClass().getName());
            }
        }
    }

    @Override
    public void onBlankFormsListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onNoConnectionAvailable() {
        if (!silentFormUpdates) {
            Toast.makeText(getActivity(), R.string.ra_no_connection_available, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateDownloadedFormList() {
        updateFormViews();
    }

    public void listBlankForms() {
        if (presenter != null) {
            presenter.listBlankForms();
        }
    }

    public void refreshBlankForms() {
        if (presenter != null) {
            presenter.refreshBlankForms();
        }
    }

    private void updateForm(CollectForm form) {
        availableForms.remove(form);
        downloadedForms.add(form);
        updateFormViews();
    }

    private void setViewsVisibility() {
        downloadedFormsTitle.setVisibility(downloadedForms.size() > 0 ? View.VISIBLE : View.GONE);
        downloadedFormsListView.setVisibility(downloadedForms.size() > 0 ? View.VISIBLE : View.GONE);
        availableFormsTitle.setVisibility(availableForms.size() > 0 ? View.VISIBLE : View.GONE);
        availableFormsListView.setVisibility(availableForms.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateFormViews() {
        downloadedFormsListView.removeAllViews();
        availableFormsListView.removeAllViews();
        createCollectFormViews(availableForms, availableFormsListView);
        createCollectFormViews(downloadedForms, downloadedFormsListView);
        setViewsVisibility();
    }

    private void hideAlertDialog() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (getActivity() != null) {
            ((CollectMainActivity) getActivity()).showFab();
        }
    }

    private void createPresenter() {
        if (presenter == null) {
            presenter = new CollectBlankFormListPresenter(this); // todo: move presenter creation out of fragments
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void createCollectFormViews(List<CollectForm> forms, LinearLayout listView) {
        for (CollectForm form : forms) {
            View view = getCollectFormItem(form);
            listView.addView(view, forms.indexOf(form));
        }
    }

    private View getCollectFormItem(CollectForm collectForm) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        FrameLayout item = (FrameLayout) inflater.inflate(R.layout.blank_collect_form_row, null);

        ViewGroup row = item.findViewById(R.id.form_row);
        TextView name = item.findViewById(R.id.name);
        TextView organization = item.findViewById(R.id.organization);
        ImageButton dlOpenButton = item.findViewById(R.id.dl_open_button);
        ImageView pinnedIcon = item.findViewById(R.id.favorites_button);
        View rowLayout = item.findViewById(R.id.row_layout);
        ImageButton updateButton = item.findViewById(R.id.later_button);

        if (collectForm != null) {
            name.setText(collectForm.getForm().getName());
            organization.setText(collectForm.getServerName());

            if (collectForm.isDownloaded()) {
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_more_vert_black_24dp));
                dlOpenButton.setOnClickListener(view -> showDownloadedPopupMenu(collectForm, row, dlOpenButton));
                rowLayout.setOnClickListener(view -> MyApplication.bus().post(new ShowBlankFormEntryEvent(collectForm)));
                if (collectForm.isUpdated()) {
                    updateButton.setVisibility(View.VISIBLE);
                    updateButton.setOnClickListener(view -> {
                        if (MyApplication.isConnectedToInternet(Objects.requireNonNull(getContext()))) {
                            presenter.updateBlankFormDef(collectForm);
                        } else {
                            // todo: (djm) handle this in presenter
                            Toast.makeText(getActivity(), R.string.not_connected_message, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    updateButton.setVisibility(View.GONE);
                }
            } else {
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_cloud_download_black_24dp));
                dlOpenButton.setOnClickListener(view -> {
                    if (MyApplication.isConnectedToInternet(Objects.requireNonNull(getContext()))) {
                        presenter.downloadBlankFormDef(collectForm);
                    } else {
                        // todo: (djm) handle this in presenter
                        Toast.makeText(getActivity(), R.string.not_connected_message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (collectForm.isPinned()) {
                pinnedIcon.setVisibility(View.VISIBLE);
            } else {
                pinnedIcon.setVisibility(View.GONE);
            }
        }

        return item;
    }

    private void showDownloadedPopupMenu(CollectForm collectForm, ViewGroup row, ImageButton dlOpenButton) {
        PopupMenu popup = new PopupMenu(row.getContext(), dlOpenButton);
        popup.inflate(R.menu.collect_server_item_menu);

        if (collectForm.isPinned()) {
            popup.getMenu().findItem(R.id.pin_server).setTitle(R.string.unpin_server);
        } else {
            popup.getMenu().findItem(R.id.pin_server).setTitle(R.string.pin_server);
        }

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.pin_server:
                    MyApplication.bus().post(new ToggleBlankFormPinnedEvent(collectForm));
                    updateFormViews();
                    break;
                case R.id.removeForm:
                    downloadedForms.remove(collectForm);
                    presenter.removeBlankFormDef(collectForm);
                    updateFormViews();
                    break;
            }
            return false;
        });

        popup.show();
    }

    private boolean checkIfDayHasPassed() {
        long lastRefresh = Preferences.getLastCollectRefresh();
        return System.currentTimeMillis() - lastRefresh > C.DAY;
    }

    private void showBanner() {
        if (noUpdatedForms > 0) {
            banner.setVisibility(View.VISIBLE);
        } else {
            banner.setVisibility(View.GONE);
        }
    }
}