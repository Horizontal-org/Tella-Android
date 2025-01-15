package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;
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

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValue;
import org.horizontal.tella.mobile.util.Util;
import org.horizontal.tella.mobile.views.collect.widgets.QuestionWidget;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

/**
 * Based on ODK DateWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziDateWidget extends UwaziQuestionWidget {
    private DatePickerDialog datePickerDialog;
    private Long intMsValue = 0L;

    private Button dateButton;
    private TextView dateText;
    ImageButton clearButton;

    private int year;
    private int month;
    private int dayOfMonth;

    private boolean nullAnswer = false;
    private boolean dateVisible = false;

    public UwaziDateWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        createDatePickerDialog();

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        clearButton = addButton(R.drawable.ic_cancel_rounded);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setVisibility(GONE);
        clearButton.setOnClickListener(v -> clearAnswer());
        clearButton.setContentDescription(getContext().getString(R.string.action_cancel));

        addDateView(linearLayout);
        clearAnswer();
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
    public UwaziValue getAnswer() {
        clearFocus();

        if (nullAnswer) {
            return null;
        }
        long intVal = intMsValue / 1000L;
        return new UwaziValue(Integer.parseInt(Long.toString(intVal)));
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate() throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateInter = this.year + "/" + this.month + "/" + this.dayOfMonth;

        Date date = sdf.parse(dateInter);

        if (date != null) {
            intMsValue = date.getTime();
        }
        nullAnswer = false;
        dateButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);
        dateText.setVisibility(VISIBLE);
        dateText.setText(getFormattedDate(getContext(), date));

        dateVisible = true;
    }

    private void createDatePickerDialog() {
        datePickerDialog = new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            UwaziDateWidget.this.year = year;
            UwaziDateWidget.this.month = monthOfYear + 1;
            UwaziDateWidget.this.dayOfMonth = dayOfMonth;

            try {
                setWidgetDate();
            } catch (ParseException e) {
                e.printStackTrace();
            }
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

    public String setBinaryData(@NonNull Object data) {
        BigDecimal bd = new BigDecimal(data.toString());
        long val = bd.longValue();
        intMsValue = val * 1000L;

        Date date = new Date(intMsValue);

        nullAnswer = false;
        dateButton.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);
        dateText.setVisibility(VISIBLE);
        dateText.setText(getFormattedDate(getContext(), date));
        dateVisible = true;

        return data.toString();
    }
}