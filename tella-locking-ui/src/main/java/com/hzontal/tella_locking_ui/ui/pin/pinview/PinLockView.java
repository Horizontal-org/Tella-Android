package com.hzontal.tella_locking_ui.ui.pin.pinview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.hzontal.tella_locking_ui.R;

/**
 * Represents a numeric lock view which can used to taken numbers as input.
 * The length of the input can be customized using {@link PinLockView#setMinPinLength(int)} (int)}, the default value being 6
 * <p/>
 * It can also be used as dial pad for taking number inputs.
 */
public class PinLockView extends RecyclerView {

    public static final int DEFAULT_PIN_LENGTH = 6;
    private static final int[] DEFAULT_KEY_SET = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0};

    private String mPin = "";
    private int mPinLength;
    private int mHorizontalSpacing, mVerticalSpacing;
    private int mTextColor, mDeleteButtonPressedColor, mOffTextColor;
    private int mTextSize, mButtonSize, mDeleteButtonSize;
    private Drawable mButtonBackgroundDrawable;
    private Drawable mDeleteButtonDrawable;
    private boolean mShowDeleteButton;

    private PinLockAdapter mAdapter;
    private PinLockListener mPinLockListener;
    private CustomizationOptionsBundle mCustomizationOptionsBundle;
    private int[] mCustomKeySet;

    private final PinLockAdapter.OnNumberClickListener mOnNumberClickListener
            = new PinLockAdapter.OnNumberClickListener() {
        @Override
        public void onNumberClicked(int keyValue) {

                mPin = mPin.concat(String.valueOf(keyValue));

                if (mPin.length() == 1) {
                    mAdapter.setPinLength(mPin.length());
                    mAdapter.notifyItemChanged(mAdapter.getItemCount() - 3);
                }
                if (mPin.length() == 6){
                    mAdapter.setPinLength(mPin.length());
                    mAdapter.notifyItemChanged(mAdapter.getItemCount() - 1);
                }

                if (mPinLockListener != null) {
                    mPinLockListener.onPinChange(mPin.length(), mPin);
                }
            if (!isShowDeleteButton()) {
                resetPinLockView();
                mPin = mPin.concat(String.valueOf(keyValue));

                if (mPinLockListener != null) {
                    mPinLockListener.onPinChange(mPin.length(), mPin);
                }

            }
        }
    };

    private final PinLockAdapter.OnDeleteClickListener mOnDeleteClickListener
            = new PinLockAdapter.OnDeleteClickListener() {
        @Override
        public void onDeleteClicked() {
            if (mPin.length() > 0) {
                mPin = mPin.substring(0, mPin.length() - 1);

                if (mPin.length() == 0) {
                    mAdapter.setPinLength(mPin.length());
                    mAdapter.notifyItemChanged(mAdapter.getItemCount() - 3);
                }
                if (mPin.length() < 6){
                    mAdapter.setPinLength(mPin.length());
                    mAdapter.notifyItemChanged(mAdapter.getItemCount() - 1);
                }

                if (mPinLockListener != null) {
                    if (mPin.length() == 0) {
                        mPinLockListener.onEmpty();
                        clearInternalPin();
                    } else {
                        mPinLockListener.onPinChange(mPin.length(), mPin);
                    }
                }
            } else {
                if (mPinLockListener != null) {
                    mPinLockListener.onEmpty();
                }
            }
        }

        @Override
        public void onDeleteLongClicked() {
            resetPinLockView();
            if (mPinLockListener != null) {
                mPinLockListener.onEmpty();
            }
        }
    };
    private final PinLockAdapter.OnConfirmClickListener mOnConfirmClickListener = () -> {
        if (mPin.length() >= DEFAULT_PIN_LENGTH){
            if (mPinLockListener != null) {
                mPinLockListener.onPinConfirmation(mPin);
            }
        }
    };

    public PinLockView(Context context) {
        super(context);
        init(null, 0);
    }

    public PinLockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public PinLockView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attributeSet, int defStyle) {

        TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.PinLockView);

        try {
            mPinLength = typedArray.getInt(R.styleable.PinLockView_pinLength, DEFAULT_PIN_LENGTH);
            mHorizontalSpacing = (int) typedArray.getDimension(R.styleable.PinLockView_keypadHorizontalSpacing, ResourceUtils.getDimensionInPx(getContext(), R.dimen.default_horizontal_spacing));
            mVerticalSpacing = (int) typedArray.getDimension(R.styleable.PinLockView_keypadVerticalSpacing, ResourceUtils.getDimensionInPx(getContext(), R.dimen.default_vertical_spacing));
            mTextColor = typedArray.getColor(R.styleable.PinLockView_keypadTextColor, ResourceUtils.getColor(getContext(), R.color.wa_white));
            mOffTextColor = typedArray.getColor(R.styleable.PinLockView_keypadOffTextColor, ResourceUtils.getColor(getContext(), R.color.wa_white_40));
            mTextSize = (int) typedArray.getDimension(R.styleable.PinLockView_keypadTextSize, ResourceUtils.getDimensionInPx(getContext(), R.dimen.default_text_size));
            mButtonSize = (int) typedArray.getDimension(R.styleable.PinLockView_keypadButtonSize, ResourceUtils.getDimensionInPx(getContext(), R.dimen.default_button_size));
            mDeleteButtonSize = (int) typedArray.getDimension(R.styleable.PinLockView_keypadDeleteButtonSize, ResourceUtils.getDimensionInPx(getContext(), R.dimen.default_delete_button_size));
            mButtonBackgroundDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadButtonBackgroundDrawable);
            mDeleteButtonDrawable = typedArray.getDrawable(R.styleable.PinLockView_keypadDeleteButtonDrawable);
            mShowDeleteButton = typedArray.getBoolean(R.styleable.PinLockView_keypadShowDeleteButton, true);
            mDeleteButtonPressedColor = typedArray.getColor(R.styleable.PinLockView_keypadDeleteButtonPressedColor, ResourceUtils.getColor(getContext(), R.color.wa_orange));
        } finally {
            typedArray.recycle();
        }

        mCustomizationOptionsBundle = new CustomizationOptionsBundle();
        mCustomizationOptionsBundle.setTextColor(mTextColor);
        mCustomizationOptionsBundle.seOffTextColor(mOffTextColor);
        mCustomizationOptionsBundle.setTextSize(mTextSize);
        mCustomizationOptionsBundle.setButtonSize(mButtonSize);
        mCustomizationOptionsBundle.setButtonBackgroundDrawable(mButtonBackgroundDrawable);
        mCustomizationOptionsBundle.setDeleteButtonDrawable(mDeleteButtonDrawable);
        mCustomizationOptionsBundle.setDeleteButtonSize(mDeleteButtonSize);
        mCustomizationOptionsBundle.setShowDeleteButton(mShowDeleteButton);
        mCustomizationOptionsBundle.setDeleteButtonPressesColor(mDeleteButtonPressedColor);

        initView();
    }

    private void initView() {
        setLayoutManager(new LTRGridLayoutManager(getContext(), 3));

        mAdapter = new PinLockAdapter(getContext());
        mAdapter.setOnItemClickListener(mOnNumberClickListener);
        mAdapter.setOnDeleteClickListener(mOnDeleteClickListener);
        mAdapter.setOnConfirmClickListener(mOnConfirmClickListener);
        mAdapter.setCustomizationOptions(mCustomizationOptionsBundle);
        setAdapter(mAdapter);

        addItemDecoration(new ItemSpaceDecoration(mHorizontalSpacing, mVerticalSpacing, 3, false));
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    /**
     * Sets a {@link PinLockListener} to the to listen to pin update events
     *
     * @param pinLockListener the listener
     */
    public void setPinLockListener(PinLockListener pinLockListener) {
        this.mPinLockListener = pinLockListener;
    }

    /**
     * Get the length of the current pin length
     *
     * @return the length of the pin
     */
    public int getMinPinLength() {
        return mPinLength;
    }

    /**
     * Sets the pin length dynamically
     *
     * @param pinLength the pin length
     */
    public void setMinPinLength(int pinLength) {
        this.mPinLength = pinLength;

    }

    /**
     * Get the text color in the buttons
     *
     * @return the text color
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * Set the text color of the buttons dynamically
     *
     * @param textColor the text color
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mCustomizationOptionsBundle.setTextColor(textColor);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the size of the text in the buttons
     *
     * @return the size of the text in pixels
     */
    public int getTextSize() {
        return mTextSize;
    }

    /**
     * Set the size of text in pixels
     *
     * @param textSize the text size in pixels
     */
    public void setTextSize(int textSize) {
        this.mTextSize = textSize;
        mCustomizationOptionsBundle.setTextSize(textSize);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the size of the pin buttons
     *
     * @return the size of the button in pixels
     */
    public int getButtonSize() {
        return mButtonSize;
    }

    /**
     * Set the size of the pin buttons dynamically
     *
     * @param buttonSize the button size
     */
    public void setButtonSize(int buttonSize) {
        this.mButtonSize = buttonSize;
        mCustomizationOptionsBundle.setButtonSize(buttonSize);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the current background drawable of the buttons, can be null
     *
     * @return the background drawable
     */
    public Drawable getButtonBackgroundDrawable() {
        return mButtonBackgroundDrawable;
    }

    /**
     * Set the background drawable of the buttons dynamically
     *
     * @param buttonBackgroundDrawable the background drawable
     */
    public void setButtonBackgroundDrawable(Drawable buttonBackgroundDrawable) {
        this.mButtonBackgroundDrawable = buttonBackgroundDrawable;
        mCustomizationOptionsBundle.setButtonBackgroundDrawable(buttonBackgroundDrawable);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the drawable of the delete button
     *
     * @return the delete button drawable
     */
    public Drawable getDeleteButtonDrawable() {
        return mDeleteButtonDrawable;
    }

    /**
     * Set the drawable of the delete button dynamically
     *
     * @param deleteBackgroundDrawable the delete button drawable
     */
    public void setDeleteButtonDrawable(Drawable deleteBackgroundDrawable) {
        this.mDeleteButtonDrawable = deleteBackgroundDrawable;
        mCustomizationOptionsBundle.setDeleteButtonDrawable(deleteBackgroundDrawable);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the delete button size in pixels
     *
     * @return size in pixels
     */
    public int getDeleteButtonSize() {
        return mDeleteButtonSize;
    }

    /**
     * Set the size of the delete button in pixels
     *
     * @param deleteButtonSize size in pixels
     */
    public void setDeleteButtonSize(int deleteButtonSize) {
        this.mDeleteButtonSize = deleteButtonSize;
        mCustomizationOptionsBundle.setDeleteButtonSize(deleteButtonSize);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Is the delete button shown
     *
     * @return returns true if shown, false otherwise
     */
    public boolean isShowDeleteButton() {
        return mShowDeleteButton;
    }

    /**
     * Dynamically set if the delete button should be shown
     *
     * @param showDeleteButton true if the delete button should be shown, false otherwise
     */
    public void setShowDeleteButton(boolean showDeleteButton) {
        this.mShowDeleteButton = showDeleteButton;
        mCustomizationOptionsBundle.setShowDeleteButton(showDeleteButton);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Get the delete button pressed/focused state color
     *
     * @return color of the button
     */
    public int getDeleteButtonPressedColor() {
        return mDeleteButtonPressedColor;
    }

    /**
     * Set the pressed/focused state color of the delete button
     *
     * @param deleteButtonPressedColor the color of the delete button
     */
    public void setDeleteButtonPressedColor(int deleteButtonPressedColor) {
        this.mDeleteButtonPressedColor = deleteButtonPressedColor;
        mCustomizationOptionsBundle.setDeleteButtonPressesColor(deleteButtonPressedColor);
        mAdapter.notifyDataSetChanged();
    }

    public int[] getCustomKeySet() {
        return mCustomKeySet;
    }

    public void setCustomKeySet(int[] customKeySet) {
        this.mCustomKeySet = customKeySet;

        if (mAdapter != null) {
            mAdapter.setKeyValues(customKeySet);
        }
    }

    public void enableLayoutShuffling() {
        this.mCustomKeySet = ShuffleArrayUtils.shuffle(DEFAULT_KEY_SET);

        if (mAdapter != null) {
            mAdapter.setKeyValues(mCustomKeySet);
        }
    }

    private void clearInternalPin() {
        mPin = "";
    }

    /**
     * Resets the {@link PinLockView}, clearing the entered pin
     */
    public void resetPinLockView() {

        clearInternalPin();

        mAdapter.setPinLength(mPin.length());
        mAdapter.notifyItemChanged(mAdapter.getItemCount() - 1);

    }

}
