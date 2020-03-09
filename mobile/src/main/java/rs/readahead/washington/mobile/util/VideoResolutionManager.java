package rs.readahead.washington.mobile.util;

import com.otaliastudios.cameraview.size.AspectRatio;
import com.otaliastudios.cameraview.size.Size;
import com.otaliastudios.cameraview.size.SizeSelector;
import com.otaliastudios.cameraview.size.SizeSelectors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.sharedpref.Preferences;
import rs.readahead.washington.mobile.presentation.entity.VideoResolutionOption;


public class VideoResolutionManager {
    private static final VideoResolutionOption defaultResolution = new VideoResolutionOption("highest", SizeSelectors.biggest(), R.string.video_quality_very_high);
    private HashMap<String, VideoResolutionOption> options;

    public VideoResolutionManager(Collection<Size> sizes) {
        options = new LinkedHashMap<>();
        options.put("highest", defaultResolution);
        if (sizes.contains(new Size(1080, 1920))) {
            options.put("high", new VideoResolutionOption("high", SizeSelectors.and(SizeSelectors.aspectRatio(AspectRatio.of(9, 16), 0), SizeSelectors.maxHeight(1920), SizeSelectors.maxWidth(1080)), R.string.video_quality_high));
        }
        if (sizes.contains(new Size(720, 1280))) {
            options.put("medium", new VideoResolutionOption("medium", SizeSelectors.and(SizeSelectors.aspectRatio(AspectRatio.of(9, 16), 0), SizeSelectors.maxHeight(1280), SizeSelectors.maxWidth(720)), R.string.video_quality_medium));
        }
        if (sizes.contains(new Size(480, 640))) {
            options.put("low", new VideoResolutionOption("low", SizeSelectors.and(SizeSelectors.aspectRatio(AspectRatio.of(3, 4), 0), SizeSelectors.maxHeight(640), SizeSelectors.maxWidth(480)), R.string.video_quality_low));
        }
    }

    public SizeSelector getVideoSize() {
        VideoResolutionOption option;
        if (options.containsKey(Preferences.getVideoResolution()) && options.get(Preferences.getVideoResolution()) != null) {
            option = options.get(Preferences.getVideoResolution());
            if (option != null) {
                return option.getVideoQuality();
            } else {
                return defaultResolution.getVideoQuality();
            }
        } else {
            return defaultResolution.getVideoQuality();
        }
    }

    SizeSelector getVideoSize(String key) {
        VideoResolutionOption option;
        if (options.containsKey(key)) {
            option = options.get(key);
            if (option != null) {
                return option.getVideoQuality();
            } else {
                return defaultResolution.getVideoQuality();
            }
        } else {
            return defaultResolution.getVideoQuality();
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
