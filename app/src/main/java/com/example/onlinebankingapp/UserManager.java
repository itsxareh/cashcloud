package com.example.onlinebankingapp;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Map;

public class UserManager {
    private static final String TAG = "UserManager";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private static UserManager instance;

    private UserManager() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    public interface UserDataCallback {
        void onDataLoaded(Map<String, Object> userData);
        void onFailure(Exception e);
    }

    public interface AccountDataCallback {
        void onAccountLoaded(DocumentSnapshot accountData);
        void onFailure(Exception e);
    }

    public interface TransactionsCallback {
        void onTransactionsLoaded(List<DocumentSnapshot> transactions);
        void onFailure(Exception e);
    }

    public void getUserData(final UserDataCallback callback) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                callback.onDataLoaded(document.getData());
                            } else {
                                Log.d(TAG, "No such document");
                                callback.onFailure(new Exception("No such document"));
                            }
                        } else {
                            Log.d(TAG, "get failed with ", task.getException());
                            callback.onFailure(task.getException());
                        }
                    });
        } else {
            callback.onFailure(new Exception("User not signed in"));
        }
    }

    public void getAccountData(String accountId, final AccountDataCallback callback) {
        db.collection("accounts").document(accountId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            callback.onAccountLoaded(document);
                        } else {
                            Log.d(TAG, "No such document");
                            callback.onFailure(new Exception("No such document"));
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }

    public void getUserTransactions(String accountId, final TransactionsCallback callback) {
        db.collection("transactions")
                .whereEqualTo("accountId", accountId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        callback.onTransactionsLoaded(querySnapshot.getDocuments());
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                        callback.onFailure(task.getException());
                    }
                });
    }
}