package com.example.checkinscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;

public class AccountViewActivity extends Activity {

    private String email;
    private String password;
    private TextView emailView;
    private TextView passwordView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_account);

        passwordView = findViewById(R.id.editTextAccountPassword);
        emailView = findViewById(R.id.editTextAccountEmail);
        TextView passwordViewLabel = findViewById(R.id.textViewPassword);
        passwordView.setVisibility(View.INVISIBLE);
        passwordViewLabel.setVisibility(View.INVISIBLE);

        // Show/hide password
        Button passwordShowBtn = findViewById(R.id.btn_show_password);
        passwordShowBtn.setOnClickListener(v -> {
            @SuppressLint("CutPasteId") TextView passwordView1 = findViewById(R.id.editTextAccountPassword);
            @SuppressLint("CutPasteId") TextView passwordViewLabel1 = findViewById(R.id.textViewPassword);
            if (passwordView1.getVisibility() == View.INVISIBLE) {
                passwordView1.setVisibility(View.VISIBLE);
                passwordViewLabel1.setVisibility(View.VISIBLE);
                passwordShowBtn.setText(R.string.btn_hide_password);
            } else {
                passwordView1.setVisibility(View.INVISIBLE);
                passwordViewLabel1.setVisibility(View.INVISIBLE);
                passwordShowBtn.setText(R.string.btn_show_password);
            }
        });

        Button BackBtn = findViewById(R.id.btn_back);
        BackBtn.setOnClickListener(v -> finish());

        // Get email and password from database
        DatabaseHelper db = new DatabaseHelper(this);
        ArrayList<String> data = db.getAccount();
        email = data.get(0);
        password = data.get(1);
        if (!email.isEmpty()) {
            emailView.setText(email);
            passwordShowBtn.setEnabled(true);
            passwordView.setText(password);
        }
        else {
            emailView.setText(R.string.no_email);
            passwordShowBtn.setEnabled(false);
        }


        Button EditAccountBtn = findViewById(R.id.btn_edit_account);
        if (email.isEmpty()) {
            EditAccountBtn.setText(R.string.btn_add_account);
        }
        else {
            EditAccountBtn.setText(R.string.btn_edit_account);
        }
        EditAccountBtn.setOnClickListener(v -> {
            showChangeEmailPasswordDialog();
        });

    }

    // Refresh the fields content and reset the buttons
    public void refreshView() {
        passwordView.setVisibility(View.INVISIBLE);
        TextView passwordViewLabel = findViewById(R.id.textViewPassword);
        passwordViewLabel.setVisibility(View.INVISIBLE);
        Button passwordShowBtn = findViewById(R.id.btn_show_password);
        passwordShowBtn.setText(R.string.btn_show_password);
        if (!email.isEmpty()) {
            emailView.setText(email);
            passwordShowBtn.setEnabled(true);
            passwordView.setText(password);
        }
        else {
            emailView.setText(R.string.no_email);
            passwordShowBtn.setEnabled(false);
        }
    }

    // Show the dialog to change the email and password
    public void showChangeEmailPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.lbl_account_edit_title);

        // Inflate the custom layout
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_change_email_password, null);
        builder.setView(dialogView);

        // Set the fields text to the current email and password
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextPassword = dialogView.findViewById(R.id.editTextPassword);
        editTextEmail.setText(email);
        editTextPassword.setText(password);

        TextView errorLabel = dialogView.findViewById(R.id.errorLabel);

        DatabaseHelper db = new DatabaseHelper(this);

        // Set the buttons
        builder.setPositiveButton(R.string.btn_save, null);
        builder.setNegativeButton(R.string.btn_cancel, (dialog, which) -> dialog.dismiss());

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {

            Button saveBtn = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(view -> {
                errorLabel.setVisibility(View.INVISIBLE);
                String newEmail = editTextEmail.getText().toString();
                String newPassword = editTextPassword.getText().toString();
                // Input validation
                if (newEmail.isEmpty() || newPassword.isEmpty()) {
                    errorLabel.setVisibility(View.VISIBLE);
                    errorLabel.setText(R.string.error_both_fields_are_required);
                }
                else if (!newEmail.matches("^4\\d{7}@live\\.napier\\.ac\\.uk$")) {
                    errorLabel.setVisibility(View.VISIBLE);
                    errorLabel.setText(R.string.error_invalid_email);
                }
                else {
                    // Save the new email and password to the database
                    db.insertData(newEmail, newPassword);
                    // Update the email and password in the AccountViewActivity
                    email = newEmail;
                    password = newPassword;
                    emailView.setText(email);
                    passwordView.setText(password);
                    // Clear cookies to avoid auto-login with old credentials
                    CookieManager.getInstance().removeAllCookies(null);
                    dialog.dismiss();
                    // Reset the activity
                    Button EditAccountBtn = findViewById(R.id.btn_edit_account);
                    EditAccountBtn.setText(R.string.btn_edit_account);
                    refreshView();
                }
            });
            Button cancelBtn = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_NEGATIVE);
            cancelBtn.setOnClickListener(view -> {
                dialog.dismiss();
                refreshView();
            });
        });
        dialog.show();
    }


}
