package rs.readahead.washington.mobile.presentation.entity;

import com.otaliastudios.cameraview.VideoQuality;

import androidx.annotation.StringRes;

public class VideoResolutionOption {
    private String alias;
    private VideoQuality videoQuality;
    private int stringResId;

    public VideoResolutionOption(String alias, VideoQuality videoQuality, @StringRes int stringResId) {
        this.alias = alias;
        this.videoQuality = videoQuality;
        this.stringResId = stringResId;
    }

    public VideoQuality getVideoQuality() {
        return videoQuality;
    }

    public String getVideoQualityKey() {
        return alias;
    }

    public int getVideoQualityStringResourceId() {
        return stringResId;
    }
}
