package rs.readahead.washington.mobile.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import org.hzontal.shared_ui.utils.CalculatorTheme;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.domain.repository.ITellaUploadsRepository;
import rs.readahead.washington.mobile.presentation.entity.CamouflageOption;
import rs.readahead.washington.mobile.views.activity.SplashActivity;


public class CamouflageManager {
    private static CamouflageManager instance;
    private static final String defaultAlias = SplashActivity.class.getCanonicalName();
    private final List<CamouflageOption> options;
    public static final int defaultAliasPosition = 15;

   // public final CamouflageOption calculatorOption = new CamouflageOption(getOptionAlias("Calculator"), R.drawable.calc_orange_skin_foreground, R.string.settings_camo_calculator2);
    final CamouflageOption defaultOption = new CamouflageOption(defaultAlias, R.drawable.tella_black, R.string.app_name);

    public synchronized static CamouflageManager getInstance() {
        if (instance == null) {
            instance = new CamouflageManager();
        }

        return instance;
    }


    private CamouflageManager() {
        options = new ArrayList<>();
        options.add(new CamouflageOption(getOptionAlias("iCamera"), R.drawable.icamera_foreground, R.string.settings_camo_icamera));
        options.add(new CamouflageOption(getOptionAlias("SelfieCam"), R.drawable.selfiecamera_foreground, R.string.settings_camo_selfie_cam));
        options.add(new CamouflageOption(getOptionAlias("SnapCamera"), R.drawable.snapcamera_foreground, R.string.settings_camo_snap_camera));
        options.add(new CamouflageOption(getOptionAlias("Weather"), R.drawable.weather_foreground, R.string.settings_camo_weather));
        options.add(new CamouflageOption(getOptionAlias("EasyWeather"), R.drawable.easyweather_foreground, R.string.settings_camo_easyweather));
        options.add(new CamouflageOption(getOptionAlias("SunnyDay"), R.drawable.sunnyday_foreground, R.string.settings_camo_sunnyday));
        options.add(new CamouflageOption(getOptionAlias("GameStation"), R.drawable.gamestation_foreground, R.string.settings_camo_gamestation));
        options.add(new CamouflageOption(getOptionAlias("PlayNow"), R.drawable.playnow_foreground, R.string.settings_camo_playnow));
        options.add(new CamouflageOption(getOptionAlias("JewelDash"), R.drawable.jeweldash_foreground, R.string.settings_camo_jeweldash));
        options.add(new CamouflageOption(getOptionAlias("Clock"), R.drawable.clock_foreground, R.string.settings_camo_clock));
        options.add(new CamouflageOption(getOptionAlias("Time"), R.drawable.time_foreground, R.string.settings_camo_time));
        options.add(new CamouflageOption(getOptionAlias("StopWatch"), R.drawable.stopwatch_foreground, R.string.settings_camo_stopwatch));
       // options.add(new CamouflageOption(getOptionAlias("Calculator"), R.drawable.calculator_foreground_circle, R.string.settings_camo_calculator2));
      //  options.add(new CamouflageOption(getOptionAlias("Calculate"), R.drawable.calculate_foreground, R.string.settings_camo_calculate));
      //  options.add(new CamouflageOption(getOptionAlias("CalculatorPlus"), R.drawable.calculatorplus_foreground, R.string.settings_camo_calculator_plus));
       // options.add(new CamouflageOption(getOptionAlias("iCalculator"), R.drawable.icalculator_foreground, R.string.settings_camo_icalculator));
       // options.add(new CamouflageOption(getOptionAlias("iCalculator"), R.drawable.icalculator_foreground, R.string.settings_camo_icalculator));

    }

    public CamouflageOption getCalculatorOptionByTheme(String calculatorTheme){
        switch (calculatorTheme)
        {
            case "BLUE_SKIN":
                return new CamouflageOption(getOptionAlias("CalculatorBlueSkin"), R.drawable.calc_blue_skin_foreground, R.string.settings_camo_calculator2);
            case "YELLOW_SKIN":
                return new CamouflageOption(getOptionAlias("CalculatorYellowSkin"), R.drawable.calc_yellow_skin_foreground, R.string.settings_camo_calculator2);
            case "ORANGE_SKIN":
                return new CamouflageOption(getOptionAlias("CalculatorOrangeSkin"), R.drawable.calc_orange_skin_foreground, R.string.settings_camo_calculator2);
            default:
                return new CamouflageOption(getOptionAlias("Calculator"), R.drawable.calc_green_skin_foreground, R.string.settings_camo_calculator2);

        }

    }
    public boolean setLauncherActivityAlias(@NonNull Context context, @NonNull String activityAlias) {
        String currentAlias = Preferences.getAppAlias();
        if (activityAlias.equals(currentAlias)) {
            return false;
        }

        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getApplicationContext().getPackageName();

        List<CamouflageOption> fullOptions = new ArrayList<>(options);
        fullOptions.add(defaultOption);
        fullOptions.add(getCalculatorOptionByTheme(Preferences.getCalculatorTheme()));

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
