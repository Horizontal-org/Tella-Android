package rs.readahead.washington.mobile.views.fragment.forms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import org.hzontal.shared_ui.bottomsheet.BottomSheetUtils;
import org.hzontal.shared_ui.utils.DialogUtils;
import org.javarosa.core.model.FormDef;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.IErrorBundle;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.ListFormResult;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.util.C;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.TellaUpgrader;
import rs.readahead.washington.mobile.views.activity.MainActivity;
import rs.readahead.washington.mobile.views.fragment.vault.home.HomeVaultFragment;
import timber.log.Timber;


public class BlankFormsListFragment extends FormListFragment {
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
    SharedFormsViewModel model = null;
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
        model = new ViewModelProvider(this).get(SharedFormsViewModel.class);
        initObservers();
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
       if (!Preferences.isJavarosa3Upgraded()) {
            model.getShowFab().postValue(false);
            showJavarosa2UpgradeSheet();
        } else {
            listBlankForms();
       }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        hideAlertDialog();
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    private void showBlankFormDownloadingDialog(int progressText) {
        if (alertDialog != null) return;
        if (getActivity() != null) {
            model.getShowFab().postValue(false);
        }
        alertDialog = DialogsUtil.showFormUpdatingDialog(getContext(), (dialog, which) -> model.userCancel(), progressText);
    }

    private void initObservers() {
        model.getShowBlankFormRefreshLoading().observe(getViewLifecycleOwner(), show -> {
            if (!show) {
                Preferences.setLastCollectRefresh(System.currentTimeMillis());
                if (silentFormUpdates) {
                    silentFormUpdates = false;
                }
                hideAlertDialog();
            } else {
                if (alertDialog != null) return;
                if (getActivity() != null) {
                    model.getShowFab().postValue(false);
                }
                if (!silentFormUpdates) {
                    alertDialog = DialogsUtil.showCollectRefreshProgressDialog(getContext(), (dialog, which) -> model.userCancel());
                }
            }
        });

        model.getOnDownloadBlankFormDefSuccess().observe(getViewLifecycleOwner(), this::updateForm);
        model.getOnDownloadBlankFormDefStart().observe(getViewLifecycleOwner(), show -> {
            if (show) {
                showBlankFormDownloadingDialog(R.string.collect_dialog_text_download_progress);
            } else {
                hideAlertDialog();
                DialogUtils.showBottomMessage(getActivity(), getString(R.string.collect_toast_download_completed), false);
            }
        });

        model.getOnUpdateBlankFormDefStart().observe(getViewLifecycleOwner(), show -> {
            if (show) {
                showBlankFormDownloadingDialog(R.string.collect_blank_dialog_expl_updating_form_definitions);

            } else {
                hideAlertDialog();
                Toast.makeText(getActivity(), R.string.collect_blank_toast_form_definition_updated, Toast.LENGTH_SHORT).show();
            }
        });

        model.getOnBlankFormDefRemoved().observe(getViewLifecycleOwner(), it -> {
            updateFormViews();
        });

        model.getOnUpdateBlankFormDefSuccess().observe(getViewLifecycleOwner(), it -> onUpdateBlankFormDefSuccess(it.getFirst(), it.getSecond())
        );

        model.getOnUserCancel().observe(getViewLifecycleOwner(), cancel -> {
            hideAlertDialog();
            Toast.makeText(getActivity(), R.string.collect_blank_toast_refresh_canceled, Toast.LENGTH_SHORT).show();
        });

        model.getOnFormDefError().observe(getViewLifecycleOwner(), this::onFormDefError);

        model.getOnFormCacheCleared().observe(getViewLifecycleOwner(), cleared -> {
            listBlankForms();
            model.getShowFab().postValue(true);
        });

        model.getOnBlankFormsListResult().observe(getViewLifecycleOwner(), this::onBlankFormsListResult);

        model.getOnNoConnectionAvailable().observe(getViewLifecycleOwner(), available -> {
            if (!silentFormUpdates) {
                Toast.makeText(getActivity(), R.string.collect_blank_toast_not_connected, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onUpdateBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        noUpdatedForms -= 1;
        showBanner();
        updateDownloadedFormList();
    }

    private void onFormDefError(Throwable error) {
        String errorMessage = FormUtils.getFormDefErrorMessage(requireContext(), error);
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    private void onBlankFormsListResult(ListFormResult listFormResult) {
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

    private void updateDownloadedFormList() {
        updateFormViews();
    }

    public void listBlankForms() {
        if (model != null) {
            model.listBlankForms();
        }
    }

    public void refreshBlankForms() {
        if (model != null) {
            model.refreshBlankForms();
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
            model.getShowFab().postValue(true);
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
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_more));
                dlOpenButton.setContentDescription(getString(R.string.collect_blank_action_desc_more_options));
                dlOpenButton.setOnClickListener(view -> showDownloadedMenu(collectForm));
                rowLayout.setOnClickListener(view -> model.getBlankFormDef(collectForm));
                pinnedIcon.setOnClickListener(view -> {
                    model.toggleFavorite(collectForm);
                    updateFormViews();
                });

                if (collectForm.isUpdated()) {
                    pinnedIcon.setVisibility(View.VISIBLE);
                    updateButton.setVisibility(View.VISIBLE);
                    updateButton.setOnClickListener(view -> {
                        if (MyApplication.isConnectedToInternet(requireContext())) {
                            model.updateBlankFormDef(collectForm);
                        } else {
                            // todo: (djm) handle this in presenter
                            Toast.makeText(getActivity(), R.string.collect_blank_toast_not_connected, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    updateButton.setVisibility(View.GONE);
                }
            } else {
                pinnedIcon.setVisibility(View.GONE);
                dlOpenButton.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.ic_download));
                dlOpenButton.setContentDescription(getString(R.string.collect_blank_action_download_form));
                dlOpenButton.setOnClickListener(view -> {
                    if (MyApplication.isConnectedToInternet(requireContext())) {
                        model.downloadBlankFormDef(collectForm);
                    } else {
                        // todo: (djm) handle this in presenter
                        Toast.makeText(getActivity(), R.string.collect_blank_toast_not_connected, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (collectForm.isPinned()) {
                pinnedIcon.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.star_filled_24dp));
            } else {
                pinnedIcon.setImageDrawable(row.getContext().getResources().getDrawable(R.drawable.star_border_24dp));
            }
        }

        return item;
    }

    private void showDownloadedMenu(CollectForm collectForm) {
        BottomSheetUtils.showEditDeleteMenuSheet(
                requireActivity().getSupportFragmentManager(),
                collectForm.getForm().getName(),
                requireContext().getString(R.string.Collect_Action_FillForm),
                requireContext().getString(R.string.action_delete),
                action -> {
                    if (action == BottomSheetUtils.Action.EDIT) {
                        model.getBlankFormDef(collectForm);
                    }
                    if (action == BottomSheetUtils.Action.DELETE) {
                        downloadedForms.remove(collectForm);
                        model.removeBlankFormDef(collectForm);
                        updateFormViews();
                    }
                },
                requireContext().getString(R.string.Collect_RemoveForm_SheetTitle),
                String.format(requireContext().getResources().getString(R.string.Collect_Subtitle_RemoveForm), collectForm.getForm().getName()),
                requireContext().getString(R.string.action_remove),
                requireContext().getString(R.string.action_cancel)
        );
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

    private void showJavarosa2UpgradeSheet() {
        BottomSheetUtils.showConfirmSheet(
                requireActivity().getSupportFragmentManager(),
                null,
                getString(R.string.Javarosa_Upgrade_Warning_Description),
                getString(R.string.action_continue),
                getString(R.string.action_cancel),
                isConfirmed -> {
                    if (isConfirmed) {
                            upgradeJavarosa2();
                    } else {
                        goHome();
                    }
                });
    }

    private void upgradeJavarosa2() {
        try {
            Toast.makeText(getContext(), getString(R.string.Javarosa_Upgrade_Toast), Toast.LENGTH_LONG).show();
            model.deleteCachedForms();
        } catch (Throwable t) {
            Timber.d(t);
        }
    }

    private void goHome(){
        if (getActivity() == null) return;
        ((MainActivity)requireActivity()).selectHome();
    }

}