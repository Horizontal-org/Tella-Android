package rs.readahead.washington.mobile.util;

import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.presentation.entity.VideoResolutionOption;


public class VideoResolutionManager {
    private static VideoResolutionManager instance;
    private static final VideoResolutionOption defaultResolution = new VideoResolutionOption("highest",
            SizeSelectors.or(SizeSelectors.and(SizeSelectors.aspectRatio(AspectRatio.of(4, 3), 0), SizeSelectors.biggest())),R.string.video_quality_very_high);
    private HashMap<String, VideoResolutionOption> options;

    public VideoResolutionManager() {
        options = new LinkedHashMap<>();
        options.put("highest", new VideoResolutionOption("highest",
                SizeSelectors.or(SizeSelectors.and(SizeSelectors.maxWidth(3840), SizeSelectors.maxHeight(2160)),
                        SizeSelectors.aspectRatio(AspectRatio.of(16, 9), 0), SizeSelectors.biggest()),
                R.string.video_quality_very_high));
        options.put("high", new VideoResolutionOption("high",
                SizeSelectors.or(SizeSelectors.and(SizeSelectors.maxWidth(1920), SizeSelectors.maxHeight(1080)),
                        SizeSelectors.aspectRatio(AspectRatio.of(16, 9), 0), SizeSelectors.biggest()),
                R.string.video_quality_high));
        options.put("medium", new VideoResolutionOption("medium",
                SizeSelectors.or(SizeSelectors.and(SizeSelectors.maxWidth(1280), SizeSelectors.maxHeight(720)),
                        SizeSelectors.aspectRatio(AspectRatio.of(16, 9), 0), SizeSelectors.biggest()),
                R.string.video_quality_medium));
        options.put("low", new VideoResolutionOption("low",
                SizeSelectors.or(SizeSelectors.and(SizeSelectors.maxWidth(640), SizeSelectors.maxHeight(480)),
                        SizeSelectors.aspectRatio(AspectRatio.of(4, 3), 0), SizeSelectors.biggest()),
                R.string.video_quality_low));
    }

    public SizeSelector getVideoSize() {
        if (Preferences.getVideoResolution() == null) {
            return defaultResolution.getVideoQuality();
        } else {
            return Objects.requireNonNull(options.get(Preferences.getVideoResolution())).getVideoQuality();
        }
    }

    ArrayList<VideoResolutionOption> getOptionsList() {
        return new ArrayList<>(options.values());
    }

    String getVideoQualityOptionKey() {
        return Preferences.getVideoResolution() == null ? defaultResolution.getVideoQualityKey() : Preferences.getVideoResolution();
    }

    void putVideoQualityOption(String key) {
        Preferences.setVideoResolution(key);
    }
}
