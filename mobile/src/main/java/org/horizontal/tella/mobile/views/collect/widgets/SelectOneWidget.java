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
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.appcompat.widget.AppCompatRadioButton;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.Util;
import org.horizontal.tella.mobile.util.StringUtils;


/**
 * Based on ODK Collect SelectOneWidget.
 */
@SuppressLint("ViewConstructor")
public class SelectOneWidget extends QuestionWidget implements
        OnCheckedChangeListener {
    List<SelectChoice> items; // may take a while to compute
    ArrayList<RadioButton> buttons;
    ImageButton deleteButton;


    public SelectOneWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        // deleted: SurveyCTO-added support for dynamic select content (from .csv files)

        items = prompt.getSelectChoices();
        buttons = new ArrayList<>();

        // Layout holds the vertical list of buttons
        LinearLayout buttonLayout = new LinearLayout(context);
        addDeleteButton();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                String choiceName = prompt.getSelectChoiceText(items.get(i));
                CharSequence choiceDisplayName;
                if (choiceName != null) {
                    choiceDisplayName = StringUtils.markdownToSpanned(choiceName);
                } else {
                    choiceDisplayName = "";
                }

                AppCompatRadioButton r = (AppCompatRadioButton) inflater.inflate(R.layout.collect_radiobutton_item, null);

                r.setText(choiceDisplayName);
                r.setMovementMethod(LinkMovementMethod.getInstance());
                r.setTag(i);
                r.setId(QuestionWidget.newUniqueId());
                r.setEnabled(!prompt.isReadOnly());
                r.setFocusable(!prompt.isReadOnly());
                r.setTextColor(getResources().getColor(R.color.wa_white));

                buttons.add(r);

                if (items.get(i).getValue().equals(s)) {
                    r.setChecked(true);
                    deleteButton.setVisibility(VISIBLE);
                }

                r.setOnCheckedChangeListener(this);

                buttonLayout.addView(r);
            }
        }

        buttonLayout.setOrientation(LinearLayout.VERTICAL);

        // The buttons take up the right half of the screen
        addAnswerView(buttonLayout);
    }

    @Override
    public void clearAnswer() {
        for (RadioButton button : this.buttons) {
            if (button.isChecked()) {
                button.setChecked(false);
                clearNextLevelsOfCascadingSelect();
                deleteButton.setVisibility(GONE);
                break;
            }
        }
    }

    @Override
    public IAnswerData getAnswer() {
        int i = getCheckedId();
        if (i == -1) {
            return null;
        } else {
            SelectChoice sc = items.get(i);
            return new SelectOneData(new Selection(sc));
        }
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        Util.hideKeyboard(context, this);
    }

    public int getCheckedId() {
        for (int i = 0; i < buttons.size(); ++i) {
            RadioButton button = buttons.get(i);
            if (button.isChecked()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        deleteButton.setVisibility(VISIBLE);
        if (!isChecked) {
            // If it got unchecked, we don't care.
            return;
        }

        for (RadioButton button : buttons) {
            if (button.isChecked() && !(buttonView == button)) {
                button.setChecked(false);
                clearNextLevelsOfCascadingSelect();
            }
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (RadioButton r : buttons) {
            r.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (RadioButton button : this.buttons) {
            button.cancelLongPress();
        }
    }

    private void addDeleteButton() {
        deleteButton = addButton(R.drawable.ic_cancel_rounded);
        deleteButton.setContentDescription(getContext().getString(R.string.action_cancel));
        deleteButton.setOnClickListener(v -> clearAnswer());
        deleteButton.setVisibility(GONE);
    }
}
