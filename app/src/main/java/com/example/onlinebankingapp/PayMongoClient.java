package com.example.onlinebankingapp;

import android.util.Base64;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

// PayMongoClient.java
public class PayMongoClient {
    private static final String BASE_URL = "https://api.paymongo.com/v1/";
    private static final String API_KEY = "sk_test_E2kCYM7eMKbGibvGFACfUgyJ";

    public static PayMongoService createService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        return retrofit.create(PayMongoService.class);
    }

    public static Map<String, String> getHeaders() {
        Map<String, String> headers = new HashMap<>();
        String authValue = "Basic " + Base64.encodeToString((API_KEY + ":").getBytes(), Base64.NO_WRAP);
        headers.put("Authorization", authValue);
        headers.put("Content-Type", "application/json");
        return headers;
    }
}

