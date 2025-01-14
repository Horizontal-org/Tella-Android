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

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.KeyEvent;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

/**
 * Based on ODK StringWidget.
 */
@SuppressLint("ViewConstructor")
public class StringHiddenWidget extends QuestionWidget {
    private String answer;


    public StringHiddenWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        setVisibility(GONE);

        answer = prompt.getAnswerText();
    }

    @Override
    public void clearAnswer() {
        answer = null;
    }

    @Override
    public IAnswerData getAnswer() {
        if (TextUtils.isEmpty(answer)) {
            return null;
        } else {
            return new StringData(answer);
        }
    }

    @Override
    public void setFocus(Context context) {
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
    }

    @Override
    public String setBinaryData(@NonNull Object data) {
        answer = (String) data;
        return answer;
    }
}
