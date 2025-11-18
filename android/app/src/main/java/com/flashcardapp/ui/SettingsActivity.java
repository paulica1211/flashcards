package com.flashcardapp.ui;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.flashcardapp.R;
import com.flashcardapp.models.SheetList;
import com.flashcardapp.models.StartingInfo;
import com.flashcardapp.network.ApiClient;
import com.flashcardapp.network.QuizApiService;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "FlashcardPreferences";
    private static final String KEY_SHEET_NAME = "selected_sheet_name";
    private static final String KEY_CARD_NUMBER = "current_card_number";

    private TextView currentSheetText;
    private TextView currentCardText;
    private MaterialButton selectSheetButton;
    private MaterialButton resetProgressButton;
    private SharedPreferences prefs;
    private QuizApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        apiService = ApiClient.getApiService();

        initializeViews();
        updateCurrentSheetDisplay();
        setupListeners();
    }

    private void initializeViews() {
        currentSheetText = findViewById(R.id.currentSheetText);
        currentCardText = findViewById(R.id.currentCardText);
        selectSheetButton = findViewById(R.id.selectSheetButton);
        resetProgressButton = findViewById(R.id.resetProgressButton);
    }

    private void updateCurrentSheetDisplay() {
        String currentSheet = prefs.getString(KEY_SHEET_NAME, "Sheet1");
        currentSheetText.setText("Current: " + currentSheet);

        // Fetch and display current card number from Google Sheets
        fetchCurrentProgress(currentSheet);
    }

    private void fetchCurrentProgress(String sheetName) {
        currentCardText.setText("Progress: Loading...");

        apiService.getFlashcardStartingInfo("getFlashcardStartingInfo", sheetName).enqueue(new Callback<StartingInfo>() {
            @Override
            public void onResponse(Call<StartingInfo> call, Response<StartingInfo> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int cardNumber = response.body().getQuestionNumber();
                    currentCardText.setText("Progress: Card " + cardNumber);
                    // Update SharedPreferences with the correct card number
                    prefs.edit().putInt(KEY_CARD_NUMBER, cardNumber).apply();
                    Log.d(TAG, "Loaded progress for " + sheetName + ": Card " + cardNumber);
                } else {
                    // Fall back to SharedPreferences
                    int localCard = prefs.getInt(KEY_CARD_NUMBER, 1);
                    currentCardText.setText("Progress: Card " + localCard);
                    Log.w(TAG, "Failed to load progress from API, using local: Card " + localCard);
                }
            }

            @Override
            public void onFailure(Call<StartingInfo> call, Throwable t) {
                // Fall back to SharedPreferences
                int localCard = prefs.getInt(KEY_CARD_NUMBER, 1);
                currentCardText.setText("Progress: Card " + localCard);
                Log.e(TAG, "Error loading progress, using local: Card " + localCard, t);
            }
        });
    }

    private void setupListeners() {
        selectSheetButton.setOnClickListener(v -> fetchAndShowSheetSelection());
        resetProgressButton.setOnClickListener(v -> resetProgress());
    }

    private void fetchAndShowSheetSelection() {
        selectSheetButton.setEnabled(false);
        selectSheetButton.setText("Loading...");

        Call<SheetList> call = apiService.getAvailableSheets("getAvailableSheets");
        call.enqueue(new Callback<SheetList>() {
            @Override
            public void onResponse(Call<SheetList> call, Response<SheetList> response) {
                selectSheetButton.setEnabled(true);
                selectSheetButton.setText("Choose Sheet");

                if (response.isSuccessful() && response.body() != null) {
                    List<String> sheets = response.body().getSheets();
                    if (sheets != null && !sheets.isEmpty()) {
                        showSheetSelectionDialog(sheets);
                    } else {
                        Toast.makeText(SettingsActivity.this, "No sheets found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SettingsActivity.this, "Failed to load sheets", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SheetList> call, Throwable t) {
                selectSheetButton.setEnabled(true);
                selectSheetButton.setText("Choose Sheet");
                Log.e(TAG, "Failed to fetch sheets", t);
                Toast.makeText(SettingsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSheetSelectionDialog(List<String> sheets) {
        String currentSheet = prefs.getString(KEY_SHEET_NAME, "Sheet1");
        String[] sheetArray = sheets.toArray(new String[0]);

        // Find currently selected sheet index
        int selectedIndex = 0;
        for (int i = 0; i < sheetArray.length; i++) {
            if (sheetArray[i].equals(currentSheet)) {
                selectedIndex = i;
                break;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Sheet")
                .setSingleChoiceItems(sheetArray, selectedIndex, (dialog, which) -> {
                    String selectedSheet = sheetArray[which];
                    prefs.edit()
                            .putString(KEY_SHEET_NAME, selectedSheet)
                            .apply();
                    updateCurrentSheetDisplay(); // This will fetch the correct card number for the new sheet
                    Toast.makeText(SettingsActivity.this, "Sheet changed to: " + selectedSheet, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resetProgress() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Progress")
                .setMessage("Are you sure you want to reset to Card 1?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    prefs.edit().putInt(KEY_CARD_NUMBER, 1).apply();
                    currentCardText.setText("Progress: Card 1");

                    // Save to Google Sheets
                    String sheetName = prefs.getString(KEY_SHEET_NAME, "Sheet1");
                    apiService.saveProgress("saveProgress", 1, sheetName)
                            .enqueue(new Callback<com.google.gson.JsonObject>() {
                                @Override
                                public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                                    if (response.isSuccessful()) {
                                        Log.d(TAG, "Progress reset saved to Google Sheets");
                                    }
                                }

                                @Override
                                public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                                    Log.e(TAG, "Failed to save reset to Google Sheets", t);
                                }
                            });

                    Toast.makeText(this, "Progress reset to Card 1", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
