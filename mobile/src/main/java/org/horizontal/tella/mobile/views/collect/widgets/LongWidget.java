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
package org.horizontal.tella.mobile.views.collect.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.core.content.ContextCompat;

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

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.Locale;

import org.horizontal.tella.mobile.R;


/**
 * Based on ODK IntegerWidget.
 */
@SuppressLint("ViewConstructor")
public class LongWidget extends QuestionWidget {
    protected boolean readOnly;
    protected EditText answer;


    @SuppressLint("NewApi")
    public LongWidget(Context context, FormEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        answer = new EditText(context);
        answer.setTextColor(getResources().getColor(R.color.wa_white_90));
        answer.getBackground().setColorFilter(getResources().getColor(R.color.wa_white_80),
                PorterDuff.Mode.SRC_ATOP);

        answer.setHint(prompt.getShortText());
        answer.setHintTextColor(getResources().getColor(R.color.wa_white_64));
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
        answer.setFilters(new InputFilter[] {new InputFilter.LengthFilter(10)});

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
    public IAnswerData getAnswer() {
        clearFocus();
        String s = answer.getText().toString();

        if (TextUtils.isEmpty(s)) {
            return null;
        } else {
            try {
                return new LongData(Long.parseLong(s));
            } catch (Exception numberFormatException) {
                return null;
            }
        }
    }

    @Override
    public void setFocus(Context context) {

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

    private Long getLongAnswerValue() {
        IAnswerData dataHolder = formEntryPrompt.getAnswerValue();
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

        return d;
    }
}
