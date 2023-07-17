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
import android.graphics.Typeface;
import android.os.Build;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.uwazi.NestedSelectValue;
import rs.readahead.washington.mobile.domain.entity.uwazi.SelectValue;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import org.hzontal.shared_ui.buttons.PanelToggleButton;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


/**
 * Based on ODK Collect SelectOneWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziSelectOneWidget extends UwaziQuestionWidget implements
        OnCheckedChangeListener {
    List<SelectValue> items;
    ArrayList<AppCompatCheckBox> checkBoxes;
    ImageButton deleteButton;
    String answer;
    private int checkBoxCounter = 0;
    private final ArrayList<String> checkIds = new ArrayList<>();


    public UwaziSelectOneWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        items = prompt.getSelectValues();
        checkBoxes = new ArrayList<>();

        // Layout holds the vertical list of buttons
        LinearLayout buttonLayout = new LinearLayout(context);
        addDeleteButton();
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getValues() != null && !items.get(i).getValues().isEmpty()) {
                    LinearLayout checkboxGroup = getHeaderCheckBox(items.get(i).getTranslatedLabel(), inflater);
                    ViewGroup checkPanel = checkboxGroup.findViewById(R.id.checkBoxes);
                    for (NestedSelectValue nestedValue : Objects.requireNonNull(items.get(i).getValues())) {
                        checkPanel.addView(getNastedCheckBox(nestedValue.getTranslatedLabel(), checkBoxCounter++, inflater, prompt));
                        checkIds.add(nestedValue.getId());
                    }
                    buttonLayout.addView(checkboxGroup);
                } else {
                    buttonLayout.addView(getCheckBox(items.get(i).getTranslatedLabel(), checkBoxCounter++, inflater, prompt));
                    checkIds.add(items.get(i).getId());
                }
            }
        }

        buttonLayout.setOrientation(LinearLayout.VERTICAL);

        addAnswerView(buttonLayout);
    }

    @Override
    public void clearAnswer() {
        for (AppCompatCheckBox button : this.checkBoxes) {
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
            //SelectValue sc = checkIds.get(i);
            return new UwaziValue(checkIds.get(i));
        }
    }

    @Override
    public void setFocus(Context context) {
        // Hide the soft keyboard if it's showing.
        Util.hideKeyboard(context, this);
    }

    public int getCheckedId() {
        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox button = checkBoxes.get(i);
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

        for (AppCompatCheckBox button : checkBoxes) {
            if (button.isChecked() && !(buttonView == button)) {
                button.setChecked(false);
            }
        }
    }

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        for (AppCompatCheckBox r : checkBoxes) {
            r.setOnLongClickListener(l);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        for (AppCompatCheckBox button : this.checkBoxes) {
            button.cancelLongPress();
        }
    }

    private void addDeleteButton() {
        deleteButton = addButton(R.drawable.ic_cancel_rounded);
        deleteButton.setContentDescription(getContext().getString(R.string.action_cancel));
        deleteButton.setOnClickListener(v -> clearAnswer());
        deleteButton.setVisibility(GONE);
    }

    public String setBinaryData(@NonNull Object data) {
        answer = data.toString();

        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox button = checkBoxes.get(i);
            if (checkIds.get(i).equals(answer)) {
                button.setChecked(true);
                deleteButton.setVisibility(VISIBLE);
            }
        }

        return data.toString();
    }

    private View getCheckBox(String label, Integer i, LayoutInflater inflater, UwaziEntryPrompt prompt) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.uwazi_checkbox_layout, null);
        AppCompatCheckBox c = view.findViewById(R.id.checkBox);
        TextView labela = view.findViewById(R.id.labeled);
        c.setTag(i);
        c.setId(QuestionWidget.newUniqueId());
        labela.setText(getChoiceDisplayName(label));
        c.setMovementMethod(LinkMovementMethod.getInstance());
        c.setFocusable(!prompt.isReadOnly());
        c.setEnabled(!prompt.isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setButtonTintList(ContextCompat.getColorStateList(getContext(), R.color.radio_buttons_color));
        }
        c.setTextColor(getResources().getColor(R.color.wa_white_88));

        checkBoxes.add(c);

        c.setOnCheckedChangeListener(this);

        return view;
    }

    private View getNastedCheckBox(String label, Integer i, LayoutInflater inflater, UwaziEntryPrompt prompt) {
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.uwazi_nested_checkbox_layout, null);
        AppCompatCheckBox c = view.findViewById(R.id.checkBox);
        TextView labela = view.findViewById(R.id.labeled);
        c.setTag(i);
        c.setId(QuestionWidget.newUniqueId());
        labela.setText(getChoiceDisplayName(label));
        c.setMovementMethod(LinkMovementMethod.getInstance());
        c.setFocusable(!prompt.isReadOnly());
        c.setEnabled(!prompt.isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setButtonTintList(ContextCompat.getColorStateList(getContext(), R.color.radio_buttons_color));
        }
        c.setTextColor(getResources().getColor(R.color.wa_white_88));

        checkBoxes.add(c);

        c.setOnCheckedChangeListener(this);

        return view;
    }

    private LinearLayout getHeaderCheckBox(String label, LayoutInflater inflater) {
        @SuppressLint("InflateParams") LinearLayout view = (LinearLayout) inflater.inflate(R.layout.uwazi_header_checkbox_layout, null);
        AppCompatCheckBox c = view.findViewById(R.id.checkBox);
        TextView labela = view.findViewById(R.id.labeled);
        PanelToggleButton checkboxesToggle = view.findViewById(R.id.toggle_button);

        labela.setText(getChoiceDisplayName(label));
        labela.setTypeface(null, Typeface.BOLD);

        checkboxesToggle.setVisibility(GONE);
        c.setVisibility(GONE);

        return view;
    }

    private CharSequence getChoiceDisplayName(String rawName) {
        if (rawName != null) {
            return StringUtils.markdownToSpanned(rawName);
        } else {
            return "";
        }
    }
}
