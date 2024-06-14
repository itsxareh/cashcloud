package com.example.onlinebankingapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
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

public class ProfileMenuActivity extends AppCompatActivity {
    TextView profileName, profilePhoneNumber;
    Button quickGuideMenu, favoritesMenu, settingsMenu;
    UserManager userManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_menu);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        userManager = UserManager.getInstance();
        quickGuideMenu = findViewById(R.id.quickGuideMenu);
        favoritesMenu = findViewById(R.id.favoritesMenu);
        settingsMenu = findViewById(R.id.settingsMenu);

        settingsMenu.setOnClickListener(v -> startActivity(new Intent(ProfileMenuActivity.this, SettingsActivity.class)));

        profileName = findViewById(R.id.profileName);
        profilePhoneNumber = findViewById(R.id.profilePhoneNumber);

        loadUserData();
    }
    private void loadUserData() {
        userManager.getUserData(new UserManager.UserDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> userData) {
                if (userData != null && userData.containsKey("fullName") && userData.containsKey("phoneNumber") && userData.containsKey("countryCode")) {
                    String userName = userData.get("fullName").toString();
                    String fullPhoneNumber = userData.get("countryCode").toString() + userData.get("phoneNumber").toString();
                    profileName.setText(userName);
                    profilePhoneNumber.setText(fullPhoneNumber);
                } else {
                    profileName.setText("Unknown User");
                    profilePhoneNumber.setText(null);
                }
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileMenuActivity.this, "Failed to load user data", Toast.LENGTH_SHORT).show();
                profileName.setText("Error");
                profilePhoneNumber.setText("Error");
            }
        });
    }
}