package org.horizontal.tella.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.text.Selection;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.internal.LinkedTreeMap;

import org.horizontal.tella.mobile.R;
import org.horizontal.tella.mobile.data.entity.uwazi.answer.UwaziLink;
import org.horizontal.tella.mobile.presentation.uwazi.UwaziValue;
import org.horizontal.tella.mobile.views.collect.widgets.QuestionWidget;
import org.horizontal.tella.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;

@SuppressLint("ViewConstructor")
public class UwaziLinkWidget extends UwaziQuestionWidget {

    protected boolean readOnly;
    protected EditText label;
    protected EditText url;

    @SuppressLint("NewApi")
    public UwaziLinkWidget(Context context, UwaziEntryPrompt prompt) {
        super(context, prompt);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        TextView labelText = new TextView(context);
        labelText.setTextColor(getResources().getColor(R.color.wa_white_80));
        labelText.setText(R.string.Uwazi_WidgetSubtitle_Label);

        TextView urlText = new TextView(context);
        urlText.setTextColor(getResources().getColor(R.color.wa_white_80));
        urlText.setText(R.string.Uwazi_WidgetSubtitle_URL);

        label = new EditText(context);
        label.setHint(R.string.Uwazi_WidgetSubtitle_URL);
        label.setHintTextColor(getResources().getColor(R.color.wa_white_64));
        label.setTextColor(getResources().getColor(R.color.wa_white_80));
        label.getBackground().setColorFilter(getResources().getColor(R.color.wa_white_80),
                PorterDuff.Mode.SRC_ATOP);
        label.setId(QuestionWidget.newUniqueId());

        url = new EditText(context);
        url.setTextColor(getResources().getColor(R.color.wa_white_80));
        url.getBackground().setColorFilter(getResources().getColor(R.color.wa_white_80),
                PorterDuff.Mode.SRC_ATOP);
        url.setId(QuestionWidget.newUniqueId());

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();

        params.setMargins(7, 5, 7, 5);
        label.setLayoutParams(params);
        url.setLayoutParams(params);

        label.setHorizontallyScrolling(false);
        label.setSingleLine(false);
        url.setHorizontallyScrolling(false);
        url.setSingleLine(false);

        String s = prompt.getAnswerText();
        if (s != null) {
            label.setText(s);
            Selection.setSelection(label.getText(), label.getText().toString().length());
        }

        if (readOnly) {
            label.setBackground(null);
            label.setEnabled(false);
            label.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            label.setFocusable(false);
            label.setVisibility(GONE);
            url.setBackground(null);
            url.setEnabled(false);
            url.setTextColor(ContextCompat.getColor(context, R.color.light_gray));
            url.setFocusable(false);
            url.setVisibility(GONE);
        }

        linearLayout.addView(labelText);
        linearLayout.addView(label);
        linearLayout.addView(urlText);
        linearLayout.addView(url);
        addAnswerView(linearLayout);

        clearAnswer();
    }

    @Override
    public void clearAnswer() {
        label.setText(null);
        url.setText(R.string.Uwazi_Answer_LinkInitialText);
    }

    @Override
    public UwaziValue getAnswer() {
        clearFocus();
        String l = label.getText().toString();
        String u = url.getText().toString();

        if ((!l.isEmpty() && !(isValidUrl(u)))) {
            this.setConstraintValidationText(getContext().getString(R.string.Uwazi_Info_NotValidURL));
        }

        if (l.isEmpty() && !(isEmptyUrl(u))) {
            this.setConstraintValidationText(getContext().getString(R.string.Uwazi_Info_UrlLabelEmpty));
        }

        if (TextUtils.isEmpty(l) && TextUtils.isEmpty(u)) {
            return null;
        } else {
            UwaziLink link = new UwaziLink(l, u);
            return new UwaziValue(link);
        }
    }

    private boolean isEmptyUrl(String url) {
        return url.equals(getContext().getString(R.string.Uwazi_Answer_LinkInitialText)) || url.isEmpty();
    }

    private boolean isValidUrl(String url) {
        return !(url.equals(getContext().getString(R.string.Uwazi_Answer_LinkInitialText))) && Patterns.WEB_URL.matcher(url).matches();
    }

    @Override
    public void setFocus(Context context) {
        InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputManager == null) {
            return;
        }

        if (!readOnly) {
            inputManager.showSoftInput(label, 0);
        } else {
            inputManager.hideSoftInputFromWindow(label.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return !event.isAltPressed() && super.onKeyDown(keyCode, event);
    }

    public String setBinaryData(@NonNull Object data) {
        UwaziLink link;
        if (data instanceof UwaziLink) {
            link = (UwaziLink) data;
        } else {
            LinkedTreeMap<String, Object> locationTreeMap = ((LinkedTreeMap<String, Object>) data);
            String labelTxt = String.valueOf(locationTreeMap.get("label"));
            String urlTxt = String.valueOf(locationTreeMap.get("url"));

            link = new UwaziLink(labelTxt, urlTxt);
        }

        label.setText(link.getLabel());
        url.setText(link.getUrl());

        return data.toString();
    }
}
