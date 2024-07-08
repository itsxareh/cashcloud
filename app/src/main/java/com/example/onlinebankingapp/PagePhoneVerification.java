package com.example.onlinebankingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.mindrot.jbcrypt.BCrypt;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PagePhoneVerification extends Fragment {
    private EditText otpCode;
    private TextView resendText, phoneNumberText, countryCodeText;
    private Button verifyButton;
    Long timeoutSeconds = 60L;
    ProgressBar progressBar;
    private String email, fullName, password, phoneNumber, countryCode, verificationId;
    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            fullName = getArguments().getString("fullName");
            email = getArguments().getString("email");
            password = getArguments().getString("password");
            phoneNumber = getArguments().getString("phoneNumber");
            countryCode = getArguments().getString("countryCode");
            verificationId = getArguments().getString("verificationId");
            resendingToken = getArguments().getParcelable("resendingToken");
        }

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                signInWithPhoneAuthCredential(credential);
                setInProgress(false);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Toast.makeText(getActivity(), "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                setInProgress(false);
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                PagePhoneVerification.this.verificationId = verificationId;
                PagePhoneVerification.this.resendingToken = forceResendingToken;
                setInProgress(false);
            }
        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_phone_verification_page, container, false);

        otpCode = rootView.findViewById(R.id.otpCode);
        verifyButton = rootView.findViewById(R.id.verifyButton);
        resendText = rootView.findViewById(R.id.resendButton);
        phoneNumberText = rootView.findViewById(R.id.phoneNumber);
        countryCodeText = rootView.findViewById(R.id.areaCode);
        progressBar = rootView.findViewById(R.id.progressBar);

        phoneNumberText.setText(phoneNumber);
        countryCodeText.setText(countryCode);
        verifyButton.setOnClickListener(v -> verifyCode());
        resendText.setOnClickListener(v -> resendVerificationCode());

        db = FirebaseFirestore.getInstance();
        return rootView;
    }

    private void verifyCode() {
        String code = otpCode.getText().toString().trim();
        if (code.length() != 6) {
            otpCode.setError("Enter valid code...");
            otpCode.requestFocus();
            return;
        }

        verifyVerificationCode(code);
    }

    private void verifyVerificationCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
        setInProgress(true);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        setInProgress(true);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    setInProgress(false);
                    if (task.isSuccessful()) {
                        String userUID = task.getResult().getUser().getUid();
                        linkPhoneNumberToEmailAccount(credential, userUID);
                    } else {
                        String message = "Something went wrong, please try again later.";
                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                        }
                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void linkPhoneNumberToEmailAccount(PhoneAuthCredential credential, String userUID) {
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkAndSaveUserData(userUID);
                    } else {
                        String errorMessage = "Failed to link phone number.";
                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }
                        if (errorMessage.contains("already been linked")) {
                            checkAndSaveUserData(mAuth.getCurrentUser().getUid());
                        } else {
                            Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void checkAndSaveUserData(String userUID) {
        DocumentReference userRef = db.collection("users").document(userUID);

        userRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                navigateToMainActivity(userUID);
            } else {
                saveUserData(userUID);
            }
        });
    }

    private void saveUserData(String userUID) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> user = new HashMap<>();
                        user.put("fullName", fullName);
                        user.put("email", email);
                        user.put("countryCode", countryCode);
                        user.put("phoneNumber", phoneNumber);
                        user.put("isEmailVerified", false);
                        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                        user.put("password", hashedPassword);
                        user.put("accounts", new HashMap<String, Boolean>());

                        db.collection("users")
                                .document(userUID)
                                .set(user)
                                .addOnSuccessListener(aVoid -> {
                                    createDefaultAccounts(userUID);
                                })
                                .addOnFailureListener(e -> {
                                    Utility.showToast(getActivity(), "Failed to save user information: " + e.getMessage());
                                });
                    } else {
                        Utility.showToast(getActivity(), "Failed to create user with email: " + task.getException().getMessage());
                    }
                });
    }

    private void createDefaultAccounts(String userUID) {
        CollectionReference accountsCollection = db.collection("accounts");

        Map<String, Object> walletAccount = new HashMap<>();
        walletAccount.put("userId", userUID);
        walletAccount.put("accountType", "wallet");
        walletAccount.put("balance", 0.00);
        walletAccount.put("transactions", new HashMap<String, Boolean>());

        Map<String, Object> savingsAccount = new HashMap<>();
        savingsAccount.put("userId", userUID);
        savingsAccount.put("accountType", "savings");
        savingsAccount.put("balance", 0.00);
        savingsAccount.put("transactions", new HashMap<String, Boolean>());

        Task<DocumentReference> walletTask = accountsCollection.add(walletAccount);
        Task<DocumentReference> savingsTask = accountsCollection.add(savingsAccount);

        Tasks.whenAllSuccess(walletTask, savingsTask)
                .addOnSuccessListener(result -> {
                    String walletAccountId = walletTask.getResult().getId();
                    String savingsAccountId = savingsTask.getResult().getId();
                    updateUserAccountReferences(userUID, walletAccountId, savingsAccountId);
                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getActivity(), "Failed to create accounts: " + e.getMessage());
                });
    }

    private void updateUserAccountReferences(String userUID, String walletAccountId, String savingsAccountId) {
        DocumentReference userRef = db.collection("users").document(userUID);

        Map<String, Object> accountsUpdate = new HashMap<>();
        accountsUpdate.put("accounts." + walletAccountId, true);
        accountsUpdate.put("accounts." + savingsAccountId, true);

        userRef.update(accountsUpdate)
                .addOnSuccessListener(aVoid -> {
                    createOnlineCard(userUID);
                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getActivity(), "Failed to update user account references: " + e.getMessage());
                });
    }
    private void createOnlineCard(String userUID){
        CollectionReference cardCollection = db.collection("cards");

        Map<String, Object> cardAccount = new HashMap<>();
        cardAccount.put("userId", userUID);
        cardAccount.put("cardType", "online card");
        cardAccount.put("cardNumber", generateRandomCardNumber());
        cardAccount.put("expirationDate", generateExpirationDate());
        cardAccount.put("cvv", generateRandomCVV());
        cardAccount.put("limit", 50000);
        cardAccount.put("balance", 0.00);

        Task<DocumentReference> cardTask = cardCollection.add(cardAccount);
        Tasks.whenAll(cardTask)
                .addOnSuccessListener(result -> {
                    String cardAccountId = cardTask.getResult().getId();
                    updateUserCardReferences(userUID, cardAccountId);
                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getActivity(), "Failed to create card: " + e.getMessage());
                });
    }
    private String generateExpirationDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.YEAR, 2);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/yy");
        return dateFormat.format(calendar.getTime());
    }
    private String generateRandomCardNumber() {
        Random random = new Random();
        StringBuilder cardNumber = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            int digit = random.nextInt(10);
            cardNumber.append(digit);
        }
        return cardNumber.toString();
    }
    private String generateRandomCVV() {
        Random random = new Random();
        StringBuilder cvv = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            int digit = random.nextInt(10);
            cvv.append(digit);
        }
        return cvv.toString();
    }
    private void updateUserCardReferences(String userUID, String cardAccountId) {
        DocumentReference userRef = db.collection("users").document(userUID);

        Map<String, Object> accountsUpdate = new HashMap<>();
        accountsUpdate.put("cards." + cardAccountId, true);

        userRef.update(accountsUpdate)
                .addOnSuccessListener(aVoid -> {
                    navigateToMainActivity(userUID);
                })
                .addOnFailureListener(e -> {
                    Utility.showToast(getActivity(), "Failed to update user card references: " + e.getMessage());
                });
    }
    private void navigateToMainActivity(String userUID) {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("userId", userUID);
        startActivity(intent);
    }

    private void resendVerificationCode() {
        startResendTimer();
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(getActivity())
                .setCallbacks(mCallbacks)
                .setForceResendingToken(resendingToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    void startResendTimer() {
        resendText.setEnabled(false);
        new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsRemaining = millisUntilFinished / 1000;
                resendText.setText("Resend OTP in " + secondsRemaining + " seconds");
            }

            public void onFinish() {
                resendText.setEnabled(true);
            }
        }.start();
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            verifyButton.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            verifyButton.setVisibility(View.VISIBLE);
        }
    }
}
