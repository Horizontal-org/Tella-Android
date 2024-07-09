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
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;

import java.util.ArrayList;
import java.util.List;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.Util;


/**
 * Based on ODK Collect SpinnerWidget.
 */
@SuppressLint("ViewConstructor")
public class SpinnerWidget extends QuestionWidget {
    List<SelectChoice> items;
    Spinner spinner;
    String[] choices;


    public SpinnerWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        items = new ArrayList<>(prompt.getSelectChoices());
        items.add(0, new SelectChoice("", ""));

        LinearLayout linearLayout = new LinearLayout(context);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.collect_widget_spinner, linearLayout, true);
        view.setId(QuestionWidget.newUniqueId());
        view.setBackgroundColor(getResources().getColor(R.color.wa_white_90));

        spinner = view.findViewById(R.id.spinner);

        choices = new String[items.size()];
        choices[0] = context.getString(R.string.collect_form_ranking_action_select_answer);
        for (int i = 1; i < items.size(); i++) {
            choices[i] = prompt.getSelectChoiceText(items.get(i));
        }

        SpinnerAdapter adapter =
                new SpinnerAdapter(getContext(), android.R.layout.simple_spinner_item, choices);

        spinner.setAdapter(adapter);
        spinner.setPrompt(prompt.getQuestionText());
        spinner.setEnabled(!prompt.isReadOnly());
        spinner.setFocusable(!prompt.isReadOnly());

        // Fill in previous answer
        String s = null;
        if (prompt.getAnswerValue() != null) {
            s = ((Selection) prompt.getAnswerValue().getValue()).getValue();
        }

        spinner.setSelection(0);
        if (s != null) {
            for (int i = 0; i < items.size(); ++i) {
                String match = items.get(i).getValue();
                if (match.equals(s)) {
                    spinner.setSelection(i);
                }
            }
        }

        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        spinner.setSelection(0);
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        int i = spinner.getSelectedItemPosition();

        if (i == -1 || i == 0) {
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

    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        spinner.setOnLongClickListener(l);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        spinner.cancelLongPress();
    }

    private class SpinnerAdapter extends ArrayAdapter<String> {
        Context context;
        String[] items;

        SpinnerAdapter(@NonNull final Context context, int resource, @NonNull String[] objects) {
            super(context, resource, objects);

            this.context = context;
            this.items = objects;
        }

        @Override
        public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false);
            }

            TextView tv = convertView.findViewById(android.R.id.text1);
            tv.setPadding(7, 5, 7, 5);

            tv.setText(position == 0 ? context.getString(R.string.collect_form_ranking_action_clear_selection) : items[position]);

            if (spinner.getSelectedItemPosition() == position && position > 0) {
                tv.setTypeface(null, Typeface.BOLD);
            } else {
                tv.setTypeface(null, Typeface.NORMAL);
            }

            return convertView;
        }

        @Override
        public int getCount() {
            return items.length;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(android.R.layout.simple_spinner_item, parent, false);
            }

            TextView tv = convertView.findViewById(android.R.id.text1);
            tv.setPadding(7, 5, 7, 5);
            tv.setText(items[position]);

            return convertView;
        }
    }
}
