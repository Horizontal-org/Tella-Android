package org.hzontal.shared_ui.dropdownlist;

import android.widget.EditText;

public interface TextWatcherInterface {
    void onTextChanged(EditText editText, CharSequence s, int start, int before, int count);
}
