package com.example.checkinscanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    private String email = "";
    private String password = "";
    private Executor executor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        executor = ContextCompat.getMainExecutor(this);

        // Set title fade-in animation
        EditText title = findViewById(R.id.editTextText);
        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        title.startAnimation(fadeInAnimation);

        Button accountViewBtn = findViewById(R.id.btn_accounts);
        accountViewBtn.setOnClickListener(v -> {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardSecure()) {
                // If the device has a secure lock screen, authenticate
                showBiometricPrompt();
            } else {
                // If the device does not have a secure lock screen, proceed to the activity directly
                Intent intent = new Intent(MainActivity.this, AccountViewActivity.class);
                startActivity(intent);
            }
        });

        ImageButton helpBtn = findViewById(R.id.btn_help);
        helpBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HelpActivity.class);
            startActivity(intent);
        });

        // Get email and password from database
        DatabaseHelper db = new DatabaseHelper(this);
        refreshAccount(db);

        // Check if email and password are set
        Button scanBtn = findViewById(R.id.btn_scan);
        scanBtn.setOnClickListener(v -> {
            refreshAccount(db);
            if (email.isEmpty() || password.isEmpty()) {
                Toast toast = Toast.makeText(getApplicationContext(), R.string.toast_no_account, Toast.LENGTH_SHORT);
                toast.show();
            }
            else {
                Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                startActivity(intent);
            }
        });
    }

    // Show biometric prompt before opening AccountViewActivity
    private void showBiometricPrompt() {
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.auth_title))
                .setDescription(getString(R.string.auth_body))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Intent intent = new Intent(MainActivity.this, AccountViewActivity.class);
                startActivity(intent);
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), R.string.toast_auth_error, Toast.LENGTH_SHORT).show();
            }
        });
        biometricPrompt.authenticate(promptInfo);
    }

    // Refresh account credentials
    public void refreshAccount(DatabaseHelper db){
        ArrayList<String> data = db.getAccount();
        email = data.get(0);
        password = data.get(1);
    }

}