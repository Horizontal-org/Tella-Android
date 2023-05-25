package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Selection;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.math.BigDecimal;
import java.util.Locale;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

@SuppressLint("ViewConstructor")
public class UwaziNumericWidget extends UwaziQuestionWidget {
    protected boolean readOnly;
    protected EditText answer;


    @SuppressLint("NewApi")
    public UwaziNumericWidget(Context context, UwaziEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        answer = new EditText(context);
        answer.setTextColor(getResources().getColor(R.color.wa_white_80));
        answer.getBackground().setColorFilter(getResources().getColor(R.color.wa_white_80),
                PorterDuff.Mode.SRC_ATOP);

        answer.setHint(prompt.getQuestion());
        answer.setId(QuestionWidget.newUniqueId());
        readOnly = prompt.isReadOnly() || readOnlyOverride;

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5); // todo: make this the same for all "edit" inputs
        answer.setLayoutParams(params);

        answer.setHorizontallyScrolling(true);
        answer.setSingleLine(true);

        answer.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        //answer.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        answer.setKeyListener(new DigitsKeyListener(true, false));

        // 10 digits max - long
        answer.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});

        Long l = getLongAnswerValue();
        if (l != null) {
            answer.setText(String.format(Locale.US, "%d", l));
            Selection.setSelection(answer.getText(), answer.getText().toString().length());
        }

        if (readOnly) {
            answer.setBackground(null);
            answer.setEnabled(false);
            answer.setTextColor(ContextCompat.getColor(context, R.color.gray));
            answer.setFocusable(false);
        }

        addAnswerView(answer);
    }

    @Override
    public void clearAnswer() {
        answer.setText(null);
    }

    @Override
    public UwaziValue getAnswer() {
        clearFocus();
        String s = answer.getText().toString();

        if (TextUtils.isEmpty(s)) {
            return null;
        } else {
            try {
                return new UwaziValue(Long.parseLong(s));
            } catch (Exception numberFormatException) {
                return null;
            }
        }
    }

    @Override
    public void setFocus(Context context) {
        // Put focus on text input field and display soft keyboard if appropriate.
        answer.requestFocus();

        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (!readOnly) {
            if (inputManager != null) {
                inputManager.showSoftInput(answer, 0);
            }
        } else {
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(answer.getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        answer.cancelLongPress();
    }

    @SuppressLint("SetTextI18n")
    public String setBinaryData(@NonNull Object data) {
        BigDecimal value = new BigDecimal(data.toString());
        long lValue = value.longValue();
        answer.setText(Long.toString(lValue));
        return Long.toString(lValue);
    }

    private Long getLongAnswerValue() {
        if (formEntryPrompt.getAnswerText() != null) {
            String numeric = formEntryPrompt.getAnswerText();
            return Long.parseLong(numeric);
        } else {
            return null;
        }
        /*IAnswerData dataHolder = formEntryPrompt.getAnswerValue();
        Long d = null;

        if (dataHolder != null) {
            try {
                Object dataValue = dataHolder.getValue();

                if (dataValue instanceof Double) {
                    d = ((Double) dataValue).longValue();
                } else {
                    d = (Long) dataValue;
                }
            } catch (Exception ignored) {}
        }

        return d;*/
    }
}
