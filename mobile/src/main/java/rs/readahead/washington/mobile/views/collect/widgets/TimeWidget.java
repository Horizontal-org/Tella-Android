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
import android.app.TimePickerDialog;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.form.api.FormEntryPrompt;
import org.joda.time.DateTime;

import java.util.Date;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.util.Util;


/**
 * Based on ODK TimeWidget.
 */
@SuppressLint("ViewConstructor")
public class TimeWidget extends QuestionWidget {
    private TimePickerDialog timePickerDialog;

    private Button timeButton;
    private TextView timeText;
    ImageButton clearButton;

    private int hour;
    private int minute;

    private boolean nullAnswer = false;
    private boolean timeVisible = false;


    public TimeWidget(Context context, FormEntryPrompt prompt) {
        super(context, prompt);

        createTimePickerDialog();

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        clearButton = addButton(R.drawable.ic_delete_grey_24px);
        clearButton.setId(QuestionWidget.newUniqueId());
        clearButton.setEnabled(!formEntryPrompt.isReadOnly());
        clearButton.setVisibility(GONE);
        clearButton.setOnClickListener(v -> clearAnswer());

        addTimeView(linearLayout);

        if (formEntryPrompt.getAnswerValue() == null) {
            clearAnswer();
        } else {
            DateTime dt = new DateTime(((Date) formEntryPrompt.getAnswerValue().getValue()).getTime());

            hour = dt.getHourOfDay();
            minute = dt.getMinuteOfHour();

            setWidgetTime();
            timePickerDialog.updateTime(hour, minute);
        }

        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        nullAnswer = true;
        timeVisible = false;
        timeButton.setVisibility(VISIBLE);
        timeButton.setText(getResources().getString(R.string.collect_form_action_select_time)); // todo: say something smart here..
        timeText.setVisibility(GONE);
        clearButton.setVisibility(GONE);
    }

    @Override
    public IAnswerData getAnswer() {
        clearFocus();

        if (nullAnswer) {
            return null;
        }

        // use picker time, convert to today's date, store as utc
        DateTime dt = (new DateTime()).withTime(hour, minute, 0, 0);

        return new TimeData(dt.toDate());
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetTime() {
        nullAnswer = false;
        timeButton.setVisibility(GONE);
        timeText.setVisibility(VISIBLE);
        timeText.setText(getAnswer().getDisplayText());
        timeVisible = true;
        clearButton.setVisibility(VISIBLE);
    }

    private void createTimePickerDialog() {
        timePickerDialog = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            TimeWidget.this.hour = hourOfDay;
            TimeWidget.this.minute = minute;

            setWidgetTime();
        }, 0, 0, DateFormat.is24HourFormat(getContext()));
    }

    private void addTimeView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.collect_widget_time, linearLayout, true);

        timeButton = linearLayout.findViewById(R.id.timeWidgetButton);
        timeText = linearLayout.findViewById(R.id.timeText);

        timeButton.setId(QuestionWidget.newUniqueId());
        timeButton.setText(getResources().getString(R.string.collect_form_action_select_time));
        timeButton.setEnabled(!formEntryPrompt.isReadOnly());

        timeButton.setOnClickListener(v -> {
            if (nullAnswer) {
                DateTime dt = new DateTime();
                timePickerDialog.updateTime(dt.getHourOfDay(), dt.getMinuteOfHour());
            }

            if (timeVisible) {
                clearAnswer();
            } else {
                timePickerDialog.show();
            }
        });
    }
}
