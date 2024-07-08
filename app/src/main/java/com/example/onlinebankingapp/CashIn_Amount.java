package com.example.onlinebankingapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.paypal.android.sdk.payments.PayPalConfiguration;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.paymaya.sdk.android.checkout.PayMayaCheckout;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CashIn_Amount extends AppCompatActivity {
    private static final String TAG = "CashIn_Amount";
    private static final String PAYPAL_CLIENT_ID = "AdTjENItd5WmMbxFepwjArvMiY8R6VnnQQP1mR0-cRsissIVE3fEUi7YJkQYeyGrHnczZ8M9-4uEk0NP";
    private static final int PAYPAL_REQUEST_CODE = 123;
    private static PayPalConfiguration config = new PayPalConfiguration()
            .environment(PayPalConfiguration.ENVIRONMENT_SANDBOX)
            .clientId(PAYPAL_CLIENT_ID);

    private String referenceNumber;
    UserManager userManager;
    EditText amount;
    TextView currentBalanceText, paymentMethod, cashInAmount, cashInChargeAmount, balanceAmount;
    Button amount100, amount500, amount1000, amount2000, payNowButton;
    private PayMongoService payMongoService;
    private String currentBalance = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Initializing CashIn_Amount activity.");

        EdgeToEdge.enable(this);
        setContentView(R.layout.cash_in_amount);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        userManager = UserManager.getInstance();
        loadUserData();

        String pMethod = getIntent().getStringExtra("paymentMethod");

        balanceAmount = findViewById(R.id.balanceAmount);
        amount = findViewById(R.id.amount);
        currentBalanceText = findViewById(R.id.currentBalanceText);
        paymentMethod = findViewById(R.id.paymentMethod);
        cashInAmount = findViewById(R.id.cashInAmount);
        cashInChargeAmount = findViewById(R.id.cashInChargeAmount);
        amount100 = findViewById(R.id.amount100);
        amount500 = findViewById(R.id.amount500);
        amount1000 = findViewById(R.id.amount1000);
        amount2000 = findViewById(R.id.amount2000);
        payNowButton = findViewById(R.id.payNowButton);

        payNowButton.setEnabled(false);

        amount100.setOnClickListener(v -> setAmounts("100.00"));
        amount500.setOnClickListener(v -> setAmounts("500.00"));
        amount1000.setOnClickListener(v -> setAmounts("1000.00"));
        amount2000.setOnClickListener(v -> setAmounts("2000.00"));

        payNowButton.setOnClickListener(v -> {
            Log.d(TAG, "onClick: Pay Now button clicked.");
            String amountText = amount.getText().toString();
            if (!amountText.isEmpty()) {
                double amountValue = Double.parseDouble(amountText);
                Log.d(TAG, "Pay Now with amount: " + amountValue);
                if (amountValue >= 1) {
                    switch (pMethod) {
                        case "Debit/Credit Card":
                            startCardPayment();
                            break;
                        case "Paypal":
                            startPayPalPayment();
                            break;
                        case "GCash":
                            startGCashPayment();
                            break;
                        case "Maya":
                            startMayaPayment();
                            break;
                        case "DOB":
                            startDOBPayment();
                            break;
                        case "BDO":
                            startBDOPayment();
                            break;
                        case "LandBank":
                            startLandBankPayment();
                            break;
                        case "GrabPay":
                            startGrabPayPayment();
                            break;
                        case "BillEase":
                            startBillEasePayment();
                            break;
                        case "DOBUBP":
                            startDOBUBPPayment();
                            break;
                        case "MetroBank":
                            startMetroBankPayment();
                            break;
                        default:
                            Log.w(TAG, "Unknown payment method: " + pMethod);
                            break;
                    }
                } else {
                    Utility.showToast(CashIn_Amount.this, "Amount must be at least ₱1.00.");
                }
            } else {
                Utility.showToast(CashIn_Amount.this, "Please enter an amount.");
            }
        });

        paymentMethod.setText(pMethod);

        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                String amountText = s.toString();
                cashInAmount.setText(amountText);
                cashInChargeAmount.setText(amountText);

                boolean isNotEmpty = !amountText.isEmpty();
                payNowButton.setEnabled(isNotEmpty);
                if (isNotEmpty) {
                    payNowButton.setBackgroundTintList(ContextCompat.getColorStateList(CashIn_Amount.this, R.color.darkpink));
                } else {
                    payNowButton.setBackgroundResource(0);
                }
            }
        });
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
                                if (accountData.exists() && "wallet".equals(accountData.getString("accountType"))) {
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
                currentBalanceText.setText("Error");
            }
        });
    }
    private void setAmounts(String value) {
        Log.d(TAG, "setAmounts: Setting amount to " + value);
        amount.setText(value);
        cashInAmount.setText(value);
        cashInChargeAmount.setText(value);
    }

    private void startPayPalPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        PayPalPayment payment = new PayPalPayment(new BigDecimal(amountValue), "PHP",
                "Cash In", PayPalPayment.PAYMENT_INTENT_SALE);

        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra(PayPalService.EXTRA_PAYPAL_CONFIGURATION, config);
        intent.putExtra(PaymentActivity.EXTRA_PAYMENT, payment);

        Log.d(TAG, "startPayPalPayment: Starting PayPal Payment with: " + payment.toJSONObject().toString());
        startActivityForResult(intent, PAYPAL_REQUEST_CODE);
    }
    private void startCardPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "card");
    }
    private void startGCashPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "gcash");
        updateUserBalance(amount.getText().toString());
    }
    private void startMayaPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "paymaya");
    }
    private void startGrabPayPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "grab_pay");
    }
    private void startMetroBankPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "brankas_metrobank");
    }
    private void startLandBankPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "brankas_landbank");
    }
    private void startBDOPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "brankas_bdo");
    }
    private void startBillEasePayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "billease");
    }
    private void startDOBPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "dob");
    }
    private void startDOBUBPPayment() {
        double amountValue = Double.parseDouble(amount.getText().toString());
        createPayMongoCheckoutSession(amountValue, "dob_ubp");
    }
    private void createPayMongoCheckoutSession(double amount, String paymentMethod) {
        String baseUrl = "https://api.paymongo.com/";
        String apiKey = "sk_test_E2kCYM7eMKbGbvfGAFCxUgyJ";
        String referenceNumber = UUID.randomUUID().toString();

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Basic " + Base64.encodeToString((apiKey + ":").getBytes(), Base64.NO_WRAP));
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();

        Gson gson = new GsonBuilder().setLenient().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        PayMongoService service = retrofit.create(PayMongoService.class);

        PayMongoRequest request = new PayMongoRequest();
        request.data = new PayMongoRequest.Data();
        request.data.attributes = new PayMongoRequest.Attributes();
        PayMongoRequest.LineItem lineItem = new PayMongoRequest.LineItem();
        lineItem.amount = (int) (amount * 100);
        lineItem.currency = "PHP";
        lineItem.description = "Cash in amount";
        lineItem.name = "Cash In";
        lineItem.quantity = 1;

        request.data.attributes.lineItems = Collections.singletonList(lineItem);
        request.data.attributes.paymentMethodTypes = Collections.singletonList(paymentMethod);
        request.data.attributes.referenceNumber = referenceNumber;

        request.data.attributes.redirect = new PayMongoRequest.Redirect();
        request.data.attributes.redirect.success_url = "http://192.168.100.10:5000/payment/success.html";
        request.data.attributes.redirect.failed_url = "http://192.168.100.10:5000/payment/failed.html";


        service.createCheckoutSession(request).enqueue(new Callback<PayMongoResponse>() {
            @Override
            public void onResponse(Call<PayMongoResponse> call, Response<PayMongoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String checkoutUrl = response.body().data.attributes.checkoutUrl;
                    Log.d(TAG, "Checkout URL: " + checkoutUrl);
                    runOnUiThread(() -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
                        startActivity(browserIntent);
                    });
                } else {
                    try {
                        if (response.errorBody() != null) {
                            Log.e(TAG, "PayMongo request not successful: " + response.message() + ", Error Body: " + response.errorBody().string());
                        } else {
                            Log.e(TAG, "PayMongo request not successful: " + response.message());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                }
            }
            @Override
            public void onFailure(Call<PayMongoResponse> call, Throwable t) {
                Log.e(TAG, "Error creating PayMongo checkout session", t);
            }
        });
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(this, PayPalService.class));
        Log.d(TAG, "onDestroy: Stopping PayPal service");
        super.onDestroy();
    }

    private void updateUserBalance(String referenceNumber) {
        double amountValue = Double.parseDouble(amount.getText().toString());
        Log.d(TAG, "updateUserBalance: Updating user balance with amount: " + amount);
        userManager.getUserData(new UserManager.UserDataCallback() {
            @Override
            public void onDataLoaded(Map<String, Object> userData) {
                if (userData != null && userData.containsKey("accounts")) {
                    Map<String, Boolean> accounts = (Map<String, Boolean>) userData.get("accounts");
                    for (String accountId : accounts.keySet()) {
                        userManager.getAccountData(accountId, new UserManager.AccountDataCallback() {
                            @Override
                            public void onAccountLoaded(DocumentSnapshot accountData) {
                                if (accountData.exists() && "wallet".equals(accountData.getString("accountType"))) {
                                    Double currentBalance = accountData.getDouble("balance");
                                    if (currentBalance == null) {
                                        currentBalance = 0.0;
                                    }
                                    double newBalance = currentBalance + amountValue;
                                    Log.d(TAG, "onAccountLoaded: New balance calculated: " + newBalance);

                                    String transactionId = userManager.getNewTransactionId();
                                    Map<String, Object> transaction = new HashMap<>();
                                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
                                    long currentTimeMillis = calendar.getTimeInMillis();
                                    transaction.put("amount", +amountValue);
                                    transaction.put("dateTime", currentTimeMillis);
                                    transaction.put("type", "Cash in");
                                    transaction.put("description", "via " + paymentMethod.getText().toString());
                                    transaction.put("reference", referenceNumber);

                                    userManager.addTransaction(transactionId, transaction)
                                            .addOnSuccessListener(aVoid -> {
                                                accountData.getReference().update("transactions", FieldValue.arrayUnion(transactionId))
                                                        .addOnSuccessListener(aVoid1 -> {
                                                            accountData.getReference().update("balance", newBalance)
                                                                    .addOnSuccessListener(aVoid2 -> {
                                                                        Log.d(TAG, "onAccountLoaded: Balance and transactions updated successfully");
                                                                        Utility.showToast(CashIn_Amount.this, "Cash in successful");
                                                                        Intent intent = new Intent(CashIn_Amount.this, ReceiptActivity.class);

                                                                        intent.putExtra("titlePage", "Cash in");
                                                                        intent.putExtra("amount", amountValue);
                                                                        intent.putExtra("dateTime", currentTimeMillis);
                                                                        intent.putExtra("description", paymentMethod.getText().toString());
                                                                        intent.putExtra("reference", referenceNumber);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    })
                                                                    .addOnFailureListener(e -> {
                                                                        Log.e(TAG, "onAccountLoaded: Failed to update balance", e);
                                                                        Utility.showToast(CashIn_Amount.this, "Failed to update balance: " + e.getMessage());
                                                                    });
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e(TAG, "onAccountLoaded: Failed to add transaction ID to account", e);
                                                            Utility.showToast(CashIn_Amount.this, "Failed to add transaction ID to account: " + e.getMessage());
                                                        });
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "onAccountLoaded: Failed to add transaction", e);
                                                Utility.showToast(CashIn_Amount.this, "Failed to add transaction: " + e.getMessage());
                                            });
                                }
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "onAccountLoaded: Failed to get account data", e);
                                Utility.showToast(CashIn_Amount.this, "Failed to get account data: " + e.getMessage());
                            }
                        });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "onDataLoaded: Failed to get user data", e);
                Utility.showToast(CashIn_Amount.this, "Failed to get user data: " + e.getMessage());
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PAYPAL_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                PaymentConfirmation confirm = data.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
                if (confirm != null) {
                    try {
                        String paymentDetails = confirm.toJSONObject().toString(4);
                        Log.i(TAG, "PaymentConfirmation info received from PayPal: " + paymentDetails);
                        updateUserBalance(referenceNumber);
                    } catch (JSONException e) {
                        Log.e(TAG, "an extremely unlikely failure occurred: ", e);
                    }
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Log.i(TAG, "The user canceled.");
            } else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
                Log.i(TAG, "An invalid Payment or PayPalConfiguration was submitted.");
            }
        }
    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            String host = uri.getHost();
            if ("success".equals(host)) {
                Log.i(TAG, "Payment success callback received");
                updateUserBalance(referenceNumber);
            } else if ("failed".equals(host)) {
                Log.i(TAG, "Payment failed callback received");
                Utility.showToast(CashIn_Amount.this, "Payment failed");
            }
        }
    }
}
