package rs.readahead.washington.mobile.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;
import rs.readahead.washington.mobile.views.activity.SplashActivity;


public class CamouflageManager {
    private static CamouflageManager instance;
    private static final String defaultAlias = SplashActivity.class.getCanonicalName();
    private final List<CamouflageOption> options;

    public final CamouflageOption calculatorOption = new CamouflageOption(getOptionAlias("Calculator"), R.drawable.calculator, R.string.settings_camo_calculator2);

    public synchronized static CamouflageManager getInstance() {
        if (instance == null) {
            instance = new CamouflageManager();
        }

        return instance;
    }

    private CamouflageManager() {
        options = new ArrayList<>();
        options.add(new CamouflageOption(defaultAlias, R.drawable.tella_black, R.string.app_name));
        options.add(new CamouflageOption(getOptionAlias("Camera"), R.drawable.camera, R.string.settings_camo_camera4));
        options.add(new CamouflageOption(getOptionAlias("CameraPro"), R.drawable.camera_pro, R.string.settings_camo_camera3));
        options.add(new CamouflageOption(getOptionAlias("SuperCam"), R.drawable.super_cam, R.string.settings_camo_camera2));
        options.add(new CamouflageOption(getOptionAlias("EasyCam"), R.drawable.easy_cam, R.string.settings_camo_camera1));
        options.add(new CamouflageOption(getOptionAlias("Weather"), R.drawable.weather, R.string.settings_camo_weather1));
        options.add(new CamouflageOption(getOptionAlias("WeatherNow"), R.drawable.weather_now, R.string.settings_camo_weather3));
        options.add(new CamouflageOption(getOptionAlias("LocalWeather"), R.drawable.local_weather, R.string.settings_camo_weather2));
        //options.add(new CamouflageOption(getOptionAlias("Calculator"), R.drawable.calculator, R.string.settings_camo_calculator2));
        options.add(new CamouflageOption(getOptionAlias("EasyMath"), R.drawable.easy_math, R.string.settings_camo_calculator1));
    }

    public boolean setLauncherActivityAlias(@NonNull Context context, @NonNull String activityAlias) {
        String currentAlias = Preferences.getAppAlias();
        if (activityAlias.equals(currentAlias)) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getApplicationContext().getPackageName();

        List<CamouflageOption> fullOptions = new ArrayList<>(options);
        fullOptions.add(calculatorOption);

        for (CamouflageOption option : fullOptions) {
            packageManager.setComponentEnabledSetting(
                    new ComponentName(packageName, option.alias),
                    option.alias.equals(activityAlias) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }

        Preferences.setAppAlias(activityAlias);

        return true;
    }

    public boolean isDefaultLauncherActivityAlias() {
        String currentAlias = Preferences.getAppAlias();
        return currentAlias == null || defaultAlias.equals(currentAlias);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setDefaultLauncherActivityAlias(@NonNull Context context) {
        //noinspection SimplifiableIfStatement
        if (isDefaultLauncherActivityAlias()) {
            return false;
        }

        return setLauncherActivityAlias(context, defaultAlias);
    }

    public List<CamouflageOption> getOptions() {
        return options;
    }

    public int getSelectedAliasPosition() {
        String currentAlias = Preferences.getAppAlias();

        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).alias.equals(currentAlias)) {
                return i;
            }
        }

        return 0;
    }

    public void disableSecretMode(@NonNull Context context) {
        Preferences.setSecretModeActive(false);
        setActiveLauncherComponent(context, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }

    public void enableSecretMode(@NonNull Context context) {
        Preferences.setSecretModeActive(true);
        setActiveLauncherComponent(context, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
    }

    private void setActiveLauncherComponent(@NonNull Context context, int state) {
        ComponentName componentName = getLauncherComponentName(context);
        PackageManager packageManager = context.getPackageManager();

        packageManager.setComponentEnabledSetting(
                componentName,
                state,
                PackageManager.DONT_KILL_APP);
    }

    private ComponentName getLauncherComponentName(@NonNull Context context) {
        String activityAlias = Preferences.getAppAlias();

        if (activityAlias == null) {
            activityAlias = options.get(0).alias;
        }

        return new ComponentName(context.getApplicationContext().getPackageName(), activityAlias);
    }

    public CharSequence getLauncherName(@NonNull Context context) {
        try {
            if (isDefaultLauncherActivityAlias()) {
                return context.getString(R.string.settings_lang_select_default);
            }
            return context.getResources().getString(context.getPackageManager().getActivityInfo(getLauncherComponentName(context), 0).labelRes);
        } catch (PackageManager.NameNotFoundException e) {
            return context.getResources().getString(R.string.settings_camo_error_fail_retrieve_app_name);
        }
    }

    private String getOptionAlias(String alias) {
        return "rs.readahead.washington.mobile.views.activity.Alias" + alias;
    }
}
