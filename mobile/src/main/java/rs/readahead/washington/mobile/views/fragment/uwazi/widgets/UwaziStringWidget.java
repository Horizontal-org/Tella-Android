package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Selection;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.entity.uwazi.answer.IUwaziAnswer;
import rs.readahead.washington.mobile.data.entity.uwazi.answer.UwaziString;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

@SuppressLint("ViewConstructor")
public class UwaziStringWidget extends UwaziQuestionWidget {
    private static final String ROWS = "rows";

    protected boolean readOnly;
    protected EditText answer;

    @SuppressLint("NewApi")
    public UwaziStringWidget(Context context, UwaziEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        answer = new EditText(context);
        answer.setTextColor(getResources().getColor(R.color.wa_white_80));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            answer.setBackgroundTintList(context.getColorStateList(R.color.dialog_white_tint));
        }
        answer.setId(QuestionWidget.newUniqueId());
        readOnly = prompt.isReadOnly() || readOnlyOverride;

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();

        // fix height, if rows attr is present
        /*String height = prompt.getQuestion().getAdditionalAttribute(null, ROWS);
        if (height != null && height.length() != 0) {
            try {
                int rows = Integer.valueOf(height);
                answer.setMinLines(rows);
                answer.setGravity(Gravity.TOP);
            } catch (Exception e) {
                Timber.e(e, this.getClass().getName());
            }
        }*/

        params.setMargins(7, 5, 7, 5);
        answer.setLayoutParams(params);

        // capitalize the first letter of the sentence
        //answer.setKeyListener(new TextKeyListener(Capitalize.SENTENCES, false));

        // needed to make long read only text scroll
        answer.setHorizontallyScrolling(false);
        answer.setSingleLine(false);

        String s = prompt.getAnswerText();
        if (s != null) {
            answer.setText(s);
            Selection.setSelection(answer.getText(), answer.getText().toString().length());
        }

        if (readOnly) {
            answer.setBackground(null);
            answer.setEnabled(false);
            answer.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            answer.setFocusable(false);
            answer.setVisibility(GONE);
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
            return new UwaziValue(s);
        }
    }

    @Override
    public void setFocus(Context context) {
        // Put focus on text input field and display soft keyboard if appropriate.
        answer.requestFocus();

        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager == null) {
            return;
        }

        if (!readOnly) {
            inputManager.showSoftInput(answer, 0);
        } else {
            inputManager.hideSoftInputFromWindow(answer.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    public String setBinaryData(@NonNull Object data) {
        answer.setText(data.toString());
        return data.toString();
    }
}

