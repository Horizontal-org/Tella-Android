package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.entity.uwazi.answer.UwaziDateRange;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziMultiDateRangeWidget extends UwaziQuestionWidget {
    private final HashMap<Integer, Long> dateValuesFrom = new HashMap<>();
    private final HashMap<Integer, Long> dateValuesTo = new HashMap<>();
    private Integer counter = 1;

    private LinearLayout dateListLayout;
    private boolean nullAnswer = false;

    public UwaziMultiDateRangeWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        addDateView(linearLayout);
        clearAnswer();
        addAnswerView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        nullAnswer = true;
    }

    @Override
    public List<UwaziValue> getAnswer() {
        List<UwaziValue> longRanges = new ArrayList<>();
        clearFocus();

        if (nullAnswer) {
            return null;
        }

        for (Map.Entry<Integer, Long> integerLongEntry : dateValuesFrom.entrySet()) {
            Integer key = (Integer) integerLongEntry.getKey();

            Long valueFrom = (Long) integerLongEntry.getValue();
            Long valueTo = dateValuesTo.get(key);

            if (valueFrom != null && valueFrom > 0 && valueTo != null && valueTo > 0) {
                long intVal = valueFrom / 1000L;
                long intVal1 = valueTo / 1000L;
                UwaziDateRange range = new UwaziDateRange(Integer.parseInt(Long.toString(intVal)), Integer.parseInt(Long.toString(intVal1)));
                longRanges.add(new UwaziValue(range));
            }
        }

        if (longRanges.isEmpty()) {
            return null;
        } else {
            return longRanges;
        }
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate(Integer key, TextView dateText, Button dateButton, int year, int month, int dayOfMonth, Boolean isFrom) throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/HH");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateInter = year + "/" + month + "/" + dayOfMonth+ "/" + 12;

        Date date = sdf.parse(dateInter);

        if (isFrom) {
            Long intMsValueTo = dateValuesTo.get(key);
            if (date != null) {
                if (intMsValueTo == null || intMsValueTo > date.getTime()) {
                    dateValuesFrom.put(key, date.getTime());
                } else {
                    showDateConstraint();
                    return;
                }
            }
        } else {
            if (date != null) {
            Long intMsValueFrom = dateValuesFrom.get(key);
                if (intMsValueFrom == null || intMsValueFrom < date.getTime()) {
                    dateValuesTo.put(key, date.getTime());
                } else {
                    showDateConstraint();
                    return;
                }
            }
        }

        nullAnswer = false;
        dateButton.setVisibility(GONE);
        dateText.setVisibility(VISIBLE);
        dateText.setText(getFormattedDate(getContext(), date));
    }

    private DatePickerDialog createDatePickerDialog(Integer key, TextView dateText, Button dateButton, Boolean isFrom) {
        return new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            try {
                setWidgetDate(key, dateText, dateButton, year, monthOfYear + 1, dayOfMonth, isFrom);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, 1971, 1, 1);
    }

    private void addDateView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multidate, linearLayout, true);

        dateListLayout = view.findViewById(R.id.dates);

        View one = getDateLayout(counter, null, null);
        dateListLayout.addView(one);

        Button addDateButton = linearLayout.findViewById(R.id.addText);
        addDateButton.setText(R.string.Uwazi_WidgetMultiDate_AddDateRange);
        addDateButton.setOnClickListener(v -> {
            View newOne = getDateLayout(++counter, null, null);
            dateListLayout.addView(newOne);
        });
    }

    private View getDateLayout(Integer key, Date dateFrom, Date dateTo) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_uwazi_date_range, null);
        TextView dateTextFrom = item.findViewById(R.id.dateTextFrom);
        TextView dateTextTo = item.findViewById(R.id.dateTextTo);
        item.setTag(key);
        ImageButton clearItem = item.findViewById(R.id.clearWidgetButton);
        int padding = getResources().getDimensionPixelSize(R.dimen.collect_widget_icon_padding);
        clearItem.setPadding(padding, 0, padding, 0);
        clearItem.setVisibility(View.VISIBLE);

        Button dateButtonFrom = item.findViewById(R.id.dateWidgetButtonFrom);
        DatePickerDialog datePickerDialogFrom = createDatePickerDialog(key, dateTextFrom, dateButtonFrom, true);

        dateButtonFrom.setOnClickListener(v -> {
            DateTime dt = new DateTime();
            datePickerDialogFrom.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            datePickerDialogFrom.show();
        });

        Button dateButtonTo = item.findViewById(R.id.dateWidgetButtonTo);
        DatePickerDialog datePickerDialogTo = createDatePickerDialog(key, dateTextTo, dateButtonTo, false);

        dateButtonTo.setOnClickListener(v -> {
            DateTime dt = new DateTime();
            datePickerDialogTo.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            datePickerDialogTo.show();
        });

        clearItem.setOnClickListener(v -> {
            dateListLayout.removeView(item);
            dateValuesFrom.remove(key);
            dateValuesTo.remove(key);
        });

        if (dateFrom != null && dateTo != null) {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dateFrom);
                setWidgetDate(key, dateTextFrom, dateButtonFrom, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), true);
                calendar.setTime(dateTo);
                setWidgetDate(key, dateTextTo, dateButtonTo, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH), false);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return item;
    }

    private String getFormattedDate(Context context, Date date) {
        java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
        return dateFormat.format(date);
    }

    public String setBinaryData(@NonNull Object data) {
        dateListLayout.removeAllViews();
        dateValuesFrom.clear();
        dateValuesTo.clear();
        counter = 0;

        for (Object o : (ArrayList) data) {
            UwaziValue value;
            UwaziDateRange dateRange;
            if (o instanceof UwaziValue) {
                value = (UwaziValue) o;
                dateRange = (UwaziDateRange)  value.getValue();
            } else {
                Double fromD = (Double) ((LinkedTreeMap) ((Map) o).get("value")).get("from");
                Double toD = (Double) ((LinkedTreeMap) ((Map) o).get("value")).get("to");
                long from = new BigDecimal(fromD).longValueExact();
                long to = new BigDecimal(toD).longValueExact();

                dateRange = new UwaziDateRange((int) from, (int) to);
            }
            BigDecimal from = new BigDecimal(dateRange.getFrom());
            long val = from.longValue();
            long intMsValueFrom = val * 1000L;

            BigDecimal to = new BigDecimal(dateRange.getTo());
            long valTo = to.longValue();
            long intMsValueTo = valTo * 1000L;

            Date dateFrom = new Date(intMsValueFrom);
            Date dateTo = new Date(intMsValueTo);

            dateListLayout.addView(getDateLayout(counter, dateFrom, dateTo));
            counter++;
        }

        nullAnswer = false;

        return data.toString();
    }

    void showDateConstraint() {
        DialogUtils.showBottomMessage((Activity) getContext(), getContext().getString(R.string.Uwazi_WidgetDateRange_Constraint), true);
    }
}