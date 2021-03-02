package com.hzontal.tella_locking_ui.ui.pin.pinview;

import android.graphics.drawable.Drawable;

/**
 * The customization options for the buttons in {@link PinLockView}
 * passed to the {@link PinLockAdapter} to decorate the individual views
 *
 */
public class CustomizationOptionsBundle {

    private int textColor;
    private int offTextColor;
    private int textSize;
    private int buttonSize;
    private Drawable buttonBackgroundDrawable;
    private Drawable deleteButtonDrawable;
    private int deleteButtonSize;
    private boolean showDeleteButton;
    private int deleteButtonPressesColor;

    public CustomizationOptionsBundle() {
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public int getOffTextColor() {
        return offTextColor;
    }

    public void seOffTextColor(int offTextColor) {
        this.offTextColor = offTextColor;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public int getButtonSize() {
        return buttonSize;
    }

    public void setButtonSize(int buttonSize) {
        this.buttonSize = buttonSize;
    }

    public Drawable getButtonBackgroundDrawable() {
        return buttonBackgroundDrawable;
    }

    public void setButtonBackgroundDrawable(Drawable buttonBackgroundDrawable) {
        this.buttonBackgroundDrawable = buttonBackgroundDrawable;
    }

    public Drawable getDeleteButtonDrawable() {
        return deleteButtonDrawable;
    }

    public void setDeleteButtonDrawable(Drawable deleteButtonDrawable) {
        this.deleteButtonDrawable = deleteButtonDrawable;
    }

    public int getDeleteButtonSize() {
        return deleteButtonSize;
    }

    public void setDeleteButtonSize(int deleteButtonSize) {
        this.deleteButtonSize = deleteButtonSize;
    }

    public boolean isShowDeleteButton() {
        return showDeleteButton;
    }

    public void setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
    }

    public int getDeleteButtonPressesColor() {
        return deleteButtonPressesColor;
    }

    public void setDeleteButtonPressesColor(int deleteButtonPressesColor) {
        this.deleteButtonPressesColor = deleteButtonPressesColor;
    }
}
