package com.example.onlinebankingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavingsFragment extends Fragment {
    Button startSavingButton;
    ImageButton showBalanceButton;
    TextView balanceAmount, balanceText;
    UserManager userManager;
    private boolean isBalanceVisible = true;
    private String currentBalance = "";
    public SavingsFragment () {
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.savings_fragment, container, false);

        showBalanceButton = rootView.findViewById(R.id.showBalanceButton);
        startSavingButton = rootView.findViewById(R.id.startSavingButton);
        balanceAmount = rootView.findViewById(R.id.balanceAmount);
        balanceText = rootView.findViewById(R.id.savingsText);
        userManager = UserManager.getInstance();

        loadUserData();
        showBalanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBalanceVisible) {
                    balanceAmount.setText("••••••••");
                    showBalanceButton.setImageResource(R.drawable.baseline_visibility_off_24);
                } else {
                    balanceAmount.setText(currentBalance);
                    showBalanceButton.setImageResource(R.drawable.baseline_remove_red_eye_24);
                }
                isBalanceVisible = !isBalanceVisible;
            }
        });
        return rootView;
    }
    private void loadUserData() {
        userManager.getUserData(new UserManager.UserDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> userData) {
                if (userData != null && userData.containsKey("accounts")) {
                    Map<String, Boolean> accounts = (Map<String, Boolean>) userData.get("accounts");
                    for (String accountId : accounts.keySet()) {
                        userManager.getAccountData(accountId, new UserManager.AccountDataCallback() {
                            @Override
                            public void onAccountLoaded(DocumentSnapshot accountData) {
                                if (accountData.exists() && "savings".equals(accountData.getString("accountType"))) {
                                    Double balance = accountData.getDouble("balance");
                                    if (balance != null) {
                                        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
                                        numberFormat.setMinimumFractionDigits(2);
                                        numberFormat.setMaximumFractionDigits(2);
                                        currentBalance = numberFormat.format(balance);
                                        balanceAmount.setText(currentBalance);
                                    } else {
                                        currentBalance = "0.00";
                                        balanceAmount.setText(currentBalance);
                                    }
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                balanceAmount.setText("Error");
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                balanceAmount.setText("Error");
            }
        });
    }
}