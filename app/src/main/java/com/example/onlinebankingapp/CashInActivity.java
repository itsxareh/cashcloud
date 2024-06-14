package com.example.onlinebankingapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CashInActivity extends AppCompatActivity {

    Button cashCloudSavings, cashCloudCenter, bankAccount, debitCreditCard;
    ImageButton bpiButton, bdoButton, unionBankButton, chinaBankButton, landBankButton, islaBankButton,
    securityBankButton, metroBankButton, philtrustBankButton, paypalButton, gCashButton, venmoButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cash_in);

        paypalButton = findViewById(R.id.paypalButton);
        cashCloudSavings = findViewById(R.id.cashCloudSavings);
        cashCloudCenter = findViewById(R.id.cashCloudCenter);
        bankAccount = findViewById(R.id.bankAccount);
        debitCreditCard = findViewById(R.id.debitCreditCard);

        bpiButton = findViewById(R.id.bpiButton);
        bdoButton = findViewById(R.id.bdoButton);
        unionBankButton = findViewById(R.id.unionBankButton);
        chinaBankButton = findViewById(R.id.chinaBankButton);
        landBankButton = findViewById(R.id.landBankButton);
        securityBankButton = findViewById(R.id.securityBankButton);
        islaBankButton = findViewById(R.id.islaBankButton);
        metroBankButton = findViewById(R.id.metroBankButton);
        philtrustBankButton = findViewById(R.id.philtrustBankButton);

        gCashButton = findViewById(R.id.gCashButton);
        venmoButton = findViewById(R.id.venmoButton);

    }
}