package com.android.iunoob.bloodbank.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.iunoob.bloodbank.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText inputEmail, inputPassword;
    private Button btnRegister, btnBackToLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Views
        inputEmail = findViewById(R.id.reg_email);
        inputPassword = findViewById(R.id.reg_password);
        btnRegister = findViewById(R.id.btn_register);
        btnBackToLogin = findViewById(R.id.btn_back_login);
        progressBar = findViewById(R.id.reg_progress);

        btnBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        btnRegister.setOnClickListener(v -> {

            String email = inputEmail.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                inputEmail.setError("Email is required");
                return;
            }
            if (TextUtils.isEmpty(password)) {
                inputPassword.setError("Password is required");
                return;
            }
            if (password.length() < 6) {
                inputPassword.setError("Password must be at least 6 characters");
                return;
            }

            progressBar.setVisibility(View.VISIBLE);

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(RegisterActivity.this, task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "Registration successful! Please complete your profile.", Toast.LENGTH_LONG).show();
                            
                            // --- FIX: Redirect to ProfileActivity after registration ---
                            Intent intent = new Intent(RegisterActivity.this, ProfileActivity.class);
                            // Add a flag to indicate this is the first time setup
                            intent.putExtra("FIRST_TIME_SETUP", true);
                            startActivity(intent);
                            finish();
                            // --- End of Fix ---

                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration Failed: " +
                                    (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
