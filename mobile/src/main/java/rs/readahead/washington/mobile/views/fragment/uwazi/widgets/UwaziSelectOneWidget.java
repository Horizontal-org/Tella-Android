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
package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatRadioButton;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.uwazi.SelectValue;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


/**
 * Based on ODK Collect SelectOneWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziSelectOneWidget extends UwaziQuestionWidget implements
        OnCheckedChangeListener {
    List<SelectValue> items;
    ArrayList<RadioButton> buttons;
    ImageButton deleteButton;
    String answer;


    public UwaziSelectOneWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        items = prompt.getSelectValues();
        buttons = new ArrayList<>();

        // Layout holds the vertical list of buttons
        LinearLayout buttonLayout = new LinearLayout(context);
        addDeleteButton();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                String choiceName = items.get(i).getLabel();
                CharSequence choiceDisplayName;
                choiceDisplayName = StringUtils.markdownToSpanned(choiceName);

                AppCompatRadioButton r = (AppCompatRadioButton) inflater.inflate(R.layout.collect_radiobutton_item, null);

                r.setText(choiceDisplayName);
                r.setMovementMethod(LinkMovementMethod.getInstance());
                r.setTag(i);
                r.setId(QuestionWidget.newUniqueId());
                r.setEnabled(!prompt.isReadOnly());
                r.setFocusable(!prompt.isReadOnly());
                r.setTextColor(getResources().getColor(R.color.wa_white));

                buttons.add(r);

                r.setOnCheckedChangeListener(this);

                buttonLayout.addView(r);
            }
        }

        buttonLayout.setOrientation(LinearLayout.VERTICAL);

        addAnswerView(buttonLayout);
    }

    @Override
    public void clearAnswer() {
        for (RadioButton button : this.buttons) {
            if (button.isChecked()) {
                button.setChecked(false);
                answer = null;
                deleteButton.setVisibility(GONE);
                break;
            }
        }
    }

    @Override
    public UwaziValue getAnswer() {
        int i = getCheckedId();
        if (i == -1) {
            return null;
        } else {
            SelectValue sc = items.get(i);
            return new UwaziValue((String) sc.getId());
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
            return;
        }

        for (RadioButton button : buttons) {
            if (button.isChecked() && !(buttonView == button)) {
                button.setChecked(false);
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
        deleteButton.setOnClickListener(v -> clearAnswer());
        deleteButton.setVisibility(GONE);
    }

    public String setBinaryData(@NonNull Object data) {
        answer = data.toString();

        for (int i = 0; i < buttons.size(); ++i) {
            RadioButton button = buttons.get(i);
            if (items.get(i).getId().equals(answer)) {
                button.setChecked(true);
                deleteButton.setVisibility(VISIBLE);
            }
        }

        return data.toString();
    }
}
