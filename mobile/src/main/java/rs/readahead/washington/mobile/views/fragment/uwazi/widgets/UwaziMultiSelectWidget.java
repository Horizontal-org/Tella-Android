package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.uwazi.NestedSelectValue;
import rs.readahead.washington.mobile.domain.entity.uwazi.SelectValue;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.custom.PanelToggleButton;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

/**
 * Based on ODK Collect SelectMultiWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziMultiSelectWidget extends UwaziQuestionWidget {
    private boolean checkBoxInit = true; // todo: check the need for this..
    private final List<SelectValue> items;
    private final ArrayList<AppCompatCheckBox> checkBoxes;
    private final ArrayList<String> checkIds = new ArrayList<>();;
    private int checkBoxCounter = 0;
    public UwaziMultiSelectWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        items = prompt.getSelectValues();
        checkBoxes = new ArrayList<>();

        // Layout holds the vertical list of check boxes
        LinearLayout choicesLayout = new LinearLayout(context);
        choicesLayout.setOrientation(LinearLayout.VERTICAL);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                // no checkbox group so id by answer + offset

                if (items.get(i).getValues() != null) {
                    // header item that hold nested checks
                    for (String key : Objects.requireNonNull(items.get(i).getValues()).keySet()){
                        LinearLayout ceckboxGroup = getHeaderCheckBox(key, i, inflater, prompt);
                        ViewGroup checkPanel = (ViewGroup) ceckboxGroup.findViewById(R.id.checkBoxes);
                        // nested key
                        for (NestedSelectValue nestedValue : Objects.requireNonNull(items.get(i).getValues()).get(key)){
                            // add nested checkbox
                            checkPanel.addView(getNastedCheckBox(nestedValue.getLabel(), checkBoxCounter++,inflater,prompt));
                            checkIds.add(nestedValue.getId());
                        }
                        choicesLayout.addView(ceckboxGroup);
                    }
                } else {
                    choicesLayout.addView(getCheckBox(items.get(i).getTranslatedLabel(), checkBoxCounter++, inflater, prompt));
                    checkIds.add(items.get(i).getId());
                }
            }
            addAnswerView(choicesLayout);
        }

        checkBoxInit = false;
    }

    @Override
    public List<UwaziValue> getAnswer() {
        List<String> vc = new ArrayList<>();
        List<UwaziValue> selection = new ArrayList<>();

        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox c = checkBoxes.get(i);
            if (c.isChecked()) {
                vc.add(checkIds.get(i));
            }
        }

        if (vc.size() == 0) {
            return null;
        } else {
            for (String answer : vc) {
               UwaziValue uValue = new UwaziValue(answer);
                selection.add(uValue);
            }
           return selection;
        }
    }

    @Override
    public void clearAnswer() {
        for (AppCompatCheckBox c: checkBoxes) {
            if (c.isChecked()) {
                c.setChecked(false);
            }
        }
    }

    @Override
    public void setFocus(Context context) {
        Util.hideKeyboard(context, this);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        for (AppCompatCheckBox c : checkBoxes) {
            c.cancelLongPress();
        }
    }

    private CharSequence getChoiceDisplayName(String rawName) {
        if (rawName != null) {
            return StringUtils.markdownToSpanned(rawName);
        } else {
            return "";
        }
    }

    public String setBinaryData(@NonNull Object data) {
        ArrayList<String> resultList = new ArrayList<>();
        if (data instanceof String ){
            resultList.add(data.toString());
        }else {
            for (Object o : (ArrayList) data) {
                if (o instanceof UwaziValue){
                    resultList.add((String) ((UwaziValue) o).getValue());
                }else {
                    String value = Objects.requireNonNull(((LinkedTreeMap) o).get("value")).toString();
                    resultList.add(value);
                }
            }
        }

        for (int i = 0; i < checkBoxes.size(); ++i) {
            AppCompatCheckBox box = checkBoxes.get(i);
            if (resultList.contains(checkIds.get(i))) {
                box.setChecked(true);
            }
        }

        return data.toString();
    }

    private View getCheckBox(String label, Integer i, LayoutInflater inflater, UwaziEntryPrompt prompt){
        @SuppressLint("InflateParams") View view = (View) inflater.inflate(R.layout.uwazi_checkbox_layout, null);
        AppCompatCheckBox c = (AppCompatCheckBox) view.findViewById(R.id.checkBox);
        c.setTag(i);
        c.setId(QuestionWidget.newUniqueId());
        c.setText(getChoiceDisplayName(label));
        c.setMovementMethod(LinkMovementMethod.getInstance());
        c.setFocusable(!prompt.isReadOnly());
        c.setEnabled(!prompt.isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setButtonTintList(ContextCompat.getColorStateList(getContext(), R.color.radio_buttons_color));
        }
        c.setTextColor(getResources().getColor(R.color.wa_white_88));

        checkBoxes.add(c);

        // when clicked, check for readonly before toggling
        c.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!checkBoxInit && formEntryPrompt.isReadOnly()) {
                buttonView.setChecked(! isChecked);
            }
        });

        return view;
    }

    private View getNastedCheckBox(String label, Integer i, LayoutInflater inflater, UwaziEntryPrompt prompt){
        @SuppressLint("InflateParams") View view = (View) inflater.inflate(R.layout.uwazi_nested_checkbox_layout, null);
        AppCompatCheckBox c = (AppCompatCheckBox) view.findViewById(R.id.checkBox);
        c.setTag(i);
        c.setId(QuestionWidget.newUniqueId());
        c.setText(getChoiceDisplayName(label));
        c.setMovementMethod(LinkMovementMethod.getInstance());
        c.setFocusable(!prompt.isReadOnly());
        c.setEnabled(!prompt.isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setButtonTintList(ContextCompat.getColorStateList(getContext(), R.color.radio_buttons_color));
        }
        c.setTextColor(getResources().getColor(R.color.wa_white_88));

        checkBoxes.add(c);

        // when clicked, check for readonly before toggling
        c.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!checkBoxInit && formEntryPrompt.isReadOnly()) {
                buttonView.setChecked(! isChecked);
            }
        });

        return view;
    }

    private LinearLayout getHeaderCheckBox(String label, Integer i, LayoutInflater inflater, UwaziEntryPrompt prompt) {
        @SuppressLint("InflateParams") LinearLayout view = (LinearLayout) inflater.inflate(R.layout.uwazi_header_checkbox_layout, null);
        AppCompatCheckBox c = (AppCompatCheckBox) view.findViewById(R.id.checkBox);
        ViewGroup checkboxesPanel = (ViewGroup) view.findViewById(R.id.checkBoxes);
        PanelToggleButton checkboxesToggle = (PanelToggleButton) view.findViewById(R.id.toggle_button);

        c.setTag(i);
        c.setId(QuestionWidget.newUniqueId());
        c.setText(getChoiceDisplayName(label));
        c.setMovementMethod(LinkMovementMethod.getInstance());
        c.setFocusable(!prompt.isReadOnly());
        c.setEnabled(!prompt.isReadOnly());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            c.setButtonTintList(ContextCompat.getColorStateList(getContext(), R.color.radio_buttons_color));
        }
        c.setTextColor(getResources().getColor(R.color.wa_white_88));

        // when clicked, check for readonly before toggling
        c.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!checkBoxInit && formEntryPrompt.isReadOnly()) {
                buttonView.setChecked(!isChecked);
            }
        });

        checkboxesToggle.setOnStateChangedListener(open -> {
                    if (checkboxesToggle.isOpen()) {
                        checkboxesPanel.setVisibility(View.VISIBLE);
                    } else {
                        checkboxesPanel.setVisibility(View.GONE);
                    }
                }
        );

        return view;
    }
}
