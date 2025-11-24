package com.flashcardapp.ui;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

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
    private static final String KEY_TTS_SPEED = "tts_speech_rate";
    private static final String KEY_TTS_AUTO_PLAY = "tts_auto_play";

    private EditText apiUrlEditText;
    private MaterialButton testConnectionButton;
    private MaterialButton saveApiUrlButton;
    private TextView currentSheetText;
    private TextView currentCardText;
    private MaterialButton selectSheetButton;
    private MaterialButton jumpToCardButton;
    private MaterialButton backToFlashcardsButton;
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedValueText;
    private SwitchCompat autoPlaySwitch;
    private MaterialButton testTtsButton;
    private SharedPreferences prefs;
    private QuizApiService apiService;
    private TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initializeViews();
        loadApiUrl();
        updateApiService();
        updateCurrentSheetDisplay();
        initializeTTS();
        loadTtsSettings();
        setupListeners();
    }

    private void initializeViews() {
        apiUrlEditText = findViewById(R.id.apiUrlEditText);
        testConnectionButton = findViewById(R.id.testConnectionButton);
        saveApiUrlButton = findViewById(R.id.saveApiUrlButton);
        currentSheetText = findViewById(R.id.currentSheetText);
        currentCardText = findViewById(R.id.currentCardText);
        selectSheetButton = findViewById(R.id.selectSheetButton);
        jumpToCardButton = findViewById(R.id.jumpToCardButton);
        backToFlashcardsButton = findViewById(R.id.backToFlashcardsButton);
        ttsSpeedSeekBar = findViewById(R.id.ttsSpeedSeekBar);
        ttsSpeedValueText = findViewById(R.id.ttsSpeedValueText);
        autoPlaySwitch = findViewById(R.id.autoPlaySwitch);
        testTtsButton = findViewById(R.id.testTtsButton);
    }

    private void loadApiUrl() {
        String currentUrl = ApiClient.getApiUrl(this);
        apiUrlEditText.setText(currentUrl);
    }

    private void updateApiService() {
        apiService = ApiClient.getApiService(this);
    }

    private void updateCurrentSheetDisplay() {
        String currentSheet = prefs.getString(KEY_SHEET_NAME, "Sheet1");
        currentSheetText.setText("Current: " + currentSheet);

        // Fetch and display current card number from Google Sheets
        fetchCurrentProgress(currentSheet);
    }

    private void fetchCurrentProgress(String sheetName) {
        if (apiService == null) {
            currentCardText.setText("Progress: API not configured");
            return;
        }

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
        saveApiUrlButton.setOnClickListener(v -> saveApiUrl());
        testConnectionButton.setOnClickListener(v -> testConnection());
        selectSheetButton.setOnClickListener(v -> fetchAndShowSheetSelection());
        jumpToCardButton.setOnClickListener(v -> showJumpToCardDialog());
        backToFlashcardsButton.setOnClickListener(v -> finish());

        // TTS speed SeekBar listener
        ttsSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = progressToSpeed(progress);
                ttsSpeedValueText.setText(String.format(Locale.US, "%.1fx", speed));

                // Apply to TTS engine
                if (textToSpeech != null) {
                    textToSpeech.setSpeechRate(speed);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Save to SharedPreferences
                float speed = progressToSpeed(seekBar.getProgress());
                prefs.edit().putFloat(KEY_TTS_SPEED, speed).apply();
                Log.d(TAG, "TTS speed saved: " + speed);
            }
        });

        testTtsButton.setOnClickListener(v -> testTtsVoice());

        // Auto-play switch listener
        autoPlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(KEY_TTS_AUTO_PLAY, isChecked).apply();
            Log.d(TAG, "Auto-play set to: " + isChecked);
        });
    }

    private void saveApiUrl() {
        String url = apiUrlEditText.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic validation
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save URL
        ApiClient.saveApiUrl(this, url);
        updateApiService();

        Toast.makeText(this, "API URL saved successfully", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "API URL saved: " + url);

        // Reload sheet display after URL change
        updateCurrentSheetDisplay();
    }

    private void testConnection() {
        String url = apiUrlEditText.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return;
        }

        // Temporarily save URL for testing
        ApiClient.saveApiUrl(this, url);
        updateApiService();

        if (apiService == null) {
            Toast.makeText(this, "Invalid URL configuration", Toast.LENGTH_SHORT).show();
            return;
        }

        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");

        // Try to fetch available sheets to test connection
        apiService.getAvailableSheets("getAvailableSheets").enqueue(new Callback<SheetList>() {
            @Override
            public void onResponse(Call<SheetList> call, Response<SheetList> response) {
                testConnectionButton.setEnabled(true);
                testConnectionButton.setText("Test");

                if (response.isSuccessful() && response.body() != null) {
                    List<String> sheets = response.body().getSheets();
                    if (sheets != null && !sheets.isEmpty()) {
                        Toast.makeText(SettingsActivity.this,
                            "✓ Connection successful! Found " + sheets.size() + " sheet(s)",
                            Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Connection test successful. Sheets: " + sheets);
                    } else {
                        Toast.makeText(SettingsActivity.this,
                            "Connection successful but no sheets found",
                            Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(SettingsActivity.this,
                        "✗ Connection failed: " + response.code(),
                        Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Connection test failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<SheetList> call, Throwable t) {
                testConnectionButton.setEnabled(true);
                testConnectionButton.setText("Test");
                Toast.makeText(SettingsActivity.this,
                    "✗ Connection error: " + t.getMessage(),
                    Toast.LENGTH_LONG).show();
                Log.e(TAG, "Connection test error", t);
            }
        });
    }

    private void fetchAndShowSheetSelection() {
        if (apiService == null) {
            Toast.makeText(this, "Please configure API URL first", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void showJumpToCardDialog() {
        // Get current card number
        int currentCard = prefs.getInt(KEY_CARD_NUMBER, 1);

        // Create EditText for number input
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Enter card number");
        input.setText(String.valueOf(currentCard));
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(this)
                .setTitle("Jump to Card")
                .setMessage("Enter the card number you want to jump to:")
                .setView(input)
                .setPositiveButton("Jump", (dialog, which) -> {
                    String inputText = input.getText().toString().trim();
                    if (inputText.isEmpty()) {
                        Toast.makeText(this, "Please enter a card number", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        int cardNumber = Integer.parseInt(inputText);
                        if (cardNumber < 1) {
                            Toast.makeText(this, "Card number must be at least 1", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Save to SharedPreferences
                        prefs.edit().putInt(KEY_CARD_NUMBER, cardNumber).apply();
                        currentCardText.setText("Progress: Card " + cardNumber);

                        // Save to Google Sheets (if API is configured)
                        if (apiService != null) {
                            String sheetName = prefs.getString(KEY_SHEET_NAME, "Sheet1");
                            apiService.saveProgress("saveProgress", cardNumber, sheetName)
                                    .enqueue(new Callback<com.google.gson.JsonObject>() {
                                        @Override
                                        public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                                            if (response.isSuccessful()) {
                                                Log.d(TAG, "Jump to card saved to Google Sheets");
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                                            Log.e(TAG, "Failed to save jump to Google Sheets", t);
                                        }
                                    });
                        }

                        Toast.makeText(this, "Jumped to Card " + cardNumber, Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid card number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==================== TTS Methods ====================

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.JAPANESE);
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech.setLanguage(Locale.ENGLISH);
                }
                testTtsButton.setEnabled(true);
                Log.d(TAG, "TTS initialized in Settings");
            } else {
                testTtsButton.setEnabled(false);
                Log.e(TAG, "TTS initialization failed in Settings");
            }
        });
    }

    private void loadTtsSettings() {
        float savedSpeed = prefs.getFloat(KEY_TTS_SPEED, 1.0f);
        int progress = speedToProgress(savedSpeed);
        ttsSpeedSeekBar.setProgress(progress);
        ttsSpeedValueText.setText(String.format(Locale.US, "%.1fx", savedSpeed));

        if (textToSpeech != null) {
            textToSpeech.setSpeechRate(savedSpeed);
        }

        boolean autoPlay = prefs.getBoolean(KEY_TTS_AUTO_PLAY, false);
        autoPlaySwitch.setChecked(autoPlay);

        Log.d(TAG, "Loaded TTS speed: " + savedSpeed + ", Auto-play: " + autoPlay);
    }

    private float progressToSpeed(int progress) {
        // Progress: 0-150 -> Speed: 0.5-2.0
        // 0 -> 0.5, 50 -> 1.0, 150 -> 2.0
        return 0.5f + (progress / 100.0f);
    }

    private int speedToProgress(float speed) {
        // Speed: 0.5-2.0 -> Progress: 0-150
        return (int) ((speed - 0.5f) * 100);
    }

    private void testTtsVoice() {
        if (textToSpeech == null) {
            Toast.makeText(this, "TTS not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String testText = "こんにちは。これはテストです。Hello, this is a test.";
        textToSpeech.speak(testText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
