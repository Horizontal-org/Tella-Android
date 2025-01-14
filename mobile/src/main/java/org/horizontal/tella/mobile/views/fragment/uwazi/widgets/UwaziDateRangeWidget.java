package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.gson.internal.LinkedTreeMap;

import org.hzontal.shared_ui.utils.DialogUtils;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.entity.uwazi.answer.UwaziDateRange;
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValue;
import org.horizontal.tella.mobile.util.Util;
import org.horizontal.tella.mobile.views.collect.widgets.QuestionWidget;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziDateRangeWidget extends UwaziQuestionWidget {
    private DatePickerDialog datePickerDialogFrom;
    private DatePickerDialog datePickerDialogTo;
    private Long intMsValueFrom = 0L;
    private Long intMsValueTo = 0L;

    private Button dateButtonFrom;
    private TextView dateTextFrom;
    ImageButton clearButton;

    private Button dateButtonTo;
    private TextView dateTextTo;

    private int yearFrom;
    private int monthFrom;
    private int dayOfMonthFrom;

    private int yearTo;
    private int monthTo;
    private int dayOfMonthTo;

    private boolean dateFromVisible = false;
    private boolean dateToVisible = false;

    public UwaziDateRangeWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        createDatePickerDialogFrom();
        createDatePickerDialogTo();

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
        dateFromVisible = false;
        dateToVisible = false;
        dateButtonFrom.setVisibility(VISIBLE);
        dateButtonFrom.setText(getResources().getString(R.string.collect_form_action_select_date));
        dateButtonTo.setVisibility(VISIBLE);
        dateButtonTo.setText(getResources().getString(R.string.collect_form_action_select_date));
        dateTextFrom.setVisibility(GONE);
        dateTextTo.setVisibility(GONE);
        clearButton.setVisibility(GONE);
        intMsValueFrom = 0L;
        intMsValueTo = 0L;
    }

    @Override
    public UwaziValue getAnswer() {
        clearFocus();

        if (intMsValueFrom == 0 && intMsValueTo == 0) {
            return null;
        }
        long intVal = intMsValueFrom / 1000L;
        long intVal1 = intMsValueTo / 1000L;
        UwaziDateRange range = new UwaziDateRange(Integer.parseInt(Long.toString(intVal)), Integer.parseInt(Long.toString(intVal1)));
        return new UwaziValue(range);
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate() throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateInter = this.yearFrom + "/" + this.monthFrom + "/" + this.dayOfMonthFrom;

        Date date = sdf.parse(dateInter);

        if (date != null && (intMsValueTo == 0L || intMsValueTo > date.getTime())) {
            intMsValueFrom = date.getTime();
            dateButtonFrom.setVisibility(GONE);
            clearButton.setVisibility(VISIBLE);
            dateTextFrom.setVisibility(VISIBLE);
            dateTextFrom.setText(getFormattedDate(getContext(), date));
            dateFromVisible = true;
        } else {
            showDateConstraint();
        }
    }

    private void setWidgetDateTo() throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateInter = this.yearTo + "/" + this.monthTo + "/" + this.dayOfMonthTo;

        Date date = sdf.parse(dateInter);

        if (date != null && (intMsValueFrom == 0L || intMsValueFrom < date.getTime())) {
            intMsValueTo = date.getTime();
            dateButtonTo.setVisibility(GONE);
            clearButton.setVisibility(VISIBLE);
            dateTextTo.setVisibility(VISIBLE);
            dateTextTo.setText(getFormattedDate(getContext(), date));
            dateToVisible = true;
        } else {
            showDateConstraint();
        }
    }


    private void createDatePickerDialogFrom() {
        datePickerDialogFrom = new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            this.yearFrom = year;
            this.monthFrom = monthOfYear + 1;
            this.dayOfMonthFrom = dayOfMonth;

            try {
                setWidgetDate();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, 2020, 1, 1);
    }

    private void createDatePickerDialogTo() {
        datePickerDialogTo = new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            this.yearTo = year;
            this.monthTo = monthOfYear + 1;
            this.dayOfMonthTo = dayOfMonth;

            try {
                setWidgetDateTo();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, 2020, 1, 1);
    }

    private void addDateView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.uwazi_widget_daterange, linearLayout, true);

        dateButtonFrom = linearLayout.findViewById(R.id.dateWidgetButton);
        dateTextFrom = linearLayout.findViewById(R.id.dateText);

        dateButtonTo = linearLayout.findViewById(R.id.dateWidgetButton1);
        dateTextTo = linearLayout.findViewById(R.id.dateText1);

        dateButtonFrom.setId(QuestionWidget.newUniqueId());
        dateButtonFrom.setText(getResources().getString(R.string.collect_form_action_select_date));
        dateButtonFrom.setEnabled(!formEntryPrompt.isReadOnly());

        dateButtonTo.setId(QuestionWidget.newUniqueId());
        dateButtonTo.setText(getResources().getString(R.string.collect_form_action_select_date));
        dateButtonTo.setEnabled(!formEntryPrompt.isReadOnly());

        dateButtonFrom.setOnClickListener(v -> {
            if (intMsValueFrom == 0L) {
                DateTime dt = new DateTime();
                datePickerDialogFrom.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            }

            if (dateFromVisible) {
                clearAnswer();
            } else {
                datePickerDialogFrom.show();
            }
        });

        dateButtonTo.setOnClickListener(v -> {
            if (intMsValueTo == 0L) {
                DateTime dt = new DateTime();
                datePickerDialogTo.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            }

            if (dateToVisible) {
                clearAnswer();
            } else {
                datePickerDialogTo.show();
            }
        });
    }

    private String getFormattedDate(Context context, Date date) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date);
    }

    public String setBinaryData(@NonNull Object data) {
        UwaziDateRange dateRange;

        if (data instanceof UwaziDateRange) {
            dateRange = (UwaziDateRange) data;
        } else {
            LinkedTreeMap<String, Object> locationTreeMap = ((LinkedTreeMap<String, Object>) data);
            long from = new BigDecimal(locationTreeMap.get("from").toString()).longValueExact();
            long to = new BigDecimal(locationTreeMap.get("to").toString()).longValueExact();

            dateRange = new UwaziDateRange((int) from, (int) to);
        }

        BigDecimal from = new BigDecimal(dateRange.getFrom());
        long val = from.longValue();
        intMsValueFrom = val * 1000L;

        BigDecimal to = new BigDecimal(dateRange.getTo());
        long valTo = to.longValue();
        intMsValueTo = valTo * 1000L;

        Date dateFrom = new Date(intMsValueFrom);
        Date dateTo = new Date(intMsValueTo);

        dateButtonFrom.setVisibility(GONE);
        dateButtonTo.setVisibility(GONE);
        clearButton.setVisibility(VISIBLE);
        dateTextFrom.setVisibility(VISIBLE);
        dateTextFrom.setText(getFormattedDate(getContext(), dateFrom));
        dateTextTo.setVisibility(VISIBLE);
        dateTextTo.setText(getFormattedDate(getContext(), dateTo));
        dateFromVisible = true;
        dateToVisible = true;

        return data.toString();
    }

    void showDateConstraint() {
        DialogUtils.showBottomMessage((Activity) getContext(), getContext().getString(R.string.Uwazi_WidgetDateRange_Constraint), true);
    }
}