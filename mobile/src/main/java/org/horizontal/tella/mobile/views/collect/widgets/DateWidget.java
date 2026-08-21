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
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.Date;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.util.Util;


/**
 * Based on ODK DateWidget.
 */
@SuppressLint("ViewConstructor")
public class DateWidget extends QuestionWidget {
    private DatePickerDialog datePickerDialog;

    private Button dateButton;
    private TextView dateText;
    ImageButton clearButton;

    private int year;
    private int month;
    private int dayOfMonth;

    private boolean nullAnswer = false;
    private boolean dateVisible = false;

    public DateWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        createDatePickerDialog();

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setContentDescription(context.getString(R.string.action_cancel));
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setVisibility(GONE);
        clearButton.setOnClickListener(v -> clearAnswer());

        addDateView(linearLayout);

        if (formEntryPrompt.getAnswerValue() == null) {
            clearAnswer();
        } else {
            DateTime dt = new DateTime(((Date) formEntryPrompt.getAnswerValue().getValue()).getTime());

            year = dt.getYear();
            month = dt.getMonthOfYear();
            dayOfMonth = dt.getDayOfMonth();

            setWidgetDate();
            datePickerDialog.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
        }

        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        nullAnswer = true;
        dateVisible = false;
        dateButton.setVisibility(VISIBLE);
        dateButton.setText(getResources().getString(R.string.collect_form_action_select_date)); // todo: say something smart here..
        dateText.setVisibility(GONE);
        clearButton.setVisibility(GONE);
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        if (nullAnswer) {
            return null;
        }

        LocalDateTime ldt = new LocalDateTime()
                .withYear(year)
                .withMonthOfYear(month)
                .withDayOfMonth(dayOfMonth)
                .withHourOfDay(0)
                .withMinuteOfHour(0);

        return new DateData(ldt.toDate());
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate() {
        nullAnswer = false;
        dateButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);
        dateText.setVisibility(VISIBLE);
        dateText.setText(getFormattedDate(getContext(), (Date) getAnswer().getValue()));
        dateVisible = true;
    }

    private void createDatePickerDialog() {
        datePickerDialog = new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            DateWidget.this.year = year;
            DateWidget.this.month = monthOfYear + 1;
            DateWidget.this.dayOfMonth = dayOfMonth;

            setWidgetDate();
        }, 1971, 1, 1);
    }

    private void addDateView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.collect_widget_date, linearLayout, true);

        dateButton = linearLayout.findViewById(R.id.dateWidgetButton);
        dateText = linearLayout.findViewById(R.id.dateText);

        dateButton.setId(QuestionWidget.newUniqueId());
        dateButton.setText(getResources().getString(R.string.collect_form_action_select_date));
        dateButton.setEnabled(!formEntryPrompt.isReadOnly());

        dateButton.setOnClickListener(v -> {
            if (nullAnswer) {
                DateTime dt = new DateTime();
                datePickerDialog.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            }

            if (dateVisible) {
                clearAnswer();
            } else {
                datePickerDialog.show();
            }
        });
    }

    private String getFormattedDate(Context context, Date date) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date);
    }
}
