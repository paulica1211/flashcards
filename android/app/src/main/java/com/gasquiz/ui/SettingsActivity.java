package com.gasquiz.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.gasquiz.R;
import com.gasquiz.models.QuestionNumberResponse;
import com.gasquiz.models.SheetNames;
import com.gasquiz.network.ApiClient;
import com.gasquiz.network.QuizApiService;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final String PREFS_NAME = "QuizPreferences";
    private static final String KEY_SHEET_NAME = "sheetName";
    private static final String KEY_QUESTION_NUMBER = "questionNumber";
    private static final String KEY_QUESTION_NUMBER_PREFIX = "questionNumber_";

    private QuizApiService apiService;
    private Spinner sheetNameSpinner;
    private EditText questionNumberInput;
    private TextView questionNumberHelper;
    private TextView statusText;
    private MaterialButton saveButton;
    private ProgressBar progressBar;

    private List<String> sheetNames;
    private String selectedSheetName;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize API service
        apiService = ApiClient.getApiService();
        preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        sheetNameSpinner = findViewById(R.id.sheetNameSpinner);
        questionNumberInput = findViewById(R.id.questionNumberInput);
        questionNumberHelper = findViewById(R.id.questionNumberHelper);
        statusText = findViewById(R.id.statusText);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);

        // Load current settings
        loadCurrentSettings();

        // Load sheet names from API
        loadSheetNames();

        // Setup spinner listener
        sheetNameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSheetName = sheetNames.get(position);
                Log.d(TAG, "Selected sheet: " + selectedSheetName);

                // Fetch and auto-fill the saved question number for this quiz set
                loadQuestionNumberForSheet(selectedSheetName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Setup save button
        saveButton.setOnClickListener(v -> saveSettings());
    }

    private void loadCurrentSettings() {
        String currentSheet = preferences.getString(KEY_SHEET_NAME, "特許");
        // Load question number for the current sheet
        int currentQuestion = preferences.getInt(KEY_QUESTION_NUMBER_PREFIX + currentSheet, 1);

        questionNumberInput.setText(String.valueOf(currentQuestion));
        questionNumberHelper.setText("Current: " + currentSheet + " - Question " + currentQuestion);
    }

    private void loadQuestionNumberForSheet(String sheetName) {
        // Try local storage first
        int localQuestionNumber = preferences.getInt(KEY_QUESTION_NUMBER_PREFIX + sheetName, -1);

        // Try API to get the latest from Google Sheets
        apiService.getQuestionNumberForSheet("getQuestionNumberForSheet", sheetName)
                .enqueue(new Callback<QuestionNumberResponse>() {
                    @Override
                    public void onResponse(Call<QuestionNumberResponse> call, Response<QuestionNumberResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            int questionNumber = response.body().getQuestionNumber();
                            // Use the maximum of local and server values (most recent progress)
                            int finalQuestionNumber = Math.max(questionNumber, localQuestionNumber > 0 ? localQuestionNumber : 1);
                            questionNumberInput.setText(String.valueOf(finalQuestionNumber));
                            questionNumberHelper.setText("Last position for " + sheetName + ": Question " + finalQuestionNumber);
                            Log.d(TAG, "Loaded question number for " + sheetName + ": " + finalQuestionNumber);
                        } else {
                            // API failed, use local or default
                            useDefaultQuestionNumber(sheetName, localQuestionNumber);
                        }
                    }

                    @Override
                    public void onFailure(Call<QuestionNumberResponse> call, Throwable t) {
                        Log.e(TAG, "Error loading question number for sheet", t);
                        // API failed, use local or default
                        useDefaultQuestionNumber(sheetName, localQuestionNumber);
                    }
                });
    }

    private void useDefaultQuestionNumber(String sheetName, int localQuestionNumber) {
        // Use local if available, otherwise default to 1
        int questionNumber = localQuestionNumber > 0 ? localQuestionNumber : 1;
        questionNumberInput.setText(String.valueOf(questionNumber));
        questionNumberHelper.setText("Select question for " + sheetName + " (using local: " + questionNumber + ")");
    }

    private void loadSheetNames() {
        showLoading(true);

        apiService.getSheetNames("getSheetNames").enqueue(new Callback<SheetNames>() {
            @Override
            public void onResponse(Call<SheetNames> call, Response<SheetNames> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    sheetNames = response.body().getSheetNames();
                    Log.d(TAG, "Loaded sheet names: " + sheetNames);

                    // Check if sheet names were actually retrieved
                    if (sheetNames != null && !sheetNames.isEmpty()) {
                        // Setup spinner adapter
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                SettingsActivity.this,
                                android.R.layout.simple_spinner_item,
                                sheetNames
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        sheetNameSpinner.setAdapter(adapter);

                        // Set current selection
                        String currentSheet = preferences.getString(KEY_SHEET_NAME, "特許");
                        int position = sheetNames.indexOf(currentSheet);
                        if (position >= 0) {
                            sheetNameSpinner.setSelection(position);
                        }
                        selectedSheetName = currentSheet;
                    } else {
                        // API returned success but no sheet names (old API version)
                        Log.w(TAG, "API returned no sheet names, using defaults");
                        useDefaultSheetNames();
                    }

                } else {
                    Log.e(TAG, "Failed to load sheet names");
                    Toast.makeText(SettingsActivity.this,
                            "Failed to load quiz sets", Toast.LENGTH_SHORT).show();

                    // Use default sheet names
                    useDefaultSheetNames();
                }
            }

            @Override
            public void onFailure(Call<SheetNames> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error loading sheet names", t);
                Toast.makeText(SettingsActivity.this,
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();

                // Use default sheet names
                useDefaultSheetNames();
            }
        });
    }

    private void useDefaultSheetNames() {
        // Default quiz sets
        sheetNames = new ArrayList<>();
        sheetNames.add("特許");
        sheetNames.add("意匠");
        sheetNames.add("商標");
        sheetNames.add("条約");
        sheetNames.add("著作");
        sheetNames.add("不競");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                sheetNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sheetNameSpinner.setAdapter(adapter);

        // Set current selection
        String currentSheet = preferences.getString(KEY_SHEET_NAME, "特許");
        int position = sheetNames.indexOf(currentSheet);
        if (position >= 0) {
            sheetNameSpinner.setSelection(position);
        }
        selectedSheetName = currentSheet;
    }

    private void saveSettings() {
        String questionNumberStr = questionNumberInput.getText().toString().trim();

        if (questionNumberStr.isEmpty()) {
            Toast.makeText(this, "Please enter a question number", Toast.LENGTH_SHORT).show();
            return;
        }

        int questionNumber;
        try {
            questionNumber = Integer.parseInt(questionNumberStr);
            if (questionNumber < 1) {
                Toast.makeText(this, "Question number must be at least 1", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid question number", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        statusText.setVisibility(View.GONE);

        // Save to SharedPreferences - both globally and per-sheet
        preferences.edit()
                .putString(KEY_SHEET_NAME, selectedSheetName)
                .putInt(KEY_QUESTION_NUMBER, questionNumber)
                .putInt(KEY_QUESTION_NUMBER_PREFIX + selectedSheetName, questionNumber)
                .apply();

        Log.d(TAG, "Saving settings: " + selectedSheetName + " - Q" + questionNumber);

        // Save to Google Sheets
        apiService.saveSettings("saveSettings", selectedSheetName, questionNumber)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        showLoading(false);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "Settings saved successfully");
                            statusText.setText("Settings saved successfully!");
                            statusText.setVisibility(View.VISIBLE);

                            Toast.makeText(SettingsActivity.this,
                                    "Settings saved! Restart app to apply.", Toast.LENGTH_LONG).show();

                            // Return to quiz activity and reload settings
                            Intent intent = new Intent(SettingsActivity.this, QuizActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            startActivity(intent);
                            finish();

                        } else {
                            Log.e(TAG, "Failed to save settings");
                            statusText.setText("Failed to save to server");
                            statusText.setVisibility(View.VISIBLE);
                            Toast.makeText(SettingsActivity.this,
                                    "Saved locally. Server update failed.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "Error saving settings", t);
                        statusText.setText("Network error");
                        statusText.setVisibility(View.VISIBLE);
                        Toast.makeText(SettingsActivity.this,
                                "Saved locally. Network error: " + t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
