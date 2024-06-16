package com.example.onlinebankingapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

public class AccountRecovery extends AppCompatActivity {
    EditText recoveryEmail;
    TextView verificationStatus;
    Button sendVerificationButton, changeEmailButton;
    UserManager userManager;
    private FirebaseAuth mAuth;

    private String userEmail;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account_recovery);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        userManager = UserManager.getInstance();
        recoveryEmail = findViewById(R.id.recoveryEmail);
        verificationStatus = findViewById(R.id.verificationStatus);
        sendVerificationButton = findViewById(R.id.sendVerificationButton);
        changeEmailButton = findViewById(R.id.changeEmailButton);

        loadUserData();

        sendVerificationButton.setOnClickListener(v -> sendEmailVerification());

    }
    private void loadUserData() {
        userManager.getUserData(new UserManager.UserDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> userData) {
                if (userData != null && userData.containsKey("email")) {
                    userEmail = userData.get("email").toString();
                    recoveryEmail.setText(userEmail);
                } else {
                    recoveryEmail.setText("Unknown User");
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(AccountRecovery.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                recoveryEmail.setError("Error");
            }
        });
    }
    private void sendEmailVerification() {
        if (userEmail != null) {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updateEmail(userEmail).addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        user.sendEmailVerification().addOnCompleteListener(this, verificationTask -> {
                            if (verificationTask.isSuccessful()) {
                                verificationStatus.setText("Verification email sent to " + userEmail);
                                Toast.makeText(AccountRecovery.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                            } else {
                                verificationStatus.setText("Failed to send verification email.");
                                Toast.makeText(AccountRecovery.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        verificationStatus.setText("Failed to update email.");
                        Toast.makeText(AccountRecovery.this, "Failed to update email.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "No authenticated user found.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No email found for user.", Toast.LENGTH_SHORT).show();
        }
    }
}