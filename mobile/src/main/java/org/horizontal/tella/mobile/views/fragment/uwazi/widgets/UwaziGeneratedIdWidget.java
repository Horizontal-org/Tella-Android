package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.Selection;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.util.UUID;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValue;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziGeneratedIdWidget extends UwaziQuestionWidget {

    protected boolean readOnly;
    protected EditText answer;

    @SuppressLint("NewApi")
    public UwaziGeneratedIdWidget(Context context, UwaziEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        answer = new EditText(context);
        answer.setTextColor(getResources().getColor(R.color.wa_white_80));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            answer.setBackgroundTintList(context.getColorStateList(R.color.dark_purple));
        }
        readOnly = prompt.isReadOnly() || readOnlyOverride;

        // needed to make long read only text scroll
        answer.setHorizontallyScrolling(false);
        answer.setSingleLine(false);
        answer.setTextAppearance(R.style.Tella_Main_White_Text_Meduim);

        String s = prompt.getAnswerText();
        if (s != null) {
            answer.setText(s);
            Selection.setSelection(answer.getText(), answer.getText().toString().length());
        } else {
            generateId();
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

    private void generateId() {
        UUID uuid = UUID.randomUUID();
        answer.setText(uuid.toString());
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

