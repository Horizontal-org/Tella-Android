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
import androidx.appcompat.widget.AppCompatCheckBox;
import android.text.method.LinkMovementMethod;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.StringUtils;


/**
 * Based on ODK Collect SelectMultiWidget.
 */
@SuppressLint("ViewConstructor")
public class SelectMultiWidget extends QuestionWidget {
    private boolean checkBoxInit = true; // todo: check the need for this..
    private final List<SelectChoice> items;
    private final ArrayList<AppCompatCheckBox> checkBoxes;


    @SuppressWarnings("unchecked")
    public SelectMultiWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        items = prompt.getSelectChoices();
        checkBoxes = new ArrayList<>();

        List<Selection> ve = new ArrayList<>();
        if (prompt.getAnswerValue() != null) {
            ve = (List<Selection>) prompt.getAnswerValue().getValue();
        }

        // Layout holds the vertical list of check boxes
        LinearLayout choicesLayout = new LinearLayout(context);
        choicesLayout.setOrientation(LinearLayout.VERTICAL);

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                // no checkbox group so id by answer + offset
                AppCompatCheckBox c = new AppCompatCheckBox(getContext());
                c.setTag(i);
                c.setId(QuestionWidget.newUniqueId());
                c.setText(getChoiceDisplayName(prompt.getSelectChoiceText(items.get(i))));
                c.setTextColor(getContext().getResources().getColor(R.color.primaryTextColor)); // todo: set in theme?
                c.setMovementMethod(LinkMovementMethod.getInstance());
                //c.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontsize);
                c.setFocusable(!prompt.isReadOnly());
                c.setEnabled(!prompt.isReadOnly());

                for (int vi = 0; vi < ve.size(); vi++) {
                    // match based on value, not key
                    if (items.get(i).getValue().equals(ve.get(vi).getValue())) {
                        c.setChecked(true);
                        break;
                    }
                }

                checkBoxes.add(c);

                // when clicked, check for readonly before toggling
                c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (!checkBoxInit && formEntryPrompt.isReadOnly()) {
                            buttonView.setChecked(! isChecked);
                        }
                    }
                });

                choicesLayout.addView(c);
            }

            addAnswerView(choicesLayout);
        }

        checkBoxInit = false;
    }

    @Override
    public IAnswerData getAnswer() {
        List<Selection> vc = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                vc.add(new Selection(items.get(i)));
            }
        }

        if (vc.size() == 0) {
            return null;
        } else {
            return new SelectMultiData(vc);
        }
    }

    @Override
    public void clearAnswer() {
        for (AppCompatCheckBox c: checkBoxes) {
            if (c.isChecked()) {
                c.setChecked(false);
            }
        }
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        for (AppCompatCheckBox c : checkBoxes) {
            c.cancelLongPress();
        }
    }

    private CharSequence getChoiceDisplayName(String rawName) {
        if (rawName != null) {
            return StringUtils.markdownToSpanned(rawName);
        } else {
            return "";
        }
    }
}
