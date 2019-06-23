package rs.readahead.washington.mobile.views.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.event.DownloadBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.RemoveFormFromBlankFormListEvent;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ToggleBlankFormPinnedEvent;
import rs.readahead.washington.mobile.bus.event.UpdateBlankFormEntryEvent;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import timber.log.Timber;


public class BlankFormsListFragment extends FormListFragment implements
        ICollectBlankFormListPresenterContract.IView {
    @BindView(R.id.blankForms)
    LinearLayout availableFormsListView;
    @BindView(R.id.downloadedForms)
    LinearLayout downloadedFormsListView;
    @BindView(R.id.avaivable_forms_title)
    TextView avaivableFormsTitle;
    @BindView(R.id.downloaded_forms_title)
    TextView downloadedFormsTitle;
    @BindView(R.id.blank_forms_info)
    TextView blankFormsInfo;

    private CollectBlankFormListPresenter presenter;
    private Unbinder unbinder;
    private List<CollectForm> availableForms;
    private List<CollectForm> downloadedForms;
    private ProgressDialog progressDialog;

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
    }

    @Override
    public void onStart() {
        super.onStart();
        listBlankForms();
    }

    @Override
    public void onDestroy() {
        destroyPresenter();
        hideProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @Override
    public void showBlankFormRefreshLoading() {
        progressDialog = DialogsUtil.showProgressDialog(getActivity(), getString(R.string.ra_getting_blank_forms));
    }

    @Override
    public void hideBlankFormRefreshLoading() {
        hideProgressDialog();
    }

    @Override
    public void onBlankFormsListResult(ListFormResult listFormResult) {
        downloadedForms.clear();
        availableForms.clear();
        blankFormsInfo.setVisibility(listFormResult.getForms().isEmpty() ? View.VISIBLE : View.GONE);
        for (CollectForm form : listFormResult.getForms()) {
            if (form.isDownloaded()) {
                downloadedForms.add(form);
            } else {
                availableForms.add(form);
            }
        }
        updateFormViews();
        // todo: make this multiply errors friendly
        for (IErrorBundle error : listFormResult.getErrors()) {
            Toast.makeText(getActivity(), String.format("%s %s", getString(R.string.ra_error_getting_forms), error.getServerName()), Toast.LENGTH_SHORT).show();
            Timber.d(error.getException(), getClass().getName());
        }
    }

    @Override
    public void onBlankFormsListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onNoConnectionAvailable() {
        Toast.makeText(getActivity(), R.string.ra_no_connection_available, Toast.LENGTH_SHORT).show();
    }

    public void updateDownloadedFormList() {
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

    public void updateForm(CollectForm form) {
        availableForms.remove(form);
        downloadedForms.add(form);
        updateFormViews();
    }

    private void setViewsVisibility() {
        downloadedFormsTitle.setVisibility(downloadedForms.size() > 0 ? View.VISIBLE : View.GONE);
        downloadedFormsListView.setVisibility(downloadedForms.size() > 0 ? View.VISIBLE : View.GONE);
        avaivableFormsTitle.setVisibility(availableForms.size() > 0 ? View.VISIBLE : View.GONE);
        availableFormsListView.setVisibility(availableForms.size() > 0 ? View.VISIBLE : View.GONE);
    }

    private void updateFormViews() {
        downloadedFormsListView.removeAllViews();
        availableFormsListView.removeAllViews();
        createCollectFormViews(availableForms, availableFormsListView);
        createCollectFormViews(downloadedForms, downloadedFormsListView);
        setViewsVisibility();
    }

    private void hideProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
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

        ViewGroup row = ButterKnife.findById(item, R.id.form_row);
        TextView name = ButterKnife.findById(item, R.id.name);
        TextView organization = ButterKnife.findById(item, R.id.organization);
        ImageButton dlOpenButton = ButterKnife.findById(item, R.id.dl_open_button);
        ImageView pinnedIcon = ButterKnife.findById(item, R.id.favorites_button);
        View rowLayout = ButterKnife.findById(item, R.id.row_layout);
        ImageButton updateButton = ButterKnife.findById(item, R.id.later_button);

        if (collectForm != null) {
            name.setText(collectForm.getForm().getName());
            organization.setText(collectForm.getServerName());

            if (collectForm.isDownloaded()) {
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_more_vert_black_24dp));
                dlOpenButton.setOnClickListener(view -> showDownloadedPopupMenu(collectForm, row, dlOpenButton));
                rowLayout.setOnClickListener(view -> MyApplication.bus().post(new ShowBlankFormEntryEvent(collectForm)));
                if (collectForm.isUpdated()) {
                    updateButton.setVisibility(View.VISIBLE);
                    updateButton.setOnClickListener(view -> MyApplication.bus().post(new UpdateBlankFormEntryEvent(collectForm)));
                } else {
                    updateButton.setVisibility(View.GONE);
                }
            } else {
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_cloud_download_black_24dp));
                dlOpenButton.setOnClickListener(view -> MyApplication.bus().post(new DownloadBlankFormEntryEvent(collectForm)));
            }

            if (collectForm.isPinned()) {
                pinnedIcon.setVisibility(View.VISIBLE);
            } else {
                pinnedIcon.setVisibility(View.GONE);
            }
        }

        return item;
    }

    void showDownloadedPopupMenu(CollectForm collectForm, ViewGroup row, ImageButton dlOpenButton) {
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
                    MyApplication.bus().post(new RemoveFormFromBlankFormListEvent(collectForm));
                    updateFormViews();
                    break;
            }
            return false;
        });

        popup.show();
    }
}
