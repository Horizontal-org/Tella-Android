package com.hzontal.tella_locking_ui.ui;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.hzontal.tella_locking_ui.R;


public class PinUnlockActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlock_activity);

        Button unlockButton = findViewById(R.id.unlock_button);
        unlockButton.setText("PIN");

        unlockButton.setOnClickListener(v -> handleSuccessfulUnlock());
    }

    private void handleSuccessfulUnlock() {
        setResult(RESULT_OK);
        finish();
    }
}
