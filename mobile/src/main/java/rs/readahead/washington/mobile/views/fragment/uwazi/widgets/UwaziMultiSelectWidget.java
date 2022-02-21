package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatCheckBox;

import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.domain.entity.uwazi.SelectValue;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.util.StringUtils;
import rs.readahead.washington.mobile.util.Util;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

/**
 * Based on ODK Collect SelectMultiWidget.
 */
@SuppressLint("ViewConstructor")
public class UwaziMultiSelectWidget extends UwaziQuestionWidget {
    private boolean checkBoxInit = true; // todo: check the need for this..
    private final List<SelectValue> items;
    private final ArrayList<AppCompatCheckBox> checkBoxes;

    @SuppressWarnings("unchecked")
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

                AppCompatCheckBox c = (AppCompatCheckBox) inflater.inflate(R.layout.collect_checkbox_item, null);
                c.setTag(i);
                c.setId(QuestionWidget.newUniqueId());
                c.setText(getChoiceDisplayName(items.get(i).getTranslatedLabel()));
                c.setMovementMethod(LinkMovementMethod.getInstance());
                //c.setTextSize(TypedValue.COMPLEX_UNIT_DIP, answerFontsize);
                c.setFocusable(!prompt.isReadOnly());
                c.setEnabled(!prompt.isReadOnly());

                c.setTextColor(getResources().getColor(R.color.wa_white));

                checkBoxes.add(c);

                // when clicked, check for readonly before toggling
                c.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (!checkBoxInit && formEntryPrompt.isReadOnly()) {
                        buttonView.setChecked(! isChecked);
                    }
                });

                choicesLayout.addView(c);
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
                vc.add(items.get(i).getId());
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
            if (resultList.contains(items.get(i).getId())) {
                box.setChecked(true);
            }
        }

        return data.toString();
    }
}
