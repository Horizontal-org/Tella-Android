package rs.readahead.washington.mobile.util;

import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_ALIAS_BLUE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_ALIAS_ORANGE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_ALIAS_YELLOW_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_BLUE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_ORANGE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALCULATOR_YELLOW_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALC_ALIAS_BLUE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALC_ALIAS_GREEN_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALC_ALIAS_ORANGE_SKIN;
import static com.hzontal.tella_locking_ui.ConstantsKt.CALC_ALIAS_YELLOW_SKIN;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.hzontal.tella_locking_ui.common.util.DivviupUtils;

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

    public final CamouflageOption calculatorOption = new CamouflageOption(getOptionAlias(CALC_ALIAS_GREEN_SKIN), R.drawable.calc_green_skin_foreground, R.string.settings_camo_calculator2);
    final CamouflageOption defaultOption = new CamouflageOption(defaultAlias, R.drawable.tella_black, R.string.app_name);

    public synchronized static CamouflageManager getInstance() {
        if (instance == null) {
            instance = new CamouflageManager();
        }

        return instance;
    }


    private CamouflageManager() {
        options = new ArrayList<>();
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.I_CAMERA_ALIAS), R.mipmap.ic_camera_round, R.string.settings_camo_icamera));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.SELFIE_CAM_ALIAS), R.mipmap.ic_selfie_cam_round, R.string.settings_camo_selfie_cam));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.SNAP_CAMERA_ALIAS), R.mipmap.ic_snap_camera_round, R.string.settings_camo_snap_camera));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.MI_CAMERA_ALIAS), R.mipmap.ic_mi_camera_round, R.string.settings_camo_micamera));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.CALCULATE_ALIAS), R.mipmap.ic_calculate_round, R.string.settings_camo_calculate));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.CALCULATOR_PLUS_ALIAS), R.mipmap.ic_calculator_plus_round, R.string.settings_camo_calculator_plus));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.I_CALCULATOR_ALIAS), R.mipmap.ic_i_calculator_round, R.string.settings_camo_icalculator));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.CALCULATOR_ALIAS), R.mipmap.ic_calculator_round, R.string.settings_camo_calculator2));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.WEATHER_ALIAS), R.mipmap.ic_weather_round, R.string.settings_camo_weather));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.EASY_WEATHER_ALIAS), R.mipmap.ic_easy_weather_round, R.string.settings_camo_easyweather));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.SUNNY_DAY_ALIAS), R.mipmap.ic_sunny_day_round, R.string.settings_camo_sunnyday));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.FORECAST_ALIAS), R.mipmap.ic_forecast_round, R.string.settings_camo_forecast));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.GAME_STATION_ALIAS), R.mipmap.ic_game_station_round, R.string.settings_camo_gamestation));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.PLAY_NOW_ALIAS), R.mipmap.ic_play_now_round, R.string.settings_camo_playnow));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.GAME_LAUNCHER_ALIAS), R.mipmap.ic_game_launcher_round, R.string.settings_camo_game_launcher));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.CLOCK_ALIAS), R.mipmap.ic_clock_round, R.string.settings_camo_clock));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.JEWEL_DASH_ALIAS), R.mipmap.ic_jewel_dash_round, R.string.settings_camo_jeweldash));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.TIME_ALIAS), R.mipmap.ic_time_round, R.string.settings_camo_time));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.STOP_WATCH_ALIAS), R.mipmap.ic_stop_watch_round, R.string.settings_camo_stopwatch));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.WATCH_ALIAS), R.mipmap.ic_watch_round, R.string.settings_camo_watch));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.WORKOUT_ALIAS), R.mipmap.ic_workout_round, R.string.settings_camo_workout));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.FITNESS_LIFE_ALIAS), R.mipmap.ic_fitness_life_round, R.string.settings_camo_fitness_life));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.HEALTH_ALIAS), R.mipmap.ic_health_round, R.string.settings_camo_health));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.PERIOD_TRACKER_ALIAS), R.mipmap.ic_period_tracker_round, R.string.settings_camo_period_tracker));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.MUSIC_ALIAS), R.mipmap.ic_music_round, R.string.settings_camo_music));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.DICTIONARY_ALIAS), R.mipmap.ic_dictionary_round, R.string.settings_camo_dictionary));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.PLANT_CARE_ALIAS), R.mipmap.ic_plant_care_round, R.string.settings_camo_plant_care));
        options.add(new CamouflageOption(getOptionAlias(CamouflageConstant.ASTROLOGY_ALIAS), R.mipmap.ic_astrology_round, R.string.settings_camo_astrology));
    }

    public CamouflageOption getCalculatorOptionByTheme(String calculatorTheme) {
        if (calculatorTheme == null) return calculatorOption;
        switch (calculatorTheme) {
            case CALCULATOR_BLUE_SKIN:
                return new CamouflageOption(getOptionAlias(CALC_ALIAS_BLUE_SKIN), R.drawable.calc_blue_skin_foreground, R.string.settings_camo_calculator2);
            case CALCULATOR_YELLOW_SKIN:
                return new CamouflageOption(getOptionAlias(CALC_ALIAS_YELLOW_SKIN), R.drawable.calc_yellow_skin_foreground, R.string.settings_camo_calculator2);
            case CALCULATOR_ORANGE_SKIN:
                return new CamouflageOption(getOptionAlias(CALC_ALIAS_ORANGE_SKIN), R.drawable.calc_orange_skin_foreground, R.string.settings_camo_calculator2);
            default:
                return calculatorOption;
        }
    }

    public boolean setLauncherActivityAlias(@NonNull Context context, @NonNull String activityAlias) {
        String currentAlias = Preferences.getAppAlias();
        Preferences.setAppAlias(activityAlias);
        if (activityAlias.equals(currentAlias)) {
            return false;
        }
        PackageManager packageManager = context.getPackageManager();
        String packageName = context.getApplicationContext().getPackageName();
        List<CamouflageOption> fullOptions = new ArrayList<>(options);
        if (currentAlias != null) {
            CamouflageOption calculatorCurrentOption = new CamouflageOption(currentAlias, getOptionDrawable(currentAlias), R.string.settings_camo_calculator2);
            fullOptions.add(calculatorCurrentOption);
        }
        CamouflageOption calculatorChosenOption = new CamouflageOption(activityAlias, getOptionDrawable(activityAlias), R.string.settings_camo_calculator2);
        fullOptions.add(defaultOption);
        fullOptions.add(calculatorChosenOption);
        for (CamouflageOption option : fullOptions) {
            packageManager.setComponentEnabledSetting(
                    new ComponentName(packageName, option.alias),
                    option.alias.equals(activityAlias) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
        }
        DivviupUtils.INSTANCE.runCamouflageEnabledEvent(context);
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

    private int getOptionDrawable(String alias) {

        if (alias == null) return R.drawable.calculator_foreground;
        switch (alias) {
            case CALCULATOR_ALIAS_BLUE_SKIN:
                return R.drawable.calc_blue_skin_foreground;
            case CALCULATOR_ALIAS_YELLOW_SKIN:
                return R.drawable.calc_yellow_skin_foreground;
            case CALCULATOR_ALIAS_ORANGE_SKIN:
                return R.drawable.calc_orange_skin_foreground;
            default:
                return R.drawable.calculator_foreground;
        }
    }
}
