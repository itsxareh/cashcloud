package com.example.onlinebankingapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Map;

public class AccountRecovery extends AppCompatActivity {
    EditText recoveryEmail;
    TextView verificationStatus;
    Button sendVerificationButton, changeEmailButton;
    UserManager userManager;
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

        userManager = UserManager.getInstance();
        recoveryEmail = findViewById(R.id.recoveryEmail);
        verificationStatus = findViewById(R.id.verificationStatus);
        sendVerificationButton = findViewById(R.id.sendVerificationButton);
        changeEmailButton = findViewById(R.id.changeEmailButton);

        loadUserData();
    }
    private void loadUserData() {
        userManager.getUserData(new UserManager.UserDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> userData) {
                if (userData != null && userData.containsKey("email")) {
                    String email = userData.get("email").toString();
                    recoveryEmail.setText(email);
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
}