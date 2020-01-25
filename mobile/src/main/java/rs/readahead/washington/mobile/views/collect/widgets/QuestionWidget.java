/*
 * Copyright (C) 2011 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package rs.readahead.washington.mobile.views.collect.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.odk.FormController;
import rs.readahead.washington.mobile.odk.exception.JavaRosaException;
import rs.readahead.washington.mobile.util.StringUtils;
import timber.log.Timber;


/**
 * Based on ODK QuestionWidget. No audio support.
 */
public abstract class QuestionWidget extends RelativeLayout {
    @SuppressWarnings("unused")
    private static final String t = "QuestionWidget";
    private static int idGenerator = 1211322;

    /**
     * Generate a unique ID to keep Android UI happy when the screen orientation
     * changes.
     */
    public static int newUniqueId() {
        return ++idGenerator;
    }

    protected FormEntryPrompt formEntryPrompt;

    private LinearLayout questionHeader;
    private TextView helpTextView;
    private TextView constraintValidationView;

    public QuestionWidget(Context context, @NonNull FormEntryPrompt formEntryPrompt) {
        super(context);

        this.formEntryPrompt = formEntryPrompt;

        inflate(context, R.layout.question_widget, this);

        // set my layout params
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        setLayoutParams(layoutParams);
        //setBackground(getContext().getResources().getDrawable(R.drawable.shadow_border));
        int h = getResources().getDimensionPixelSize(R.dimen.collect_form_padding_horizontal);
        int v = getResources().getDimensionPixelSize(R.dimen.collect_form_padding_vertical);
        setPadding(h, v, h, v);

        questionHeader = findViewById(R.id.questionHeader);

        TextView questionTitleView = findViewById(R.id.questionTitle);
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(formEntryPrompt.getLongText());
        int start = builder.length();
        if (formEntryPrompt.isRequired()) {
            builder.append(" *");
            int end = builder.length();
            builder.setSpan(new ForegroundColorSpan(Color.RED), start, end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        questionTitleView.setText(builder);

        helpTextView = findViewById(R.id.questionHelpText);
        if (!TextUtils.isEmpty(formEntryPrompt.getHelpText())) {
            helpTextView.setText(formEntryPrompt.getHelpText());
        } else {
            helpTextView.setVisibility(GONE);
        }

        constraintValidationView = findViewById(R.id.constraintValidationView);
    }

    public TextView getHelpTextView() {
        return helpTextView;
    }

    public void setConstraintValidationText(@Nullable String text) {
        if (StringUtils.isTextEqual(text, constraintValidationView.getText().toString())) {
            return;
        }

        if (TextUtils.isEmpty(text)) {
            constraintValidationView.setText(null);
            constraintValidationView.setVisibility(GONE);
        } else {
            constraintValidationView.setText(text);
            constraintValidationView.setVisibility(VISIBLE);
        }
    }

    public FormEntryPrompt getPrompt() {
        return formEntryPrompt;
    }

    @SuppressWarnings("UnusedReturnValue")
    public String setBinaryData(@NonNull Object data) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public String getBinaryName() {
        throw new UnsupportedOperationException();
    }

    // http://code.google.com/p/android/issues/detail?id=8488
    private void recycleDrawablesRecursive(ViewGroup viewGroup, List<ImageView> images) {
        int childCount = viewGroup.getChildCount();
        for (int index = 0; index < childCount; index++) {
            View child = viewGroup.getChildAt(index);
            if (child instanceof ImageView) {
                images.add((ImageView) child);
            } else if (child instanceof ViewGroup) {
                recycleDrawablesRecursive((ViewGroup) child, images);
            }
        }
        viewGroup.destroyDrawingCache();
    }

    // http://code.google.com/p/android/issues/detail?id=8488
    public void recycleDrawables() {
        List<ImageView> images = new ArrayList<ImageView>();
        // collect all the image views
        recycleDrawablesRecursive(this, images);
        for (ImageView imageView : images) {
            imageView.destroyDrawingCache();
            Drawable d = imageView.getDrawable();
            if (d instanceof BitmapDrawable) {
                imageView.setImageDrawable(null);
                BitmapDrawable bd = (BitmapDrawable) d;
                Bitmap bmp = bd.getBitmap();
                if (bmp != null) {
                    bmp.recycle();
                }
            }
        }
    }

    // Abstract methods
    public abstract IAnswerData getAnswer();

    public abstract void clearAnswer();

    public abstract void setFocus(Context context);

    /**
     * Override this to implement fling gesture suppression (e.g. for embedded WebView treatments).
     *
     * @return true if the fling gesture should be suppressed
     */
    public boolean suppressFlingGesture(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    /**
     * Default place to put the answer
     * (below the help text or question text if there is no help text)
     * If you have many elements, use this first
     * and use the standard addView(view, params) to place the rest
     */
    protected void addAnswerView(View v) {
        if (v == null) {
            Timber.e("cannot add a null view as an answerView");
            return;
        }

        // default place to add answer
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

        /*if (helpTextView.getVisibility() == View.VISIBLE) {
            params.addRule(RelativeLayout.BELOW, helpTextView.getId());
        } else {
            params.addRule(RelativeLayout.BELOW, questionTitleView.getId());
        }*/

        params.addRule(RelativeLayout.BELOW, questionHeader.getId());

        addView(v, params);
    }

    /**
     * Every subclassed widget should override this, adding any views they may contain, and calling
     * super.cancelLongPress()
     */
    public void cancelLongPress() {
        super.cancelLongPress();

        if (helpTextView != null) {
            helpTextView.cancelLongPress();
        }
    }

    /**
     * It's needed only for external choices. Everything works well and
     * out of the box when we use internal choices instead
     */
    protected void clearNextLevelsOfCascadingSelect() {
        FormController formController = FormController.getActive(); // todo: check this out!
        if (formController.currentCaptionPromptIsQuestion()) {
            try {
                FormIndex startFormIndex = formController.getQuestionPrompt().getIndex();
                formController.stepToNextScreenEvent();
                while (formController.currentCaptionPromptIsQuestion()
                        && formController.getQuestionPrompt().getFormElement().getAdditionalAttribute(null, "query") != null) {
                    formController.saveAnswer(formController.getQuestionPrompt().getIndex(), null);
                    formController.stepToNextScreenEvent();
                }
                formController.jumpToIndex(startFormIndex);
            } catch (JavaRosaException e) {
                Timber.e(e, null);
            }
        }
    }

    protected ImageButton addButton(int drawableResource) {
        ImageButton button = new ImageButton(getContext());
        button.setBackground(getResources().getDrawable(R.drawable.collect_widget_menu_background));
        button.setImageDrawable(getResources().getDrawable(drawableResource));

        int padding = getResources().getDimensionPixelSize(R.dimen.collect_widget_icon_padding);
        button.setPadding(padding, 0, padding, 0);

        LinearLayout iconsLayout = findViewById(R.id.iconsLayout);
        iconsLayout.addView(button);

        return button;
    }

    protected View getQuestionRootView() {
        return questionHeader;
    }
}
