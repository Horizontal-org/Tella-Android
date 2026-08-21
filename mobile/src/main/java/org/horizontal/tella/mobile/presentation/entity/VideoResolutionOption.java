package org.horizontal.tella.mobile.presentation.entity;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.camera.video.QualitySelector;

public class VideoResolutionOption {
    private final String alias;
    private final QualitySelector qualitySelector;
    private final int stringResId;

    public VideoResolutionOption(@NonNull String alias, @NonNull QualitySelector qualitySelector, @StringRes int stringResId) {
        this.alias = alias;
        this.qualitySelector = qualitySelector;
        this.stringResId = stringResId;
    }

    @NonNull
    public QualitySelector getQualitySelector() {
        return qualitySelector;
    }

    @NonNull
    public String getVideoQualityKey() {
        return alias;
    }

    @StringRes
    public int getVideoQualityStringResourceId() {
        return stringResId;
    }
}
