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
    private static final VideoResolutionOption defaultResolution = new VideoResolutionOption("highest", SizeSelectors.biggest(), R.string.camera_video_resolution_select_highest);
    private HashMap<String, VideoResolutionOption> options;

    public VideoResolutionManager(Collection<Size> sizes) {
        options = new LinkedHashMap<>();
        options.put("highest", defaultResolution);
        if (sizes.contains(new Size(1080, 1920))) {
            addVideoOption(1080, 1920, 9, 16, "high", R.string.camera_video_resolution_select_1080p);
        }
        if (sizes.contains(new Size(720, 1280))) {
            addVideoOption(720, 1280, 9, 16, "medium", R.string.camera_video_resolution_select_720p);
        }
        if (sizes.contains(new Size(480, 640))) {
            addVideoOption(480, 640, 3, 4, "low", R.string.camera_video_resolution_select_480p);
        }
    }

    public SizeSelector getVideoSize() {
        return getVideoSize(Preferences.getVideoResolution());
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

    SizeSelector getVideoSize(String key) {
        VideoResolutionOption option = options.get(key);
        return option != null ? option.getVideoQuality() : getDefaultVideoResolution();
    }

    private SizeSelector getDefaultVideoResolution() {
        return defaultResolution.getVideoQuality();
    }

    private void addVideoOption(int width, int height, int aspectX, int aspectY, String resolutionName, int resOptionStringId) {
        options.put(resolutionName, new VideoResolutionOption(resolutionName, SizeSelectors.and(SizeSelectors.aspectRatio(AspectRatio.of(aspectX, aspectY), 0), SizeSelectors.maxHeight(height), SizeSelectors.maxWidth(width)), resOptionStringId));
    }
}
