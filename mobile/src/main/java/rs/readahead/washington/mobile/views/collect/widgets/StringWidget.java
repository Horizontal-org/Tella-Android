/*
 * Copyright (C) 2009 University of Washington
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

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.core.content.ContextCompat;
import android.text.Selection;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TableLayout;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import rs.readahead.washington.mobile.R;
import timber.log.Timber;


/**
 * Based on ODK StringWidget.
 */
@SuppressLint("ViewConstructor")
public class StringWidget extends QuestionWidget {
    private static final String ROWS = "rows";

    protected boolean readOnly;
    protected EditText answer;


    public StringWidget(Context context, FormEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        answer = new EditText(context);
        answer.setTextColor(getResources().getColor(R.color.colorPrimaryInverse));
        answer.setId(QuestionWidget.newUniqueId());
        readOnly = prompt.isReadOnly() || readOnlyOverride;

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();

        // fix height, if rows attr is present
        String height = prompt.getQuestion().getAdditionalAttribute(null, ROWS);
        if (height != null && height.length() != 0) {
            try {
                int rows = Integer.valueOf(height);
                answer.setMinLines(rows);
                answer.setGravity(Gravity.TOP);
            } catch (Exception e) {
                Timber.e(e, this.getClass().getName());
            }
        }

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
            answer.setTextColor(ContextCompat.getColor(context, R.color.primaryTextColor));
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
    public IAnswerData getAnswer() {
        clearFocus();
        String s = answer.getText().toString();

        if (TextUtils.isEmpty(s)) {
            return null;
        } else {
            return new StringData(s);
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
}
