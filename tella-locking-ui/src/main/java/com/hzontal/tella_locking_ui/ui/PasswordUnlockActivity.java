package com.hzontal.tella_locking_ui.ui;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import com.hzontal.tella_locking_ui.R;

import org.hzontal.tella.keys.config.IUnlockRegistryHolder;
import org.hzontal.tella.keys.config.UnlockRegistry;
import org.hzontal.tella.keys.config.UnlockResult;

import javax.crypto.spec.PBEKeySpec;

public class PasswordUnlockActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unlock_activity);

        Button unlockButton = findViewById(R.id.unlock_button);
        unlockButton.setText("Password");
        unlockButton.setOnClickListener(v -> handleUnlock("password"));

        Button badButton = findViewById(R.id.bad_button);
        badButton.setOnClickListener(v -> handleUnlock("xxx"));
    }

    private void handleUnlock(String secret) {
        IUnlockRegistryHolder holder = (IUnlockRegistryHolder) getApplicationContext();
        UnlockRegistry registry = holder.getUnlockRegistry();

        registry.putResult(PasswordUnlockActivity.class.getName(),
                new UnlockResult(new PBEKeySpec(secret.toCharArray())));

        setResult(RESULT_OK);
        finish();
    }
}
