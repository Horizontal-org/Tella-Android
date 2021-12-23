package rs.readahead.washington.mobile.views.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.hzontal.shared_ui.bottomsheet.CustomBottomSheetFragment;
import org.hzontal.shared_ui.utils.DialogUtils;
import org.javarosa.core.model.FormDef;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.databinding.FragmentBlankFormsListBinding;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.mvp.contract.ICollectBlankFormListPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectBlankFormListPresenter;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.ViewUtil;
import rs.readahead.washington.mobile.views.activity.CollectMainActivity;
import rs.readahead.washington.mobile.views.adapters.CollectDownloadedFormInstanceRecycleViewAdapter;
import rs.readahead.washington.mobile.views.custom.TopSpaceItemDecoration;
import timber.log.Timber;

public class BlankFormsListFragment extends FormListFragment implements
        ICollectBlankFormListPresenterContract.IView,
        BlankFormListListener {
    private CollectBlankFormListPresenter presenter;
    private AlertDialog alertDialog;
    private boolean silentFormUpdates = false;
    private CollectDownloadedFormInstanceRecycleViewAdapter adapter;

    private FragmentBlankFormsListBinding binding;

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

        adapter = new CollectDownloadedFormInstanceRecycleViewAdapter(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentBlankFormsListBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        createPresenter();

        binding.downloadedForms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.downloadedForms.addItemDecoration(new TopSpaceItemDecoration(ViewUtil.getDpInPixels(requireContext(), 16)));
        binding.downloadedForms.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // listBlankForms();
        refreshBlankForms();
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
        binding = null;
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
        showBlankFormDownloadingDialog(R.string.collect_blank_dialog_expl_updating_form_definitions);
    }

    @Override
    public void onUpdateBlankFormDefEnd() {
        hideAlertDialog();
        Toast.makeText(getActivity(), R.string.collect_blank_toast_form_definition_updated, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBlankFormDefRemoved(String formName) {
        DialogUtils.showBottomMessage(requireActivity(), String.format("%s has been deleted.", formName), false); // todo: string
    }

    @Override
    public void onBlankFormDefRemoveError(Throwable error) {
        // todo: show error
    }

    @Override
    public void onUpdateBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        updateDownloadedFormList();
    }

    @Override
    public void onUserCancel() {
        hideAlertDialog();
        Toast.makeText(getActivity(), R.string.collect_blank_toast_refresh_canceled, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFormDefError(Throwable error) {
        String errorMessage = FormUtils.getFormDefErrorMessage(requireContext(), error);
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBlankFormsListResult(ListFormResult listFormResult) {
        adapter.setForms(listFormResult.getForms());
    }

    @Override
    public void onBlankFormsListError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public void onNoConnectionAvailable() {
        if (!silentFormUpdates) {
            Toast.makeText(getActivity(), R.string.collect_blank_toast_not_connected, Toast.LENGTH_SHORT).show();
        }
    }

    public void showDeleteBottomSheet(final CollectForm form) {
        final CustomBottomSheetFragment bottomSheet = CustomBottomSheetFragment.Companion.with(requireActivity().getSupportFragmentManager())
                .page(R.layout.form_fragment_delete_btm_sheet)
                .cancellable(true);

        bottomSheet.holder(new DeleteBottomSheetHolder(), holder -> {
            holder.title.setText(form.getForm().getName());
            holder.title2.setText(String.format("Delete \"%s\"?", form.getForm().getName())); // todo: string
            holder.delete.setOnClickListener(v -> {
                holder.firstStep.setVisibility(View.GONE);
                holder.secondStep.setVisibility(View.VISIBLE);
            });
            holder.yes.setOnClickListener(v -> {
                presenter.removeBlankFormDef(form);
                bottomSheet.dismiss();
            });
            holder.no.setOnClickListener(v -> {
                // todo: remove this
                presenter.downloadBlankFormDef(form);
                bottomSheet.dismiss();
            });
        }).transparentBackground().launch();
    }

    private void updateDownloadedFormList() {
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
            presenter = new CollectBlankFormListPresenter(this);
        }
    }

    private void destroyPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    static class DeleteBottomSheetHolder extends CustomBottomSheetFragment.PageHolder {
        public ViewGroup firstStep;
        public ViewGroup secondStep;
        public TextView title;
        public TextView delete;
        public TextView title2;
        public TextView no;
        public TextView yes;

        @Override
        public void bindView(@NonNull View view) {
            firstStep = view.findViewById(R.id.first_step);
            secondStep = view.findViewById(R.id.second_step);
            title = view.findViewById(R.id.title);
            delete = view.findViewById(R.id.delete);
            title2 = view.findViewById(R.id.title2);
            no = view.findViewById(R.id.no);
            yes = view.findViewById(R.id.yes);
        }
    }
}
