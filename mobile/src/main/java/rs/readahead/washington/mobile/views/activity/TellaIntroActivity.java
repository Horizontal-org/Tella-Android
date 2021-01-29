package rs.readahead.washington.mobile.views.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.ISlidePolicy;
import com.hzontal.tella_locking_ui.ui.pattern.PatternSetActivity;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import rs.readahead.washington.mobile.MyApplication;
import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.util.LocaleManager;
import rs.readahead.washington.mobile.util.PermissionUtil;


@RuntimePermissions
public class TellaIntroActivity extends AppIntro {
    public static final String FROM_ABOUT = "from_about";
    private boolean policyRespected = false;
    private AlertDialog alertDialog;
    private boolean fromAbout;
    private boolean permissionSlideEnabled;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.getInstance().getLocalizedContext(base));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        fromAbout = getIntent().getBooleanExtra(FROM_ABOUT, false);
        //permissionSlideEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        //        (!fromAbout || hasMissingPermission());
        // we have disabled above as there is no way to know if user selected
        // "do not ask again", in which case permission request will fail
        permissionSlideEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !fromAbout;

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.app_name,
                R.drawable.tella_white,
                R.string.onboard_intro_subheading,
                R.string.onboard_into_expl
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.onboard_camo_heading,
                R.drawable.ic_lock_white_24dp,
                R.string.onboard_camo_subheading,
                R.string.onboard_camo_expl
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.onboard_collect_heading,
                R.drawable.main_collect,
                R.string.onboard_collect_subheading,
                R.string.onboard_collect_expl
        )));

        addSlide(IntroFragment.newInstance(new IntroPage(
                R.string.onboard_del_heading,
                R.drawable.ic_report_problem_white_24dp,
                R.string.onboard_del_subheading,
                R.string.onboard_del_expl
        )));

        if (permissionSlideEnabled) {
            addSlide(PermissionFragment.newInstance());
        }

        showStatusBar(true);
        showSkipButton(true);
        setSkipText(getString(R.string.action_skip));
        setSeparatorColor(getResources().getColor(R.color.wa_gray));

        if (!fromAbout) {
            setDoneText(getString(R.string.onboard_perm_action_start_app));
        }
    }

    @Override
    public void onSkipPressed(@Nullable Fragment fragment) {
        if (permissionSlideEnabled) {
            pager.setCurrentItem(getSlides().size() - 1);
        } else {
            closeTellaIntro();
        }
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
    }

    @Override
    public void onDonePressed(@Nullable Fragment fragment) {
        askPermissions(); // Hzontal wants permissions to be asked until all are accepted
    }

    @Override
    public void onBackPressed() {
        boolean closeApp = pager.isFirstSlide(fragments.size());

        super.onBackPressed();

        if (!fromAbout && closeApp) {
            finish();
            MyApplication.resetKeys();
        }
    }

    public void askPermissions() {
        TellaIntroActivityPermissionsDispatcher.askPermissionsImplWithPermissionCheck(this);
    }

    @NeedsPermission({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.PROCESS_OUTGOING_CALLS,
            //Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            //Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    public void askPermissionsImpl() {
        closeTellaIntro();
    }

    @OnShowRationale({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.PROCESS_OUTGOING_CALLS,
            //Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            //Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void showPermissionsRationale(final PermissionRequest request) {
        alertDialog = PermissionUtil.showRationale(
                this, request, getString(R.string.onboard_perm_dialog_expl_grant_permissions));
    }

    @OnPermissionDenied({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.PROCESS_OUTGOING_CALLS,
            //Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            //Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void onPermissionsDenied() {
        // policyRespected = true;
    }

    @OnNeverAskAgain({
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            //Manifest.permission.PROCESS_OUTGOING_CALLS,
            //Manifest.permission.READ_CALL_LOG,
            Manifest.permission.RECORD_AUDIO,
            //Manifest.permission.SEND_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    })
    void onPermissionsNeverAskAgain() {
        closeTellaIntro();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        TellaIntroActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (alertDialog != null && alertDialog.isShowing()) {
            alertDialog.dismiss();
        }
    }

    private void closeTellaIntro() {
        if (!fromAbout) {
            Preferences.setFirstStart(false);
            startActivity(new Intent(TellaIntroActivity.this, PatternSetActivity.class));
        }

        finish();
    }

    /* private boolean hasMissingPermission() {
        String permissions[] = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.PROCESS_OUTGOING_CALLS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA
        };
        for (String permission: permissions) {
            if (!PermissionUtil.checkPermission(this, permission)) {
                return true;
            }
        }
        return false;
    } */

    public static class IntroFragment extends Fragment {
        private static final String ARG_HEADER_RES_ID = "headerResId";
        private static final String ARG_IMAGE_RES_ID = "imageResId";
        private static final String ARG_TITLE_RES_ID = "titleResId";
        private static final String ARG_TEXT_RES_ID = "textResId";
        private static final String ARG_LINK_RES_ID = "linkResId";
        private static final String ARG_LINK_ACTIVITY_CLASS = "linkActivityClass";

        private int imageResId;
        private int headerResId, titleResId, textResId;


        public static IntroFragment newInstance(IntroPage introPage) {
            IntroFragment slide = new IntroFragment();

            Bundle args = new Bundle();
            args.putInt(ARG_HEADER_RES_ID, introPage.headerResId);
            args.putInt(ARG_IMAGE_RES_ID, introPage.imageResId);
            args.putInt(ARG_TITLE_RES_ID, introPage.titleResId);
            args.putInt(ARG_TEXT_RES_ID, introPage.textResId);
            args.putInt(ARG_LINK_RES_ID, introPage.linkResId);
            args.putSerializable(ARG_LINK_ACTIVITY_CLASS, introPage.linkActivityClass);
            slide.setArguments(args);

            return slide;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setRetainInstance(true);

            if (getArguments() != null && getArguments().size() != 0) {
                imageResId = getArguments().getInt(ARG_IMAGE_RES_ID);
                headerResId = getArguments().getInt(ARG_HEADER_RES_ID);
                titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
                textResId = getArguments().getInt(ARG_TEXT_RES_ID);
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            if (savedInstanceState != null) {
                imageResId = savedInstanceState.getInt(ARG_IMAGE_RES_ID);
                headerResId = savedInstanceState.getInt(ARG_HEADER_RES_ID);
                titleResId = savedInstanceState.getInt(ARG_TITLE_RES_ID);
                textResId = savedInstanceState.getInt(ARG_TEXT_RES_ID);
            }
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_tella_intro, container, false);

            TextView he = view.findViewById(R.id.header);
            TextView ti = view.findViewById(R.id.title);
            TextView te = view.findViewById(R.id.text);
            ImageView im = view.findViewById(R.id.image);

            he.setText(getString(headerResId));
            ti.setText(getString(titleResId));
            te.setText(getString(textResId));
            im.setImageResource(imageResId);

            return view;
        }
    }

    public static class PermissionFragment extends Fragment implements ISlidePolicy {
        public static PermissionFragment newInstance() {
            return new PermissionFragment();
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_tella_intro_permissions, container, false);

            TextView li = view.findViewById(R.id.link);

            SpannableString content = new SpannableString(getString(R.string.onboard_perm_action_grant_permissions));
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            li.setText(content);
            li.setOnClickListener(v -> askPermissions());

            return view;
        }

        @Override
        public boolean isPolicyRespected() {
            TellaIntroActivity activity = (TellaIntroActivity) getActivity();
            return activity == null || activity.policyRespected;
        }

        @Override
        public void onUserIllegallyRequestedNextPage() {
            askPermissions();
        }

        private void askPermissions() {
            TellaIntroActivity activity = (TellaIntroActivity) getActivity();
            if (activity != null) {
                activity.askPermissions();
            }
        }
    }

    private static class IntroPage {
        @StringRes
        int headerResId;
        @DrawableRes
        int imageResId;
        @StringRes
        int titleResId;
        @StringRes
        int textResId;
        @StringRes
        int linkResId;
        Class linkActivityClass;

        IntroPage(int headerResId, int imageResId, int titleResId, int textResId) {
            this.headerResId = headerResId;
            this.imageResId = imageResId;
            this.titleResId = titleResId;
            this.textResId = textResId;
        }
    }
}