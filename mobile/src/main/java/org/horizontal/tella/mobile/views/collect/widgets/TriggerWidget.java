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
import androidx.appcompat.widget.AppCompatCheckBox;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.form.api.FormEntryPrompt;

import org.horizontal.tella.mobile.R;


/**
 * Based on ODK TriggerWidget.
 */
@SuppressLint("ViewConstructor")
public class TriggerWidget extends QuestionWidget {
    public static final String OK_TEXT = "OK";

    private final AppCompatCheckBox triggerButton;
    private final TextView stringAnswer;
    private final FormEntryPrompt prompt;

    public TriggerWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);
        this.prompt = prompt;

        LayoutInflater inflater = LayoutInflater.from(getContext());
        triggerButton = (AppCompatCheckBox) inflater.inflate(R.layout.collect_checkbox_item, null);
        triggerButton.setTextColor(getResources().getColor(R.color.wa_white));
        triggerButton.setBackground(getResources().getDrawable(R.drawable.transparent_solid));
        triggerButton.setId(QuestionWidget.newUniqueId());
        triggerButton.setText(getContext().getString(R.string.collect_form_acknowledge_select_expl));
        triggerButton.setEnabled(!prompt.isReadOnly());

        triggerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (triggerButton.isChecked()) {
                    stringAnswer.setText(OK_TEXT);
                } else {
                    stringAnswer.setText(null);
                }
            }
        });

        stringAnswer = new TextView(getContext());
        stringAnswer.setId(QuestionWidget.newUniqueId());
        stringAnswer.setGravity(Gravity.CENTER);

        String s = prompt.getAnswerText();
        if (s != null) {
            if (s.equals(OK_TEXT)) {
                triggerButton.setChecked(true);
            } else {
                triggerButton.setChecked(false);
            }
            stringAnswer.setText(s);

        }

        // finish complex layout
        addAnswerView(triggerButton);
    }

    public FormEntryPrompt getFormEntryPrompt() {
        return prompt;
    }

    @Override
    public void clearAnswer() {
        stringAnswer.setText(null);
        triggerButton.setChecked(false);
    }

    @Override
    public void setFocus(Context context) {
    }

    @Override
    public IAnswerData getAnswer() {
        String s = stringAnswer.getText().toString();
        return !s.isEmpty()
                ? new StringData(s)
                : null;
    }

    @Override
    public void setOnLongClickListener(View.OnLongClickListener l) {
        triggerButton.setOnLongClickListener(l);
        stringAnswer.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        triggerButton.cancelLongPress();
        stringAnswer.cancelLongPress();
    }
}
