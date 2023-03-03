package rs.readahead.washington.mobile.presentation.entity;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;


public class CamouflageOption {
    public String alias;
    public int drawableResId;
    public int stringResId;

    public CamouflageOption(String alias, @DrawableRes int drawableResId,@StringRes int stringResId) {
        this.alias = alias;
        this.drawableResId = drawableResId;
        this.stringResId = stringResId;
    }
}
