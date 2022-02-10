package rs.readahead.washington.mobile.views.fragment.uwazi.widgets;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.gson.internal.LinkedTreeMap;

import rs.readahead.washington.mobile.R;
import rs.readahead.washington.mobile.data.entity.uwazi.answer.UwaziLink;
import rs.readahead.washington.mobile.presentation.uwazi.UwaziValue;
import rs.readahead.washington.mobile.views.collect.widgets.QuestionWidget;
import rs.readahead.washington.mobile.views.fragment.uwazi.entry.UwaziEntryPrompt;
import timber.log.Timber;

@SuppressLint("ViewConstructor")
public class UwaziLinkWidget extends UwaziQuestionWidget {
    private static final String ROWS = "rows";

    protected boolean readOnly;
    protected EditText label;
    protected EditText url;

    @SuppressLint("NewApi")
    public UwaziLinkWidget(Context context, UwaziEntryPrompt prompt, boolean readOnlyOverride) {
        super(context, prompt);

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
/*
        label = new EditText(context);
        label.setTextColor(getResources().getColor(R.color.wa_white_80));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            label.setBackgroundTintList(context.getColorStateList(R.color.dialog_white_tint));
        }
        label.setId(QuestionWidget.newUniqueId());
        readOnly = prompt.isReadOnly() || readOnlyOverride;

        TableLayout.LayoutParams params = new TableLayout.LayoutParams();
        params.setMargins(7, 5, 7, 5);
        label.setLayoutParams(params);

        label.setHorizontallyScrolling(false);
        label.setSingleLine(false);

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
        }*/

        addLinkView(linearLayout);
    }

    @Override
    public void clearAnswer() {
        label.setText(null);
        url.setText(null);
    }

    @Override
    public UwaziValue getAnswer() {
        clearFocus();
        String l = label.getText().toString();
        String u = url.getText().toString();

        if (!URLUtil.isValidUrl(l)) {
            this.setConstraintValidationText("This is not a valid Url");
        }

        if (TextUtils.isEmpty(l) && TextUtils.isEmpty(u)) {
            return null;
        } else {
            UwaziLink link = new UwaziLink(l, u);
            return new UwaziValue(link);
        }
    }

    @Override
    public void setFocus(Context context) {
        // Put focus on text input field and display soft keyboard if appropriate.
        label.requestFocus();

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

    private void addLinkView(LinearLayout linearLayout) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        Timber.d("++++++++++++++++ inflater");

        inflater.inflate(R.layout.uwazi_widget_link, linearLayout, true);

        label = linearLayout.findViewById(R.id.label);
        url = linearLayout.findViewById(R.id.url);

        label.setId(QuestionWidget.newUniqueId());
        url.setId(QuestionWidget.newUniqueId());

        label.setSingleLine(true);
        url.setSingleLine(true);
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
        if (!URLUtil.isValidUrl(link.getUrl())) {
            this.setConstraintValidationText("This is not a valid Url");
        }

        return data.toString();
    }
}

