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
import android.text.method.LinkMovementMethod;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;

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
        buttonLayout.setOrientation(LinearLayout.VERTICAL);

        addDeleteButton();

        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        // LayoutParams for every rdio row
        ViewGroup.MarginLayoutParams rowLayoutParams = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLayoutParams.setMargins(0, context.getResources().getDimensionPixelSize(R.dimen.activity_vertical_small_margin), 0, 0);

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                String choiceName = prompt.getSelectChoiceText(items.get(i));
                CharSequence choiceDisplayName;
                if (choiceName != null) {
                    choiceDisplayName = StringUtils.markdownToSpanned(choiceName);
                } else {
                    choiceDisplayName = "";
                }

                ViewGroup vg = (ViewGroup) inflate(getContext(), R.layout.collect_widget_select_one_radio, null);
                RadioButton r = vg.findViewById(R.id.radio_button);

                r.setText(choiceDisplayName);
                r.setMovementMethod(LinkMovementMethod.getInstance());
                r.setTag(i);
                r.setId(QuestionWidget.newUniqueId());
                r.setEnabled(!prompt.isReadOnly());
                r.setFocusable(!prompt.isReadOnly());

                buttons.add(r);

                if (items.get(i).getValue().equals(s)) {
                    r.setChecked(true);
                    deleteButton.setVisibility(VISIBLE);
                }

                r.setOnCheckedChangeListener(this);

                buttonLayout.addView(vg, rowLayoutParams);
            }
        }

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
        deleteButton = addButton(R.drawable.ic_cancel_white_24dp);
        deleteButton.setOnClickListener(v -> clearAnswer());
        deleteButton.setVisibility(GONE);
    }
}
