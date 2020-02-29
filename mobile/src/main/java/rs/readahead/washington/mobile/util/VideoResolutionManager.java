package rs.readahead.washington.mobile.util;

import com.otaliastudios.cameraview.VideoQuality;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.presentation.entity.VideoResolutionOption;


public class VideoResolutionManager {
    private static VideoResolutionManager instance;
    private static final VideoResolutionOption defaultResolution = new VideoResolutionOption("highest", VideoQuality.HIGHEST, R.string.video_quality_very_high);
    private HashMap<String, VideoResolutionOption> options;

    public synchronized static VideoResolutionManager getInstance() {
        if (instance == null) {
            instance = new VideoResolutionManager();
        }

        return instance;
    }

    private VideoResolutionManager() {
        options = new LinkedHashMap<>();
        options.put("highest", new VideoResolutionOption("highest", VideoQuality.HIGHEST, R.string.video_quality_very_high));
        options.put("high", new VideoResolutionOption("high", VideoQuality.MAX_1080P, R.string.video_quality_high));
        options.put("medium", new VideoResolutionOption("medium", VideoQuality.MAX_720P, R.string.video_quality_medium));
        options.put("low", new VideoResolutionOption("low", VideoQuality.MAX_480P, R.string.video_quality_low));
    }

    ArrayList<VideoResolutionOption> getOptionsList() {
        return new ArrayList<>(options.values());
    }

    public VideoQuality getVideoQuality() {
        if (Preferences.getVideoResolution() == null) {
            return defaultResolution.getVideoQuality();
        } else {
            return Objects.requireNonNull(options.get(Preferences.getVideoResolution())).getVideoQuality();
        }
    }

    String getVideoQualityOptionKey() {
        return Preferences.getVideoResolution() == null ? defaultResolution.getVideoQualityKey() : Preferences.getVideoResolution();
    }

    void putVideoQualityOption(String key) {
        Preferences.setVideoResolution(key);
    }
}
