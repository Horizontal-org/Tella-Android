package rs.readahead.washington.mobile.presentation.entity;

import com.otaliastudios.cameraview.size.SizeSelector;

import androidx.annotation.StringRes;

public class VideoResolutionOption {
    private String alias;
    private SizeSelector videoSize;
    private int stringResId;

    public VideoResolutionOption(String alias, SizeSelector videoSize, @StringRes int stringResId) {
        this.alias = alias;
        this.videoSize = videoSize;
        this.stringResId = stringResId;
    }

    public SizeSelector getVideoQuality() {
        return videoSize;
    }

    public String getVideoQualityKey() {
        return alias;
    }

    public int getVideoQualityStringResourceId() {
        return stringResId;
    }
}
