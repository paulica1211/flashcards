package com.flashcardapp.network;

import android.content.Context;
import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String PREFS_NAME = "FlashcardPreferences";
    private static final String KEY_API_URL = "api_base_url";
    private static final String DEFAULT_URL = "https://script.google.com/macros/s/YOUR_DEPLOYMENT_ID/exec";

    private static Retrofit retrofit = null;
    private static String currentBaseUrl = null;

    public static Retrofit getClient(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(KEY_API_URL, null);

        // If no URL is configured, return null (caller should handle)
        if (baseUrl == null || baseUrl.isEmpty()) {
            return null;
        }

        // Ensure URL ends with /
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }

        // Recreate retrofit if URL changed or not initialized
        if (retrofit == null || !baseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = baseUrl;

            // Logging interceptor for debugging
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // HTTP client with timeouts
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static QuizApiService getApiService(Context context) {
        Retrofit client = getClient(context);
        if (client == null) {
            return null;
        }
        return client.create(QuizApiService.class);
    }

    // Check if API URL is configured
    public static boolean isConfigured(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String baseUrl = prefs.getString(KEY_API_URL, null);
        return baseUrl != null && !baseUrl.isEmpty();
    }

    // Save API URL
    public static void saveApiUrl(Context context, String url) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_API_URL, url).apply();
        // Reset retrofit to force recreation with new URL
        retrofit = null;
        currentBaseUrl = null;
    }

    // Get current API URL
    public static String getApiUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_API_URL, "");
    }
}
