package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
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

import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;


@SuppressLint("ViewConstructor")
public class UwaziMultiDateWidget extends UwaziQuestionWidget {
    private final HashMap<Integer, Long> dateValues = new HashMap<>();
    private Integer counter = 1;

    private LinearLayout dateListLayout;
    private boolean nullAnswer = false;

    public UwaziMultiDateWidget(Context context, UwaziEntryPrompt prompt) {
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
        clearFocus();
        List<UwaziValue> longDates = new ArrayList<>();

        if (nullAnswer) {
            return null;
        }
        for (Long dateLong : dateValues.values()) {
            if (dateLong != null && dateLong > 0) {
                long intVal = dateLong / 1000L;
                longDates.add(new UwaziValue(Integer.parseInt(Long.toString(intVal))));
            }
        }

        if (longDates.isEmpty()) {
            return null;
        } else {
            return longDates;
        }
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    private void setWidgetDate(Integer key, TextView dateText, Button dateButton, int year, int month, int dayOfMonth) throws ParseException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateInter = year + "/" + month + "/" + dayOfMonth;

        Date date = sdf.parse(dateInter);

        if (date != null) {
            dateValues.put(key, date.getTime());
        }
        nullAnswer = false;
        dateButton.setVisibility(GONE);
        dateText.setVisibility(VISIBLE);
        dateText.setText(getFormattedDate(getContext(), date));
    }

    private DatePickerDialog createDatePickerDialog(Integer key, TextView dateText, Button dateButton) {
        return new DatePickerDialog(getContext(), AlertDialog.THEME_HOLO_LIGHT, (view, year, monthOfYear, dayOfMonth) -> {
            try {
                setWidgetDate(key, dateText, dateButton, year, monthOfYear + 1, dayOfMonth);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }, 1971, 1, 1);
    }

    private void addDateView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());

        View view = inflater.inflate(R.layout.uwazi_widget_multidate, linearLayout, true);

        dateListLayout = view.findViewById(R.id.dates);

        View one = getDateLayout(counter, null);
        dateListLayout.addView(one);

        Button addDateButton = linearLayout.findViewById(R.id.addText);
        addDateButton.setOnClickListener(v -> {
            View newOne = getDateLayout(++counter, null);
            dateListLayout.addView(newOne);
        });
    }

    private View getDateLayout(Integer key, Date date) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        @SuppressLint("InflateParams")
        LinearLayout item = (LinearLayout) inflater.inflate(R.layout.item_uwazi_date, null);
        TextView dateText = item.findViewById(R.id.dateText);
        item.setTag(key);
        ImageButton clearItem = item.findViewById(R.id.clearWidgetButton);
        int padding = getResources().getDimensionPixelSize(R.dimen.collect_widget_icon_padding);
        clearItem.setPadding(padding, 0, padding, 0);
        clearItem.setVisibility(View.VISIBLE);

        Button dateButton = item.findViewById(R.id.dateWidgetButton);
        dateButton.setText(getResources().getString(R.string.collect_form_action_select_date));
        DatePickerDialog datePickerDialog = createDatePickerDialog(key, dateText, dateButton);

        dateButton.setOnClickListener(v -> {
            DateTime dt = new DateTime();
            datePickerDialog.updateDate(dt.getYear(), dt.getMonthOfYear() - 1, dt.getDayOfMonth());
            datePickerDialog.show();
        });

        clearItem.setOnClickListener(v -> {
            dateListLayout.removeView(item);
            dateValues.remove(key);
        });

        if (date != null) {
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                setWidgetDate(key, dateText, dateButton, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+1, calendar.get(Calendar.DAY_OF_MONTH));
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
        dateValues.clear();
        counter = 0;

        for (Object o : (ArrayList) data) {
            String value = Objects.requireNonNull(((LinkedTreeMap) o).get("value")).toString();
            BigDecimal bd = new BigDecimal(value);
            long valong = bd.longValue() * 1000L;
            dateValues.put(counter, valong);
            Date date = new Date(valong);
            dateListLayout.addView(getDateLayout(counter, date));
            counter++;
        }

        nullAnswer = false;

        return data.toString();
    }
}