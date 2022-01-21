package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import org.javarosa.core.model.FormDef;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.bus.EventCompositeDisposable;
import rs.readahead.washington.mobile.bus.EventObserver;
import rs.readahead.washington.mobile.bus.event.CancelPendingFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormInstanceDeletedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSavedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmissionErrorEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmitStoppedEvent;
import rs.readahead.washington.mobile.bus.event.CollectFormSubmittedEvent;
import rs.readahead.washington.mobile.bus.event.ReSubmitFormInstanceEvent;
import rs.readahead.washington.mobile.bus.event.ShowBlankFormEntryEvent;
import rs.readahead.washington.mobile.bus.event.ShowFormInstanceEntryEvent;
import rs.readahead.washington.mobile.bus.event.ToggleBlankFormPinnedEvent;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.entity.collect.CollectForm;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstance;
import rs.readahead.washington.mobile.domain.entity.collect.CollectFormInstanceStatus;
import rs.readahead.washington.mobile.javarosa.FormUtils;
import rs.readahead.washington.mobile.mvp.contract.ICollectCreateFormControllerContract;
import rs.readahead.washington.mobile.mvp.contract.ICollectMainPresenterContract;
import rs.readahead.washington.mobile.mvp.presenter.CollectCreateFormControllerPresenter;
import rs.readahead.washington.mobile.mvp.presenter.CollectMainPresenter;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.util.DialogsUtil;
import rs.readahead.washington.mobile.util.PermissionUtil;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.adapters.ViewPagerAdapter;
import rs.readahead.washington.mobile.views.base_ui.BaseLockActivity;
import rs.readahead.washington.mobile.views.fragment.forms.BlankFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.forms.DraftFormsListFragment;
import rs.readahead.washington.mobile.views.fragment.forms.FormListFragment;
import rs.readahead.washington.mobile.views.fragment.forms.SubmittedFormsListFragment;
import timber.log.Timber;


@RuntimePermissions
public class CollectMainActivity extends BaseLockActivity implements
        ICollectMainPresenterContract.IView,
        ICollectCreateFormControllerContract.IView {
    int blankFragmentPosition;
    @BindView(R.id.fab)
    FloatingActionButton fab;
    @BindView(R.id.tabs)
    TabLayout tabLayout;
    @BindView(R.id.container)
    View formsViewPager;
    @BindView(R.id.blank_forms_layout)
    View noServersView;
    @BindView(R.id.blank_forms_text)
    TextView blankFormsText;
    private EventCompositeDisposable disposables;
    private CollectMainPresenter presenter;
    private CollectCreateFormControllerPresenter formControllerPresenter;
    private AlertDialog alertDialog;
    private ViewPager mViewPager;
    private ViewPagerAdapter adapter;
    private long numOfCollectServers = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_collect_main);
        ButterKnife.bind(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.collect_app_bar);
        }

        presenter = new CollectMainPresenter(this);

        initViewPageAdapter();

        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(adapter);
        mViewPager.setCurrentItem(blankFragmentPosition);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        fab.setOnClickListener(view -> {
            if (MyApplication.isConnectedToInternet(getContext())) {
                if (mViewPager.getCurrentItem() == blankFragmentPosition) {
                    getBlankFormsListFragment().refreshBlankForms();
                }
            } else {
                showToast(getString(R.string.collect_blank_toast_not_connected));
            }
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                fab.setVisibility((position == blankFragmentPosition && numOfCollectServers > 0) ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        blankFormsText.setText(Html.fromHtml(getString(R.string.collect_expl_not_connected_to_server)));
        blankFormsText.setMovementMethod(LinkMovementMethod.getInstance());
        StringUtils.stripUnderlines(blankFormsText);

        disposables = MyApplication.bus().createCompositeDisposable();
        disposables.wire(ShowBlankFormEntryEvent.class, new EventObserver<ShowBlankFormEntryEvent>() {
            @Override
            public void onNext(ShowBlankFormEntryEvent event) {
                // showFormEntry(event.getForm());
            }
        });
        disposables.wire(ToggleBlankFormPinnedEvent.class, new EventObserver<ToggleBlankFormPinnedEvent>() {
            @Override
            public void onNext(ToggleBlankFormPinnedEvent event) {
                toggleFormFavorite(event.getForm());
            }
        });
        disposables.wire(ShowFormInstanceEntryEvent.class, new EventObserver<ShowFormInstanceEntryEvent>() {
            @Override
            public void onNext(ShowFormInstanceEntryEvent event) {
                showFormInstanceEntry(event.getInstanceId());
            }
        });
        disposables.wire(CollectFormSubmittedEvent.class, new EventObserver<CollectFormSubmittedEvent>() {
            @Override
            public void onNext(CollectFormSubmittedEvent event) {
                getDraftFormsListFragment().listDraftForms();
                getSubmittedFormsListFragment().listSubmittedForms();
                setPagerToSubmittedFragment();
            }
        });
        disposables.wire(CollectFormSubmitStoppedEvent.class, new EventObserver<CollectFormSubmitStoppedEvent>() {
            @Override
            public void onNext(CollectFormSubmitStoppedEvent event) {
                getDraftFormsListFragment().listDraftForms();
                getSubmittedFormsListFragment().listSubmittedForms();
                setPagerToSubmittedFragment();
            }
        });
        disposables.wire(CollectFormSubmissionErrorEvent.class, new EventObserver<CollectFormSubmissionErrorEvent>() {
            @Override
            public void onNext(CollectFormSubmissionErrorEvent event) {
                getDraftFormsListFragment().listDraftForms();
                getSubmittedFormsListFragment().listSubmittedForms();
                setPagerToSubmittedFragment();
            }
        });
        disposables.wire(CollectFormSavedEvent.class, new EventObserver<CollectFormSavedEvent>() {
            @Override
            public void onNext(CollectFormSavedEvent event) {
                getDraftFormsListFragment().listDraftForms();
            }
        });
        disposables.wire(CollectFormInstanceDeletedEvent.class, new EventObserver<CollectFormInstanceDeletedEvent>() {
            @Override
            public void onNext(CollectFormInstanceDeletedEvent event) {
                onFormInstanceDeleteSuccess();
            }
        });
        /*disposables.wire(DeleteFormInstanceEvent.class, new EventObserver<DeleteFormInstanceEvent>() {
            @Override
            public void onNext(DeleteFormInstanceEvent event) {
                showDeleteInstanceDialog(event.getInstanceId(), event.getStatus());
            }
        });*/
        disposables.wire(CancelPendingFormInstanceEvent.class, new EventObserver<CancelPendingFormInstanceEvent>() {
            @Override
            public void onNext(CancelPendingFormInstanceEvent event) {
                showCancelPendingFormDialog(event.getInstanceId());
            }
        });
        disposables.wire(ReSubmitFormInstanceEvent.class, new EventObserver<ReSubmitFormInstanceEvent>() {
            @Override
            public void onNext(ReSubmitFormInstanceEvent event) {
                reSubmitFormInstance(event.getInstance());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        countServers();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (disposables != null) {
            disposables.dispose();
        }

        stopPresenter();
        stopCreateFormControllerPresenter();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.collect_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (id == R.id.help_item) {
            startCollectHelp();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        CollectMainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    public void onGetBlankFormDefSuccess(CollectForm collectForm, FormDef formDef) {
        startCreateFormControllerPresenter(collectForm, formDef);
    }

    @Override
    public void onInstanceFormDefSuccess(CollectFormInstance instance) {
        startCreateInstanceFormControllerPresenter(instance);
    }

    @Override
    public void onFormDefError(Throwable error) {
        String errorMessage = FormUtils.getFormDefErrorMessage(this, error);
        showToast(errorMessage);
    }

    @Override
    public void onFormControllerCreated(FormController formController) {
        if (Preferences.isAnonymousMode()) {
            startCollectFormEntryActivity(); // no need to check for permissions, as location won't be turned on
        } else {
            CollectMainActivityPermissionsDispatcher.startCollectFormEntryActivityWithPermissionCheck(this);
        }
    }

    @Override
    public void onFormControllerError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onToggleFavoriteSuccess(CollectForm form) {
        getBlankFormsListFragment().listBlankForms();
    }

    @Override
    public void onToggleFavoriteError(Throwable error) {
        Timber.d(error, getClass().getName());
    }

    @Override
    public void onFormInstanceDeleteSuccess() {
        Toast.makeText(this, R.string.collect_toast_form_deleted, Toast.LENGTH_SHORT).show();
        getSubmittedFormsListFragment().listSubmittedForms();
        getDraftFormsListFragment().listDraftForms();
    }

    @Override
    public void onFormInstanceDeleteError(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void onCountCollectServersEnded(long num) {
        numOfCollectServers = num;

        if (numOfCollectServers < 1) {
            tabLayout.setVisibility(View.GONE);
            formsViewPager.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            noServersView.setVisibility(View.VISIBLE);
        } else {
            tabLayout.setVisibility(View.VISIBLE);
            formsViewPager.setVisibility(View.VISIBLE);
            noServersView.setVisibility(View.GONE);

            if (mViewPager.getCurrentItem() == blankFragmentPosition) {
                fab.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onCountCollectServersFailed(Throwable throwable) {
        Timber.d(throwable, getClass().getName());
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    public void startCollectFormEntryActivity() {
        startActivity(new Intent(this, CollectFormEntryActivity.class));
    }

    @OnShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    void showFineLocationRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(this, request, getString(R.string.permission_dialog_expl_GPS));
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationPermissionDenied() {
        startCollectFormEntryActivity();
    }

    @OnNeverAskAgain(Manifest.permission.ACCESS_FINE_LOCATION)
    void onFineLocationNeverAskAgain() {
        startCollectFormEntryActivity();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showFormEntry(CollectForm form) {
        startGetFormDefPresenter(form);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void toggleFormFavorite(CollectForm form) {
        presenter.toggleFavorite(form);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showFormInstanceEntry(long instanceId) {
        startGetInstanceFormDefPresenter(instanceId);
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showDeleteInstanceDialog(final long instanceId, CollectFormInstanceStatus status) {
        alertDialog = DialogsUtil.showFormInstanceDeleteDialog(
                this,
                status,
                (dialog, which) -> presenter.deleteFormInstance(instanceId));
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void showCancelPendingFormDialog(final long instanceId) {
        alertDialog = new AlertDialog.Builder(this)
                .setMessage(R.string.collect_sent_dialog_expl_discard_unsent_form)
                .setPositiveButton(R.string.action_discard, (dialog, which) -> presenter.deleteFormInstance(instanceId))
                .setNegativeButton(R.string.action_cancel, (dialog, which) -> {
                })
                .setCancelable(true)
                .show();
    }

    @SuppressWarnings("MethodOnlyUsedFromInnerClass")
    private void reSubmitFormInstance(CollectFormInstance instance) {
        startActivity(new Intent(this, FormSubmitActivity.class)
                .putExtra(FormSubmitActivity.FORM_INSTANCE_ID_KEY, instance.getId()));
    }

    private void countServers() {
        presenter.countCollectServers();
    }

    private void startGetFormDefPresenter(CollectForm form) {
        presenter.getBlankFormDef(form);
    }

    private void startGetInstanceFormDefPresenter(long instanceId) {
        presenter.getInstanceFormDef(instanceId);
    }

    private void stopPresenter() {
        if (presenter != null) {
            presenter.destroy();
            presenter = null;
        }
    }

    private void startCreateFormControllerPresenter(CollectForm form, FormDef formDef) {
        stopCreateFormControllerPresenter();
        formControllerPresenter = new CollectCreateFormControllerPresenter(this);
        formControllerPresenter.createFormController(form, formDef);
    }

    private void startCreateInstanceFormControllerPresenter(CollectFormInstance instance) {
        stopCreateFormControllerPresenter();
        formControllerPresenter = new CollectCreateFormControllerPresenter(this);
        formControllerPresenter.createFormController(instance);
    }

    private void stopCreateFormControllerPresenter() {
        if (formControllerPresenter != null) {
            formControllerPresenter.destroy();
            formControllerPresenter = null;
        }
    }

    private void initViewPageAdapter() {
        adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(DraftFormsListFragment.newInstance(), getString(R.string.collect_draft_tab_title));
        adapter.addFragment(BlankFormsListFragment.newInstance(), getString(R.string.collect_tab_title_blank));
        adapter.addFragment(SubmittedFormsListFragment.newInstance(), getString(R.string.collect_sent_tab_title));
        blankFragmentPosition = getFragmentPosition(FormListFragment.Type.BLANK);
    }

    private DraftFormsListFragment getDraftFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.DRAFT);
    }

    private BlankFormsListFragment getBlankFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.BLANK);
    }

    private SubmittedFormsListFragment getSubmittedFormsListFragment() {
        return getFormListFragment(FormListFragment.Type.SUBMITTED);
    }

    private <T> T getFormListFragment(FormListFragment.Type type) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FormListFragment fragment = (FormListFragment) adapter.getItem(i);
            if (fragment.getFormListType() == type) {
                //noinspection unchecked
                return (T) fragment;
            }
        }
        throw new IllegalArgumentException();
    }

    private int getFragmentPosition(FormListFragment.Type type) {
        for (int i = 0; i < adapter.getCount(); i++) {
            FormListFragment fragment = (FormListFragment) adapter.getItem(i);
            if (fragment.getFormListType() == type) {
                return i;
            }
        }
        throw new IllegalArgumentException();
    }

    private void setPagerToSubmittedFragment() {
        mViewPager.setCurrentItem(getFragmentPosition(FormListFragment.Type.SUBMITTED));
        fab.setVisibility(View.GONE);
    }

    private void startCollectHelp() {
        startActivity(new Intent(CollectMainActivity.this, CollectHelpActivity.class));
    }

    public void hideFab() {
        if (fab != null) {
            fab.setVisibility(View.GONE);
        }
    }

    public void showFab() {
        if (fab != null) {
            fab.setVisibility(View.VISIBLE);
        }
    }
}
