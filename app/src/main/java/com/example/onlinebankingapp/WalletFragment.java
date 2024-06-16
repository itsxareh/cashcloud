package com.example.onlinebankingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Map;

public class WalletFragment extends Fragment {

    Button cashInButton, sendButton;
    ImageButton bankServiceButton, cardsServiceButton, savingsServiceButton,
            stocksServiceButton, loadButton, billsButton, cryptoButton, moreButton;
    TextView balanceAmount, balanceText, seeAllButton;
    RecyclerView transactionRecyclerView;
    TransactionAdapter transactionAdapter;
    UserManager userManager;

    public WalletFragment () {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View rootView = inflater.inflate(R.layout.wallet_fragment, container, false);

        cashInButton = rootView.findViewById(R.id.cashInButton);
        sendButton = rootView.findViewById(R.id.sendButton);

        bankServiceButton = rootView.findViewById(R.id.bankServiceButton);
        cardsServiceButton = rootView.findViewById(R.id.cardsServiceButton);
        savingsServiceButton = rootView.findViewById(R.id.savingsServiceButton);
//        stocksServiceButton = rootView.findViewById(R.id.stocksServiceButton);
//        loadButton = rootView.findViewById(R.id.loadServiceButton);
//        billsButton = rootView.findViewById(R.id.billsServiceButton);
//        cryptoButton = rootView.findViewById(R.id.cryptoServiceButton);
//        moreButton = rootView.findViewById(R.id.moreServiceButton);

        balanceAmount = rootView.findViewById(R.id.balanceAmount);
        balanceText = rootView.findViewById(R.id.balanceText);
        seeAllButton = rootView.findViewById(R.id.seeAllButton);

        transactionRecyclerView = rootView.findViewById(R.id.transactionRecyclerView);
        transactionRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        cashInButton.setOnClickListener(v -> startActivity(new Intent(getActivity(), CashInActivity.class)));

        userManager = UserManager.getInstance();

        loadUserData();

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
                                if (accountData.exists() && "wallet".equals(accountData.getString("accountType"))) {
                                    Double balance = accountData.getDouble("balance");
                                    if (balance != null) {
                                        balanceAmount.setText(String.format("%.2f", balance));
                                    } else {
                                        balanceAmount.setText("0.00");
                                    }
                                    loadUserTransactions(accountId);
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


    private void loadUserTransactions(String accountId) {
        userManager.getUserTransactions(accountId, new UserManager.TransactionsCallback() {
            @Override
            public void onTransactionsLoaded(List<DocumentSnapshot> transactions) {
                transactionAdapter = new TransactionAdapter(transactions);
                transactionRecyclerView.setAdapter(transactionAdapter);
            }

            @Override
            public void onFailure(Exception e) {
            }
        });
    }

    private class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {
        private List<DocumentSnapshot> transactions;

        public TransactionAdapter(List<DocumentSnapshot> transactions) {
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DocumentSnapshot transaction = transactions.get(position);
            holder.bind(transaction);
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView transactionType, transactionDate, transactionAmount, transactionDescription;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                transactionType = itemView.findViewById(R.id.transactionType);
                transactionDate = itemView.findViewById(R.id.transactionDate);
                transactionAmount = itemView.findViewById(R.id.transactionAmount);
                transactionDescription = itemView.findViewById(R.id.transactionDescription);
            }

            public void bind(DocumentSnapshot transaction) {
                Map<String, Object> data = transaction.getData();
                if (data != null) {
                    transactionType.setText(data.get("type").toString());
                    transactionDate.setText(data.get("date").toString());
                    transactionAmount.setText(data.get("amount").toString());
                    transactionDescription.setText(data.get("description").toString());
                }
            }
        }
    }
}
