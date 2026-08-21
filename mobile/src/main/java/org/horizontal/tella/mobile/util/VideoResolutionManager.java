package org.horizontal.tella.mobile.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.sharedpref.Preferences;
import org.horizontal.tella.mobile.presentation.entity.VideoResolutionOption;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class VideoResolutionManager {
    private static final VideoResolutionOption defaultResolution = new VideoResolutionOption(
            "highest",
            buildHighestSelector(Arrays.asList(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD)),
            R.string.camera_video_resolution_select_highest
    );

    private final HashMap<String, VideoResolutionOption> options;

    public VideoResolutionManager(@NonNull List<Quality> supportedQualities) {
        options = new LinkedHashMap<>();
        options.put("highest", new VideoResolutionOption(
                "highest",
                buildHighestSelector(supportedQualities),
                R.string.camera_video_resolution_select_highest
        ));
        if (supportedQualities.contains(Quality.FHD)) {
            options.put("high", new VideoResolutionOption(
                    "high",
                    QualitySelector.from(Quality.FHD),
                    R.string.camera_video_resolution_select_1080p
            ));
        }
        if (supportedQualities.contains(Quality.HD)) {
            options.put("medium", new VideoResolutionOption(
                    "medium",
                    QualitySelector.from(Quality.HD),
                    R.string.camera_video_resolution_select_720p
            ));
        }
        if (supportedQualities.contains(Quality.SD)) {
            options.put("low", new VideoResolutionOption(
                    "low",
                    QualitySelector.from(Quality.SD),
                    R.string.camera_video_resolution_select_480p
            ));
        }
    }

    @NonNull
    public QualitySelector getQualitySelector() {
        return getQualitySelector(Preferences.getVideoResolution());
    }

    @NonNull
    ArrayList<VideoResolutionOption> getOptionsList() {
        return new ArrayList<>(options.values());
    }

    @NonNull
    String getVideoQualityOptionKey() {
        String saved = Preferences.getVideoResolution();
        return saved != null && options.containsKey(saved)
                ? saved
                : defaultResolution.getVideoQualityKey();
    }

    void putVideoQualityOption(@NonNull String key) {
        Preferences.setVideoResolution(key);
    }

    @NonNull
    QualitySelector getQualitySelector(@Nullable String key) {
        VideoResolutionOption option = key != null ? options.get(key) : null;
        return option != null ? option.getQualitySelector() : getDefaultQualitySelector();
    }

    @NonNull
    private QualitySelector getDefaultQualitySelector() {
        return defaultResolution.getQualitySelector();
    }

    @NonNull
    private static QualitySelector buildHighestSelector(@NonNull List<Quality> supportedQualities) {
        List<Quality> preferenceOrder = Arrays.asList(Quality.UHD, Quality.FHD, Quality.HD, Quality.SD);
        List<Quality> ordered = new ArrayList<>();
        for (Quality quality : preferenceOrder) {
            if (supportedQualities.contains(quality)) {
                ordered.add(quality);
            }
        }
        if (ordered.isEmpty()) {
            return QualitySelector.from(Quality.SD);
        }
        return QualitySelector.fromOrderedList(
                ordered,
                FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        );
    }
}
