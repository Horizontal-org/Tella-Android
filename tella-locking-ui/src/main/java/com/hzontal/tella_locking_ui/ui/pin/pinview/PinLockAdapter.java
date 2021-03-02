package com.hzontal.tella_locking_ui.ui.pin.pinview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.hzontal.tella_locking_ui.R;

import org.jetbrains.annotations.NotNull;

import static com.hzontal.tella_locking_ui.ui.pin.pinview.PinLockView.DEFAULT_PIN_LENGTH;


public class PinLockAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NUMBER = 0;
    private static final int VIEW_TYPE_DELETE = 1;
    private static final int VIEW_TYPE_OK = 2;
    private static final int KEYBOARD_KEYS_LENGTH = 12;

    private Context mContext;
    private CustomizationOptionsBundle mCustomizationOptionsBundle;
    private OnNumberClickListener mOnNumberClickListener;
    private OnDeleteClickListener mOnDeleteClickListener;
    private OnConfirmClickListener mOnConfirmClickListener;
    private int mPinLength;

    private int[] mKeyValues;

    public PinLockAdapter(Context context) {
        this.mContext = context;
        this.mKeyValues = getAdjustKeyValues(new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 0});
    }

    @Override
    public RecyclerView.@NotNull ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == VIEW_TYPE_NUMBER) {
            View view = inflater.inflate(R.layout.layout_number_item, parent, false);
            viewHolder = new NumberViewHolder(view);
        } else if (viewType == VIEW_TYPE_DELETE) {
            View view = inflater.inflate(R.layout.layout_delete_item, parent, false);
            viewHolder = new DeleteViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.layout_number_item, parent, false);
            viewHolder = new ConfirmViewHolder(view);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_NUMBER) {
            NumberViewHolder vh1 = (NumberViewHolder) holder;
            configureNumberButtonHolder(vh1, position);
        } else if (holder.getItemViewType() == VIEW_TYPE_DELETE) {
            DeleteViewHolder vh2 = (DeleteViewHolder) holder;
            configureDeleteButtonHolder(vh2);
        } else if (holder.getItemViewType() == VIEW_TYPE_OK) {
            ConfirmViewHolder vh3 = (ConfirmViewHolder) holder;
            configureOkButtonHolder(vh3);
        }
    }

    private void configureOkButtonHolder(ConfirmViewHolder holder) {
        if (holder != null) {
            holder.mOkButton.setText(mContext.getString(R.string.ok));
            if (mCustomizationOptionsBundle == null)
                return;

            holder.mOkButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    mCustomizationOptionsBundle.getTextSize());
            if (mPinLength >= DEFAULT_PIN_LENGTH) {
                holder.mOkButton.setTextColor(mCustomizationOptionsBundle.getTextColor());
            } else {
                holder.mOkButton.setTextColor(mCustomizationOptionsBundle.getOffTextColor());
            }
        }
    }

    private void configureNumberButtonHolder(NumberViewHolder holder, int position) {
        if (holder != null) {
            holder.mNumberButton.setText(String.valueOf(mKeyValues[position]));
            holder.mNumberButton.setVisibility(View.VISIBLE);
            holder.mNumberButton.setTag(mKeyValues[position]);

            if (mCustomizationOptionsBundle != null) {
                holder.mNumberButton.setTextColor(mCustomizationOptionsBundle.getTextColor());
                if (mCustomizationOptionsBundle.getButtonBackgroundDrawable() != null) {
                    holder.mNumberButton.setBackground(
                            mCustomizationOptionsBundle.getButtonBackgroundDrawable());
                }
                holder.mNumberButton.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        mCustomizationOptionsBundle.getTextSize());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        mCustomizationOptionsBundle.getButtonSize(),
                        mCustomizationOptionsBundle.getButtonSize());
                holder.mNumberButton.setLayoutParams(params);
            }
        }
    }

    private void configureDeleteButtonHolder(DeleteViewHolder holder) {
        if (holder != null) {
            if (mCustomizationOptionsBundle.isShowDeleteButton() && mPinLength > 0) {
                holder.mButtonImage.setVisibility(View.VISIBLE);
                if (mCustomizationOptionsBundle.getDeleteButtonDrawable() != null) {
                    holder.mButtonImage.setImageDrawable(mCustomizationOptionsBundle.getDeleteButtonDrawable());
                }
                holder.mButtonImage.setColorFilter(mCustomizationOptionsBundle.getTextColor(),
                        PorterDuff.Mode.SRC_ATOP);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        mCustomizationOptionsBundle.getDeleteButtonSize(),
                        mCustomizationOptionsBundle.getDeleteButtonSize());
                holder.mButtonImage.setLayoutParams(params);
            } else {
                holder.mButtonImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return KEYBOARD_KEYS_LENGTH;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return VIEW_TYPE_OK;
        } else if (position == getItemCount() - 3) {
            return VIEW_TYPE_DELETE;
        }
        return VIEW_TYPE_NUMBER;
    }

    public int getPinLength() {
        return mPinLength;
    }

    public void setPinLength(int pinLength) {
        this.mPinLength = pinLength;
    }

    public int[] getKeyValues() {
        return mKeyValues;
    }

    public void setKeyValues(int[] keyValues) {
        this.mKeyValues = getAdjustKeyValues(keyValues);
        notifyDataSetChanged();
    }

    private int[] getAdjustKeyValues(int[] keyValues) {
        int[] adjustedKeyValues = new int[keyValues.length + 1];
        for (int i = 0; i < keyValues.length; i++) {
            if (i < 9) {
                adjustedKeyValues[i] = keyValues[i];
            } else {
                adjustedKeyValues[i] = -1;
                adjustedKeyValues[i + 1] = keyValues[i];
            }
        }
        return adjustedKeyValues;
    }

    public OnNumberClickListener getOnItemClickListener() {
        return mOnNumberClickListener;
    }

    public void setOnItemClickListener(OnNumberClickListener onNumberClickListener) {
        this.mOnNumberClickListener = onNumberClickListener;
    }

    public OnDeleteClickListener getOnDeleteClickListener() {
        return mOnDeleteClickListener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener onDeleteClickListener) {
        this.mOnDeleteClickListener = onDeleteClickListener;
    }

    public CustomizationOptionsBundle getCustomizationOptions() {
        return mCustomizationOptionsBundle;
    }

    public void setCustomizationOptions(CustomizationOptionsBundle customizationOptionsBundle) {
        this.mCustomizationOptionsBundle = customizationOptionsBundle;
    }

    public void setOnConfirmClickListener(OnConfirmClickListener mOnConfirmClickListener) {
        this.mOnConfirmClickListener = mOnConfirmClickListener;
    }

    public interface OnNumberClickListener {
        void onNumberClicked(int keyValue);
    }

    public interface OnDeleteClickListener {
        void onDeleteClicked();

        void onDeleteLongClicked();
    }

    public interface OnConfirmClickListener {
        void onConfirmClicked();
    }

    public class NumberViewHolder extends RecyclerView.ViewHolder {
        Button mNumberButton;

        public NumberViewHolder(final View itemView) {
            super(itemView);
            mNumberButton = (Button) itemView.findViewById(R.id.button);
            mNumberButton.setOnClickListener(v -> {
                if (mOnNumberClickListener != null) {
                    mOnNumberClickListener.onNumberClicked((Integer) v.getTag());
                }
            });
        }
    }

    public class ConfirmViewHolder extends RecyclerView.ViewHolder {
        Button mOkButton;

        public ConfirmViewHolder(final View itemView) {
            super(itemView);
            mOkButton = (Button) itemView.findViewById(R.id.button);
            mOkButton.setOnClickListener(v -> {
                if (mOnConfirmClickListener != null) {
                    mOnConfirmClickListener.onConfirmClicked();
                }
            });
        }
    }

    public class DeleteViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mDeleteButton;
        ImageView mButtonImage;

        @SuppressLint("ClickableViewAccessibility")
        public DeleteViewHolder(final View itemView) {
            super(itemView);
            mDeleteButton = (LinearLayout) itemView.findViewById(R.id.button);
            mButtonImage = (ImageView) itemView.findViewById(R.id.buttonImage);

            if (mCustomizationOptionsBundle.isShowDeleteButton() && mPinLength > 0) {
                mDeleteButton.setOnClickListener(v -> {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteClicked();
                    }
                });

                mDeleteButton.setOnLongClickListener(v -> {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteLongClicked();
                    }
                    return true;
                });

                mDeleteButton.setOnTouchListener(new View.OnTouchListener() {
                    private Rect rect;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            mButtonImage.setColorFilter(mCustomizationOptionsBundle
                                    .getDeleteButtonPressesColor());
                            rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        }
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            mButtonImage.clearColorFilter();
                        }
                        if (event.getAction() == MotionEvent.ACTION_MOVE) {
                            if (!rect.contains(v.getLeft() + (int) event.getX(),
                                    v.getTop() + (int) event.getY())) {
                                mButtonImage.clearColorFilter();
                            }
                        }
                        return false;
                    }
                });
            }
        }
    }
}
