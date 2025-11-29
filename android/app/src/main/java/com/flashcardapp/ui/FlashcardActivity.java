package com.flashcardapp.ui;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.OutputStream;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.flashcardapp.R;
import com.flashcardapp.models.Flashcard;
import com.flashcardapp.models.FlashcardBatch;
import com.flashcardapp.models.SheetList;
import com.flashcardapp.models.StartingInfo;
import com.flashcardapp.network.ApiClient;
import com.flashcardapp.network.QuizApiService;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardActivity extends AppCompatActivity {

    private static final String TAG = "FlashcardActivity";
    private static final String PREFS_NAME = "FlashcardPreferences";
    private static final String KEY_CARD_NUMBER = "current_card_number";
    private static final String KEY_SHEET_NAME = "selected_sheet_name";
    private static final String KEY_TTS_SPEED = "tts_speech_rate";
    private static final String KEY_TTS_AUTO_PLAY = "tts_auto_play";
    private static final String KEY_TTS_LANGUAGE = "tts_language";
    private static final int PREFETCH_COUNT = 10; // Number of cards to prefetch
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    private static final int REQUEST_RECORD_AUDIO = 200;
    private static final int REQUEST_WRITE_STORAGE = 201;
    private static final int MAX_RECORDING_DURATION = 60000; // 60 seconds

    // Views
    private TextView cardProgressText;
    private CardView flashcardContainer;
    private android.widget.ScrollView scrollView;
    private TextView frontSideText;
    private TextView backSideText;
    private TextView frontPronunciationText;
    private TextView backPronunciationText;
    private TextView frontSideExtraText;
    private TextView backSideExtraText;
    private android.widget.EditText frontSideEditText;
    private android.widget.EditText backSideEditText;
    private MaterialButton previousButton;
    private MaterialButton nextButton;
    private MaterialButton editButton;
    private MaterialButton settingsButton;
    private MaterialButton ttsButton;
    private MaterialButton importance0Button;
    private MaterialButton importance1Button;
    private MaterialButton importance2Button;
    private MaterialButton importance3Button;
    private MaterialButton recordButton;
    private MaterialButton playButton;
    private MaterialButton deleteAudioButton;
    private MaterialButton saveEditButton;
    private MaterialButton cancelEditButton;
    private MaterialButton colorRedButton;
    private MaterialButton colorBlueButton;
    private MaterialButton colorYellowButton;
    private MaterialButton colorBlackButton;
    private MaterialButton boldButton;
    private MaterialButton underlineButton;
    private android.widget.HorizontalScrollView colorPalette;
    private android.widget.LinearLayout editControls;
    private ProgressBar progressBar;

    // API
    private QuizApiService apiService;

    // Audio
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private boolean isRecording = false;
    private boolean isPlaying = false;

    // TTS
    private TextToSpeech textToSpeech;
    private boolean isSpeaking = false;
    private String cachedFrontText;
    private String cachedBackText;

    // Data
    private Flashcard currentFlashcard;
    private int currentCardNumber = 1;
    private boolean isShowingFront = true;
    private boolean isEditMode = false;

    // Cache for prefetched cards
    private Map<Integer, Flashcard> cardCache = new HashMap<>();
    private boolean isPrefetching = false;

    // Gesture detector for swipe
    private GestureDetector gestureDetector;
    private boolean isFlipping = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        initViews();
        setupClickListeners();
        initializeTTS();

        // Check if API URL is configured
        if (!ApiClient.isConfigured(this)) {
            showError("Please configure API URL in Settings");
            Toast.makeText(this, "Please set API URL in Settings", Toast.LENGTH_LONG).show();
            // Open settings automatically
            openSettings();
            return;
        }

        apiService = ApiClient.getApiService(this);

        if (apiService == null) {
            showError("API service not available. Please check Settings.");
            return;
        }

        // Check if sheet name is set, if not, fetch available sheets
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.contains(KEY_SHEET_NAME)) {
            Log.d(TAG, "Sheet name not set, fetching available sheets...");
            fetchAndSetDefaultSheet();
        } else {
            // Load starting position
            loadStartingInfo();
        }
    }

    private void initViews() {
        cardProgressText = findViewById(R.id.cardProgressText);
        flashcardContainer = findViewById(R.id.flashcardContainer);
        scrollView = findViewById(R.id.scrollView);
        frontSideText = findViewById(R.id.frontSideText);
        backSideText = findViewById(R.id.backSideText);
        frontPronunciationText = findViewById(R.id.frontPronunciationText);
        backPronunciationText = findViewById(R.id.backPronunciationText);
        frontSideExtraText = findViewById(R.id.frontSideExtraText);
        backSideExtraText = findViewById(R.id.backSideExtraText);
        frontSideEditText = findViewById(R.id.frontSideEditText);
        backSideEditText = findViewById(R.id.backSideEditText);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        editButton = findViewById(R.id.editButton);
        settingsButton = findViewById(R.id.settingsButton);
        ttsButton = findViewById(R.id.ttsButton);
        importance0Button = findViewById(R.id.importance0Button);
        importance1Button = findViewById(R.id.importance1Button);
        importance2Button = findViewById(R.id.importance2Button);
        importance3Button = findViewById(R.id.importance3Button);
        recordButton = findViewById(R.id.recordButton);
        playButton = findViewById(R.id.playButton);
        deleteAudioButton = findViewById(R.id.deleteAudioButton);
        saveEditButton = findViewById(R.id.saveEditButton);
        cancelEditButton = findViewById(R.id.cancelEditButton);
        colorRedButton = findViewById(R.id.colorRedButton);
        colorBlueButton = findViewById(R.id.colorBlueButton);
        colorYellowButton = findViewById(R.id.colorYellowButton);
        colorBlackButton = findViewById(R.id.colorBlackButton);
        boldButton = findViewById(R.id.boldButton);
        underlineButton = findViewById(R.id.underlineButton);
        colorPalette = findViewById(R.id.colorPalette);
        editControls = findViewById(R.id.editControls);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        // Setup swipe gesture detector
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true; // Must return true to capture gesture
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();

                    Log.d(TAG, "Fling detected - diffX: " + diffX + ", diffY: " + diffY + ", velocityX: " + velocityX);

                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        // Horizontal swipe - flip card
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            Log.d(TAG, "Horizontal swipe detected - flipping card");
                            flipCard();
                            return true;
                        }
                    }
                    // Vertical swipes are ignored
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return false;
            }
        });

        // Apply gesture detector to both card container and scroll view
        View.OnTouchListener swipeListener = (v, event) -> {
            Log.d(TAG, "Touch event: " + event.getAction());
            gestureDetector.onTouchEvent(event);
            return false; // Let ScrollView handle scrolling
        };

        flashcardContainer.setOnTouchListener(swipeListener);
        scrollView.setOnTouchListener(swipeListener);

        previousButton.setOnClickListener(v -> loadPreviousCard());
        nextButton.setOnClickListener(v -> loadNextCard());
        editButton.setOnClickListener(v -> toggleEditMode());
        settingsButton.setOnClickListener(v -> openSettings());
        ttsButton.setOnClickListener(v -> toggleTTS());

        // Importance level buttons
        importance0Button.setOnClickListener(v -> setImportance(0));
        importance1Button.setOnClickListener(v -> setImportance(1));
        importance2Button.setOnClickListener(v -> setImportance(2));
        importance3Button.setOnClickListener(v -> setImportance(3));

        // Audio control buttons
        recordButton.setOnClickListener(v -> toggleRecording());
        playButton.setOnClickListener(v -> togglePlayback());
        deleteAudioButton.setOnClickListener(v -> deleteAudio());

        // Edit mode controls
        saveEditButton.setOnClickListener(v -> showSaveConfirmation());
        cancelEditButton.setOnClickListener(v -> showCancelConfirmation());

        // Color palette buttons
        colorRedButton.setOnClickListener(v -> applyColor("#9C1E3B"));  // Red Berry
        colorBlueButton.setOnClickListener(v -> applyColor("#0000FF"));
        colorYellowButton.setOnClickListener(v -> applyColor("#7F00FF"));  // Purple
        colorBlackButton.setOnClickListener(v -> applyColor("#000000"));

        // Style buttons
        boldButton.setOnClickListener(v -> applyBold());
        underlineButton.setOnClickListener(v -> applyUnderline());
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload API service in case URL was changed in Settings
        apiService = ApiClient.getApiService(this);

        if (apiService == null) {
            // API URL not configured, don't try to load cards
            return;
        }

        // Clear cache and reload if sheet was changed in settings
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        // Reload current card number from SharedPreferences (may have been updated in Settings)
        int savedCardNumber = preferences.getInt(KEY_CARD_NUMBER, 1);
        if (currentCardNumber != savedCardNumber) {
            Log.d(TAG, "Card number changed in Settings: " + currentCardNumber + " -> " + savedCardNumber);
            currentCardNumber = savedCardNumber;
        }

        // Load TTS settings
        loadTtsSettings();

        // Only clear cache if we have cards cached (avoid clearing on first load)
        if (!cardCache.isEmpty()) {
            cardCache.clear();
            Log.d(TAG, "Cache cleared due to potential sheet change");
        }

        loadCurrentCard();
    }

    private void fetchAndSetDefaultSheet() {
        showLoading(true);

        apiService.getAvailableSheets("getAvailableSheets").enqueue(new Callback<SheetList>() {
            @Override
            public void onResponse(Call<SheetList> call, Response<SheetList> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> sheets = response.body().getSheets();
                    if (sheets != null && !sheets.isEmpty()) {
                        // Use first sheet as default (skip Settings sheet if present)
                        String defaultSheet = sheets.get(0);
                        if (defaultSheet.equals("Settings") && sheets.size() > 1) {
                            defaultSheet = sheets.get(1);
                        }

                        // Save default sheet name
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                        prefs.edit().putString(KEY_SHEET_NAME, defaultSheet).apply();

                        Log.d(TAG, "Set default sheet to: " + defaultSheet);
                        Toast.makeText(FlashcardActivity.this, "Using sheet: " + defaultSheet, Toast.LENGTH_SHORT).show();

                        // Now load starting info
                        loadStartingInfo();
                    } else {
                        showLoading(false);
                        showError("No sheets found in spreadsheet");
                    }
                } else {
                    showLoading(false);
                    showError("Failed to fetch sheets. Please check Settings.");
                }
            }

            @Override
            public void onFailure(Call<SheetList> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Error fetching sheets", t);
                showError("Network error. Please check Settings.");
            }
        });
    }

    private void loadStartingInfo() {
        showLoading(true);

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        apiService.getFlashcardStartingInfo("getFlashcardStartingInfo", sheetName).enqueue(new Callback<StartingInfo>() {
            @Override
            public void onResponse(Call<StartingInfo> call, Response<StartingInfo> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    StartingInfo info = response.body();
                    currentCardNumber = info.getQuestionNumber();
                    Log.d(TAG, "Starting from Google Sheets for " + sheetName + ": Card=" + currentCardNumber);
                    loadCurrentCard();
                } else {
                    // Fall back to local SharedPreferences if API fails
                    Log.w(TAG, "Failed to load from API, using local cache");
                    loadFromLocalCache();
                }
            }

            @Override
            public void onFailure(Call<StartingInfo> call, Throwable t) {
                showLoading(false);
                Log.e(TAG, "Network error loading starting info, using local cache", t);
                loadFromLocalCache();
            }
        });
    }

    private void loadFromLocalCache() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedCard = preferences.getInt(KEY_CARD_NUMBER, 1);
        currentCardNumber = savedCard;
        Log.d(TAG, "Using local cache: Card=" + currentCardNumber);
        loadCurrentCard();
    }

    private void loadCurrentCard() {
        // Check if card is in cache
        if (cardCache.containsKey(currentCardNumber)) {
            Log.d(TAG, "Loading card " + currentCardNumber + " from cache");
            currentFlashcard = cardCache.get(currentCardNumber);
            displayFlashcard(currentFlashcard);
            // Don't save progress here - only save when user navigates

            // Prefetch more cards if needed
            prefetchCards();
            return;
        }

        // Load from network if not in cache
        showLoading(true);
        resetCardState();

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        apiService.getFlashcard("getFlashcard", currentCardNumber, sheetName)
                .enqueue(new Callback<Flashcard>() {
                    @Override
                    public void onResponse(Call<Flashcard> call, Response<Flashcard> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            currentFlashcard = response.body();
                            cardCache.put(currentCardNumber, currentFlashcard);
                            displayFlashcard(currentFlashcard);
                            // Don't save progress here - only save when user navigates

                            // Prefetch more cards after first load
                            prefetchCards();
                        } else {
                            showError("Failed to load flashcard");
                        }
                    }

                    @Override
                    public void onFailure(Call<Flashcard> call, Throwable t) {
                        showLoading(false);
                        showError("Network error: " + t.getMessage());
                        Log.e(TAG, "Error loading flashcard", t);
                    }
                });
    }

    private void prefetchCards() {
        if (isPrefetching || currentFlashcard == null) {
            return;
        }

        // Calculate which cards to prefetch
        int totalCards = currentFlashcard.getTotalCards();
        int startCard = Math.max(1, currentCardNumber - 2); // Prefetch 2 cards behind
        int endCard = Math.min(totalCards, currentCardNumber + PREFETCH_COUNT);

        // Check if we need to prefetch
        boolean needsPrefetch = false;
        for (int i = startCard; i <= endCard; i++) {
            if (!cardCache.containsKey(i)) {
                needsPrefetch = true;
                break;
            }
        }

        if (!needsPrefetch) {
            return;
        }

        isPrefetching = true;
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        Log.d(TAG, "Prefetching cards " + startCard + " to " + endCard);

        apiService.getFlashcardBatch("getFlashcardBatch", startCard, endCard - startCard + 1, sheetName)
                .enqueue(new Callback<FlashcardBatch>() {
                    @Override
                    public void onResponse(Call<FlashcardBatch> call, Response<FlashcardBatch> response) {
                        isPrefetching = false;

                        if (response.isSuccessful() && response.body() != null) {
                            List<Flashcard> cards = response.body().getCards();
                            if (cards != null) {
                                for (Flashcard card : cards) {
                                    cardCache.put(card.getCurrentNum(), card);
                                }
                                Log.d(TAG, "Prefetched " + cards.size() + " cards. Cache size: " + cardCache.size());
                            }
                        } else {
                            Log.w(TAG, "Failed to prefetch cards");
                        }
                    }

                    @Override
                    public void onFailure(Call<FlashcardBatch> call, Throwable t) {
                        isPrefetching = false;
                        Log.e(TAG, "Error prefetching cards", t);
                    }
                });
    }

    private void loadNextCard() {
        if (currentFlashcard != null && currentCardNumber >= currentFlashcard.getTotalCards()) {
            Toast.makeText(this, "No next card - you've reached the end", Toast.LENGTH_SHORT).show();
            return;
        }
        currentCardNumber++;
        loadCurrentCard();
        saveCurrentProgress(); // Save only when user navigates
    }

    private void loadPreviousCard() {
        if (currentCardNumber <= 1) {
            Toast.makeText(this, "No previous card", Toast.LENGTH_SHORT).show();
            return;
        }
        currentCardNumber--;
        loadCurrentCard();
        saveCurrentProgress(); // Save only when user navigates
    }

    private void displayFlashcard(Flashcard flashcard) {
        cardProgressText.setText("Card " + flashcard.getCurrentNum() + " of " + flashcard.getTotalCards());

        // Parse HTML formatting for rich text (bold, colors, etc.)
        frontSideText.setText(parseHtml(flashcard.getFrontSide()));
        backSideText.setText(parseHtml(flashcard.getBackSide()));

        // Set pronunciation text
        String frontPronunciation = flashcard.getFrontPronunciation();
        if (frontPronunciation != null && !frontPronunciation.isEmpty()) {
            frontPronunciationText.setText(frontPronunciation);
        } else {
            frontPronunciationText.setText("");
        }

        String backPronunciation = flashcard.getBackPronunciation();
        if (backPronunciation != null && !backPronunciation.isEmpty()) {
            backPronunciationText.setText(backPronunciation);
        } else {
            backPronunciationText.setText("");
        }

        // Set extra text
        String frontSideExtra = flashcard.getFrontSideExtra();
        if (frontSideExtra != null && !frontSideExtra.isEmpty()) {
            frontSideExtraText.setText(frontSideExtra);
        } else {
            frontSideExtraText.setText("");
        }

        String backSideExtra = flashcard.getBackSideExtra();
        if (backSideExtra != null && !backSideExtra.isEmpty()) {
            backSideExtraText.setText(backSideExtra);
        } else {
            backSideExtraText.setText("");
        }

        // Pre-parse and cache text for TTS (improves TTS response time)
        cachedFrontText = stripHtmlTags(flashcard.getFrontSide());
        cachedBackText = stripHtmlTags(flashcard.getBackSide());

        // Reset card state
        isShowingFront = true;
        isFlipping = false;
        frontSideText.setVisibility(View.VISIBLE);
        backSideText.setVisibility(View.GONE);
        backSideExtraText.setVisibility(View.GONE);

        // Show front pronunciation only (C column)
        if (frontPronunciation != null && !frontPronunciation.isEmpty()) {
            frontPronunciationText.setVisibility(View.VISIBLE);
        } else {
            frontPronunciationText.setVisibility(View.GONE);
        }
        backPronunciationText.setVisibility(View.GONE);

        // Show front extra text only (E column)
        if (frontSideExtra != null && !frontSideExtra.isEmpty()) {
            frontSideExtraText.setVisibility(View.VISIBLE);
        } else {
            frontSideExtraText.setVisibility(View.GONE);
        }

        // Reset any animations for all text elements
        frontSideText.setAlpha(1.0f);
        backSideText.setAlpha(1.0f);
        frontSideText.setRotationY(0);
        backSideText.setRotationY(0);
        frontPronunciationText.setAlpha(1.0f);
        backPronunciationText.setAlpha(1.0f);
        frontPronunciationText.setRotationY(0);
        backPronunciationText.setRotationY(0);
        frontSideExtraText.setAlpha(1.0f);
        backSideExtraText.setAlpha(1.0f);
        frontSideExtraText.setRotationY(0);
        backSideExtraText.setRotationY(0);

        // Update importance level UI
        updateImportanceButtons(flashcard.getImportance());

        // Update audio buttons UI
        updateAudioButtons();

        // Stop TTS when switching cards
        if (isSpeaking) {
            stopTTS();
        }

        // Auto-play if enabled
        if (shouldAutoPlay()) {
            speakCurrentSide();
        }
    }

    private boolean shouldAutoPlay() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return preferences.getBoolean(KEY_TTS_AUTO_PLAY, false);
    }

    private void updateImportanceButtons(int importance) {
        // Reset all buttons to outlined style
        importance0Button.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        importance1Button.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        importance2Button.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        importance3Button.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        // Highlight selected button
        MaterialButton selectedButton = null;
        switch (importance) {
            case 0:
                selectedButton = importance0Button;
                break;
            case 1:
                selectedButton = importance1Button;
                break;
            case 2:
                selectedButton = importance2Button;
                break;
            case 3:
                selectedButton = importance3Button;
                break;
        }

        if (selectedButton != null) {
            selectedButton.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
    }

    private void setImportance(int level) {
        if (currentFlashcard == null) {
            return;
        }

        Log.d(TAG, "Setting importance to " + level + " for card " + currentCardNumber);
        currentFlashcard.setImportance(level);
        updateImportanceButtons(level);

        // Update in cache
        cardCache.put(currentCardNumber, currentFlashcard);

        // Save to Google Sheets via API
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        apiService.saveImportance("saveImportance", currentCardNumber, level, sheetName)
                .enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Importance saved successfully");
                            Toast.makeText(FlashcardActivity.this, "Importance set to " + level, Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "Failed to save importance");
                            Toast.makeText(FlashcardActivity.this, "Failed to save importance", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        Log.e(TAG, "Error saving importance", t);
                        Toast.makeText(FlashcardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Spanned parseHtml(String html) {
        if (html == null || html.isEmpty()) {
            return Html.fromHtml("", Html.FROM_HTML_MODE_COMPACT);
        }

        Log.d(TAG, "Parsing HTML: " + html);

        // Convert line breaks to <br> tags
        String formatted = html.replace("\n", "<br>");

        // Convert CSS underline styles to <u> tags for Android's HTML parser
        // Google Sheets may send: <span style="text-decoration: underline;">text</span>
        // Match the entire span element with underline and wrap content in <u> tags
        formatted = formatted.replaceAll(
            "<span style=\"([^\"]*)text-decoration:\\s*underline([^\"]*)\">([^<]*)</span>",
            "<u>$3</u>"
        );
        formatted = formatted.replaceAll(
            "<span style=\"([^\"]*)text-decoration-line:\\s*underline([^\"]*)\">([^<]*)</span>",
            "<u>$3</u>"
        );

        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(formatted, Html.FROM_HTML_MODE_COMPACT);
        } else {
            result = Html.fromHtml(formatted);
        }

        Log.d(TAG, "Parsed result: " + result.toString());
        return result;
    }

    private void flipCard() {
        // Prevent multiple simultaneous flips
        if (isFlipping) {
            Log.d(TAG, "Already flipping, ignoring swipe");
            return;
        }

        isFlipping = true;
        Log.d(TAG, "Flipping card from " + (isShowingFront ? "front" : "back"));

        // Stop current TTS if playing
        if (isSpeaking) {
            stopTTS();
        }

        // Flip with animation
        final float scale = getResources().getDisplayMetrics().density;
        flashcardContainer.setCameraDistance(8000 * scale);

        AnimatorSet frontAnim;
        AnimatorSet backAnim;
        AnimatorSet frontPronunciationAnim;
        AnimatorSet backPronunciationAnim;
        AnimatorSet frontExtraAnim;
        AnimatorSet backExtraAnim;

        if (isShowingFront) {
            // Flip from front to back
            frontAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            backAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);

            frontAnim.setTarget(frontSideText);
            backAnim.setTarget(backSideText);

            frontAnim.start();
            backAnim.start();

            backSideText.setVisibility(View.VISIBLE);

            // Animate pronunciation text
            frontPronunciationAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            frontPronunciationAnim.setTarget(frontPronunciationText);
            frontPronunciationAnim.start();

            if (currentFlashcard != null && currentFlashcard.getBackPronunciation() != null
                    && !currentFlashcard.getBackPronunciation().isEmpty()) {
                backPronunciationText.setVisibility(View.VISIBLE);
                backPronunciationAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);
                backPronunciationAnim.setTarget(backPronunciationText);
                backPronunciationAnim.start();
            }

            // Animate extra text
            frontExtraAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            frontExtraAnim.setTarget(frontSideExtraText);
            frontExtraAnim.start();

            if (currentFlashcard != null && currentFlashcard.getBackSideExtra() != null
                    && !currentFlashcard.getBackSideExtra().isEmpty()) {
                backSideExtraText.setVisibility(View.VISIBLE);
                backExtraAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);
                backExtraAnim.setTarget(backSideExtraText);
                backExtraAnim.start();
            }

            frontSideText.postDelayed(() -> {
                frontSideText.setVisibility(View.GONE);
                frontPronunciationText.setVisibility(View.GONE);
                frontSideExtraText.setVisibility(View.GONE);

                if (currentFlashcard == null || currentFlashcard.getBackPronunciation() == null
                        || currentFlashcard.getBackPronunciation().isEmpty()) {
                    backPronunciationText.setVisibility(View.GONE);
                }
                if (currentFlashcard == null || currentFlashcard.getBackSideExtra() == null
                        || currentFlashcard.getBackSideExtra().isEmpty()) {
                    backSideExtraText.setVisibility(View.GONE);
                }

                isFlipping = false;

                // Auto-play back side after flip animation
                if (shouldAutoPlay()) {
                    speakCurrentSide();
                }
            }, 200);

            isShowingFront = false;
        } else {
            // Flip from back to front
            frontAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);
            backAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);

            frontAnim.setTarget(frontSideText);
            backAnim.setTarget(backSideText);

            backAnim.start();
            frontAnim.start();

            frontSideText.setVisibility(View.VISIBLE);

            // Animate pronunciation text
            backPronunciationAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            backPronunciationAnim.setTarget(backPronunciationText);
            backPronunciationAnim.start();

            if (currentFlashcard != null && currentFlashcard.getFrontPronunciation() != null
                    && !currentFlashcard.getFrontPronunciation().isEmpty()) {
                frontPronunciationText.setVisibility(View.VISIBLE);
                frontPronunciationAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);
                frontPronunciationAnim.setTarget(frontPronunciationText);
                frontPronunciationAnim.start();
            }

            // Animate extra text
            backExtraAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            backExtraAnim.setTarget(backSideExtraText);
            backExtraAnim.start();

            if (currentFlashcard != null && currentFlashcard.getFrontSideExtra() != null
                    && !currentFlashcard.getFrontSideExtra().isEmpty()) {
                frontSideExtraText.setVisibility(View.VISIBLE);
                frontExtraAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);
                frontExtraAnim.setTarget(frontSideExtraText);
                frontExtraAnim.start();
            }

            backSideText.postDelayed(() -> {
                backSideText.setVisibility(View.GONE);
                backPronunciationText.setVisibility(View.GONE);
                backSideExtraText.setVisibility(View.GONE);

                if (currentFlashcard == null || currentFlashcard.getFrontPronunciation() == null
                        || currentFlashcard.getFrontPronunciation().isEmpty()) {
                    frontPronunciationText.setVisibility(View.GONE);
                }
                if (currentFlashcard == null || currentFlashcard.getFrontSideExtra() == null
                        || currentFlashcard.getFrontSideExtra().isEmpty()) {
                    frontSideExtraText.setVisibility(View.GONE);
                }

                isFlipping = false;

                // Auto-play front side after flip animation
                if (shouldAutoPlay()) {
                    speakCurrentSide();
                }
            }, 200);

            isShowingFront = true;
        }
    }

    private void resetCardState() {
        isShowingFront = true;
        isFlipping = false;
        frontSideText.setVisibility(View.VISIBLE);
        backSideText.setVisibility(View.GONE);

        // Reset any animations for all text elements
        frontSideText.setAlpha(1.0f);
        backSideText.setAlpha(1.0f);
        frontSideText.setRotationY(0);
        backSideText.setRotationY(0);
        frontPronunciationText.setAlpha(1.0f);
        backPronunciationText.setAlpha(1.0f);
        frontPronunciationText.setRotationY(0);
        backPronunciationText.setRotationY(0);
        frontSideExtraText.setAlpha(1.0f);
        backSideExtraText.setAlpha(1.0f);
        frontSideExtraText.setRotationY(0);
        backSideExtraText.setRotationY(0);
    }

    private void saveCurrentProgress() {
        // Validate card number before saving
        if (currentCardNumber < 1) {
            Log.e(TAG, "Invalid card number detected: " + currentCardNumber + ", not saving");
            return;
        }

        if (currentFlashcard != null && currentCardNumber > currentFlashcard.getTotalCards()) {
            Log.w(TAG, "Card number " + currentCardNumber + " exceeds total cards " + currentFlashcard.getTotalCards());
        }

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_CARD_NUMBER, currentCardNumber)
                .apply();

        Log.d(TAG, "Saved progress locally: Card=" + currentCardNumber);

        // Also save to Google Sheets with sheet name (only if API service is available)
        if (apiService == null) {
            Log.w(TAG, "API service not available, skipping Google Sheets sync");
            return;
        }

        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");
        final int cardToSave = currentCardNumber; // Capture value to avoid race condition

        Log.d(TAG, "Sending to Google Sheets - Sheet: " + sheetName + ", Card: " + cardToSave);

        apiService.saveProgress("saveProgress", cardToSave, sheetName)
                .enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Progress saved to Google Sheets for " + sheetName + ": Card=" + cardToSave);
                        } else {
                            Log.w(TAG, "Failed to save progress to Google Sheets: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        Log.e(TAG, "Error saving progress to Google Sheets", t);
                    }
                });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        flashcardContainer.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // ==================== Audio Recording Methods ====================

    private File getAudioFile(int cardNumber) {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        // Sanitize sheet name for file system
        String safeSheetName = sanitizeFileName(sheetName);

        File audioDir = new File(getFilesDir(), "audio");
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }

        String fileName = safeSheetName + "_card_" + cardNumber + ".3gp";
        return new File(audioDir, fileName);
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[/\\\\:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .trim();
    }

    private void updateAudioButtons() {
        File audioFile = getAudioFile(currentCardNumber);
        boolean hasAudio = audioFile.exists();

        playButton.setEnabled(hasAudio);
        deleteAudioButton.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
        recordButton.setText(hasAudio ? "🎤 Re-record" : "🎤 Record");

        // Reset states
        if (!hasAudio) {
            isPlaying = false;
            playButton.setText("▶️ Play");
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }

    private void startRecording() {
        // Check permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO);
            return;
        }

        File audioFile = getAudioFile(currentCardNumber);

        // Show confirmation if audio file already exists
        if (audioFile.exists()) {
            new AlertDialog.Builder(this)
                    .setTitle("Re-record Audio")
                    .setMessage("This will replace the existing recording. Continue?")
                    .setPositiveButton("Re-record", (dialog, which) -> {
                        performRecording();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            performRecording();
        }
    }

    private void performRecording() {
        // Stop playback if playing
        if (isPlaying) {
            stopPlayback();
        }

        File audioFile = getAudioFile(currentCardNumber);

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(audioFile.getAbsolutePath());
        mediaRecorder.setMaxDuration(MAX_RECORDING_DURATION);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            recordButton.setText("⏹ Stop");
            recordButton.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
            Log.d(TAG, "Recording started for card " + currentCardNumber);
            Toast.makeText(this, "Recording... (max 60 sec)", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Recording failed", e);
            Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show();
            releaseMediaRecorder();
        }
    }

    private void stopRecording() {
        if (mediaRecorder != null && isRecording) {
            try {
                mediaRecorder.stop();
                Log.d(TAG, "Recording stopped");

                // Save to public Music folder in background
                saveToPublicMusicFolder();

                Toast.makeText(this, "Recording saved", Toast.LENGTH_SHORT).show();
            } catch (RuntimeException e) {
                Log.e(TAG, "Stop recording failed", e);
            } finally {
                releaseMediaRecorder();
                isRecording = false;
                recordButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                updateAudioButtons();
            }
        }
    }

    private void saveToPublicMusicFolder() {
        // Check storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
                return;
            }
        }

        // Run in background thread
        new Thread(() -> {
            try {
                File sourceFile = getAudioFile(currentCardNumber);
                if (!sourceFile.exists()) {
                    Log.w(TAG, "Source audio file not found: " + sourceFile.getAbsolutePath());
                    return;
                }

                SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");
                String safeSheetName = sanitizeFileName(sheetName);
                String fileName = safeSheetName + "_card_" + currentCardNumber + ".3gp";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ use MediaStore API
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
                    values.put(MediaStore.Audio.Media.RELATIVE_PATH,
                              Environment.DIRECTORY_MUSIC + "/FlashcardApp/" + safeSheetName);
                    values.put(MediaStore.Audio.Media.IS_PENDING, 1);

                    Uri collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                    Uri itemUri = getContentResolver().insert(collection, values);

                    if (itemUri != null) {
                        try (OutputStream out = getContentResolver().openOutputStream(itemUri);
                             FileInputStream in = new FileInputStream(sourceFile)) {
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            out.flush();
                        }

                        values.clear();
                        values.put(MediaStore.Audio.Media.IS_PENDING, 0);
                        getContentResolver().update(itemUri, values, null, null);

                        Log.d(TAG, "Audio saved to public Music folder (MediaStore): " + fileName);
                    }
                } else {
                    // Android 9 and below use File API
                    File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
                    File flashcardDir = new File(musicDir, "FlashcardApp/" + safeSheetName);
                    if (!flashcardDir.exists()) {
                        flashcardDir.mkdirs();
                    }

                    File destFile = new File(flashcardDir, fileName);
                    try (FileInputStream in = new FileInputStream(sourceFile);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }

                    // Notify media scanner
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(destFile));
                    sendBroadcast(mediaScanIntent);

                    Log.d(TAG, "Audio saved to public Music folder: " + destFile.getAbsolutePath());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save audio to public Music folder", e);
                runOnUiThread(() -> Toast.makeText(this,
                    "Failed to save to Music folder", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void togglePlayback() {
        if (isPlaying) {
            stopPlayback();
        } else {
            startPlayback();
        }
    }

    private void startPlayback() {
        File audioFile = getAudioFile(currentCardNumber);

        if (!audioFile.exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Stop recording if recording
        if (isRecording) {
            stopRecording();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
            playButton.setText("⏸ Pause");
            Log.d(TAG, "Playback started for card " + currentCardNumber);

            mediaPlayer.setOnCompletionListener(mp -> {
                isPlaying = false;
                playButton.setText("▶️ Play");
                releaseMediaPlayer();
            });
        } catch (IOException e) {
            Log.e(TAG, "Playback failed", e);
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show();
            releaseMediaPlayer();
        }
    }

    private void stopPlayback() {
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.stop();
            releaseMediaPlayer();
            isPlaying = false;
            playButton.setText("▶️ Play");
            Log.d(TAG, "Playback stopped");
        }
    }

    private void deleteAudio() {
        File audioFile = getAudioFile(currentCardNumber);
        if (!audioFile.exists()) {
            return;
        }

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Recording")
                .setMessage("Are you sure you want to delete this recording?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Stop playback if playing this file
                    if (isPlaying) {
                        stopPlayback();
                    }

                    if (audioFile.delete()) {
                        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
                        updateAudioButtons();
                        Log.d(TAG, "Audio deleted for card " + currentCardNumber);
                    } else {
                        Toast.makeText(this, "Failed to delete recording", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    // ==================== TTS Methods ====================

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsButton.setEnabled(true);
                Log.d(TAG, "TTS initialized successfully");

                // Load and apply settings
                loadTtsSettings();
            } else {
                ttsButton.setEnabled(false);
                Log.e(TAG, "TTS initialization failed");
                Toast.makeText(this, "Text-to-Speech not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTtsSettings() {
        if (textToSpeech == null) return;

        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load speed
        float speed = preferences.getFloat(KEY_TTS_SPEED, 1.0f);
        textToSpeech.setSpeechRate(speed);

        // Load language
        String language = preferences.getString(KEY_TTS_LANGUAGE, "ja");
        Locale locale;
        switch (language) {
            case "ja":
                locale = Locale.JAPANESE;
                break;
            case "en":
                locale = Locale.ENGLISH;
                break;
            case "zh":
                locale = Locale.CHINESE;
                break;
            default:
                locale = Locale.JAPANESE;
                break;
        }

        int result = textToSpeech.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported: " + language + ", falling back to English");
            textToSpeech.setLanguage(Locale.ENGLISH);
        }

        Log.d(TAG, "TTS settings loaded - Speed: " + speed + ", Language: " + language);
    }

    private void toggleTTS() {
        if (isSpeaking) {
            stopTTS();
        } else {
            speakCurrentSide();
        }
    }

    private void speakCurrentSide() {
        if (textToSpeech == null || currentFlashcard == null) return;

        // Use pre-cached text (avoids HTML parsing delay)
        String textToSpeak = isShowingFront ? cachedFrontText : cachedBackText;

        // Safety check for null or empty cache
        if (textToSpeak == null || textToSpeak.isEmpty()) {
            Toast.makeText(this, "No text to speak", Toast.LENGTH_SHORT).show();
            return;
        }

        isSpeaking = true;
        ttsButton.setText("⏸");

        textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance");
        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS started");
            }

            @Override
            public void onDone(String utteranceId) {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    ttsButton.setText("🔊");
                    Log.d(TAG, "TTS completed");
                });
            }

            @Override
            public void onError(String utteranceId) {
                runOnUiThread(() -> {
                    isSpeaking = false;
                    ttsButton.setText("🔊");
                    Log.e(TAG, "TTS error");
                });
            }
        });
    }

    private void stopTTS() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            isSpeaking = false;
            ttsButton.setText("🔊");
            Log.d(TAG, "TTS stopped");
        }
    }

    private String stripHtmlTags(String html) {
        if (html == null || html.isEmpty()) return "";
        return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim();
    }

    // ==================== Edit Mode Methods ====================

    private void toggleEditMode() {
        if (isEditMode) {
            showCancelConfirmation();
        } else {
            enterEditMode();
        }
    }

    private void enterEditMode() {
        if (currentFlashcard == null) {
            Toast.makeText(this, "No card loaded", Toast.LENGTH_SHORT).show();
            return;
        }

        isEditMode = true;
        editButton.setText("✖");

        Log.d(TAG, "Entering edit mode");
        Log.d(TAG, "editControls: " + editControls);
        Log.d(TAG, "colorPalette: " + colorPalette);

        // Disable swipe gestures in edit mode
        flashcardContainer.setOnTouchListener(null);
        scrollView.setOnTouchListener(null);

        // Show edit controls and color palette
        editControls.setVisibility(View.VISIBLE);
        colorPalette.setVisibility(View.VISIBLE);

        Log.d(TAG, "Edit controls visibility: " + editControls.getVisibility());
        Log.d(TAG, "Color palette visibility: " + colorPalette.getVisibility());

        // Hide navigation and other controls
        previousButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        ttsButton.setVisibility(View.GONE);
        importance0Button.setVisibility(View.GONE);
        importance1Button.setVisibility(View.GONE);
        importance2Button.setVisibility(View.GONE);
        importance3Button.setVisibility(View.GONE);
        recordButton.setVisibility(View.GONE);
        playButton.setVisibility(View.GONE);
        deleteAudioButton.setVisibility(View.GONE);

        // Hide navigation bar and importance section completely
        findViewById(R.id.navigationButtons).setVisibility(View.GONE);
        findViewById(R.id.audioControls).setVisibility(View.GONE);

        // Hide importance container (portrait) or label (landscape)
        android.view.View importanceContainer = findViewById(R.id.importanceContainer);
        if (importanceContainer != null) {
            importanceContainer.setVisibility(View.GONE);
        }
        android.view.View importanceLabel = findViewById(R.id.importanceLabel);
        if (importanceLabel != null) {
            importanceLabel.setVisibility(View.GONE);
        }

        // Adjust flashcard container to be above editControls
        android.widget.RelativeLayout.LayoutParams params =
            (android.widget.RelativeLayout.LayoutParams) flashcardContainer.getLayoutParams();
        params.addRule(android.widget.RelativeLayout.ABOVE, R.id.editControls);
        flashcardContainer.setLayoutParams(params);

        // Switch to EditText for the current side
        if (isShowingFront) {
            frontSideText.setVisibility(View.GONE);
            frontSideEditText.setVisibility(View.VISIBLE);
            frontSideEditText.setText(frontSideText.getText());
            frontSideEditText.requestFocus();
            // Disable text selection menu
            disableTextSelectionMenu(frontSideEditText);
        } else {
            backSideText.setVisibility(View.GONE);
            backSideEditText.setVisibility(View.VISIBLE);
            backSideEditText.setText(backSideText.getText());
            backSideEditText.requestFocus();
            // Disable text selection menu
            disableTextSelectionMenu(backSideEditText);
        }

        Toast.makeText(this, "Edit mode: Select text and choose a color", Toast.LENGTH_LONG).show();
    }

    private void disableTextSelectionMenu(android.widget.EditText editText) {
        // Keep the ActionMode but remove only the translate menu item
        editText.setCustomSelectionActionModeCallback(new android.view.ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // Return true to allow the action mode with default items
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                // Remove only the translate menu item
                for (int i = menu.size() - 1; i >= 0; i--) {
                    android.view.MenuItem item = menu.getItem(i);
                    CharSequence title = item.getTitle();
                    if (title != null && (title.toString().toLowerCase().contains("translate")
                        || title.toString().toLowerCase().contains("翻訳"))) {
                        menu.removeItem(item.getItemId());
                    }
                }
                return true;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
            }
        });

        // For Android 6.0+ (API 23+), also customize the insertion handle menu
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            editText.setCustomInsertionActionModeCallback(new android.view.ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
                    menu.clear();
                    return true;
                }

                @Override
                public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(android.view.ActionMode mode) {
                }
            });
        }
    }

    private void showSaveConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Save Changes")
                .setMessage("Save edited contents to Google Sheets?")
                .setPositiveButton("Save", (dialog, which) -> {
                    saveEdit();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCancelConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Discard Changes")
                .setMessage("Edited contents will be discarded. Continue?")
                .setPositiveButton("Discard", (dialog, which) -> {
                    cancelEdit();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void cancelEdit() {
        isEditMode = false;
        editButton.setText("✏️");

        // Hide keyboard
        hideKeyboard();

        // Clear focus and selection from EditText
        if (isShowingFront) {
            frontSideEditText.clearFocus();
            frontSideEditText.setSelection(0);
        } else {
            backSideEditText.clearFocus();
            backSideEditText.setSelection(0);
        }

        // Re-enable swipe gestures
        View.OnTouchListener swipeListener = (v, event) -> {
            gestureDetector.onTouchEvent(event);
            return false;
        };
        flashcardContainer.setOnTouchListener(swipeListener);
        scrollView.setOnTouchListener(swipeListener);

        // Hide edit controls and color palette
        editControls.setVisibility(View.GONE);
        colorPalette.setVisibility(View.GONE);

        // Show navigation and other controls
        previousButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
        ttsButton.setVisibility(View.VISIBLE);
        importance0Button.setVisibility(View.VISIBLE);
        importance1Button.setVisibility(View.VISIBLE);
        importance2Button.setVisibility(View.VISIBLE);
        importance3Button.setVisibility(View.VISIBLE);
        recordButton.setVisibility(View.VISIBLE);
        playButton.setVisibility(View.VISIBLE);
        if (getAudioFile(currentCardNumber).exists()) {
            deleteAudioButton.setVisibility(View.VISIBLE);
        }

        // Show navigation bars
        findViewById(R.id.navigationButtons).setVisibility(View.VISIBLE);
        findViewById(R.id.audioControls).setVisibility(View.VISIBLE);

        // Show importance container (portrait) or label (landscape)
        android.view.View importanceContainer = findViewById(R.id.importanceContainer);
        if (importanceContainer != null) {
            importanceContainer.setVisibility(View.VISIBLE);
        }
        android.view.View importanceLabel = findViewById(R.id.importanceLabel);
        if (importanceLabel != null) {
            importanceLabel.setVisibility(View.VISIBLE);
        }

        // Restore flashcard container to be above audioControls
        android.widget.RelativeLayout.LayoutParams params =
            (android.widget.RelativeLayout.LayoutParams) flashcardContainer.getLayoutParams();
        params.addRule(android.widget.RelativeLayout.ABOVE, R.id.audioControls);
        flashcardContainer.setLayoutParams(params);

        // Switch back to TextView
        if (isShowingFront) {
            frontSideEditText.setVisibility(View.GONE);
            frontSideText.setVisibility(View.VISIBLE);
        } else {
            backSideEditText.setVisibility(View.GONE);
            backSideText.setVisibility(View.VISIBLE);
        }
    }

    private void applyColor(String colorHex) {
        android.widget.EditText editText = isShowingFront ? frontSideEditText : backSideEditText;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start >= end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.text.SpannableString spannable = new android.text.SpannableString(editText.getText());
        android.text.style.ForegroundColorSpan colorSpan =
            new android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor(colorHex));
        spannable.setSpan(colorSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannable);
        editText.setSelection(end); // Move cursor to end of selection

        Toast.makeText(this, "Color applied", Toast.LENGTH_SHORT).show();
    }

    private void applyBold() {
        android.widget.EditText editText = isShowingFront ? frontSideEditText : backSideEditText;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start >= end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.text.SpannableString spannable = new android.text.SpannableString(editText.getText());
        android.text.style.StyleSpan boldSpan =
            new android.text.style.StyleSpan(android.graphics.Typeface.BOLD);
        spannable.setSpan(boldSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannable);
        editText.setSelection(end); // Move cursor to end of selection

        Toast.makeText(this, "Bold applied", Toast.LENGTH_SHORT).show();
    }

    private void applyUnderline() {
        android.widget.EditText editText = isShowingFront ? frontSideEditText : backSideEditText;

        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();

        if (start >= end) {
            Toast.makeText(this, "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        android.text.SpannableString spannable = new android.text.SpannableString(editText.getText());
        android.text.style.UnderlineSpan underlineSpan = new android.text.style.UnderlineSpan();
        spannable.setSpan(underlineSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        editText.setText(spannable);
        editText.setSelection(end); // Move cursor to end of selection

        Toast.makeText(this, "Underline applied", Toast.LENGTH_SHORT).show();
    }

    private void saveEdit() {
        if (currentFlashcard == null) return;

        // Hide keyboard
        hideKeyboard();

        showLoading(true);

        // Get the edited text
        android.widget.EditText editText = isShowingFront ? frontSideEditText : backSideEditText;
        android.text.Spanned spannedText = (android.text.Spanned) editText.getText();

        // Convert to HTML
        String htmlContent = spannableToHtml(spannedText);
        String side = isShowingFront ? "front" : "back";

        Log.d(TAG, "Saving " + side + " side HTML: " + htmlContent);

        // Save to Google Sheets
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        apiService.saveCardContent("saveCardContent", currentCardNumber, sheetName, side, htmlContent)
                .enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        showLoading(false);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "Card content saved successfully");
                            Toast.makeText(FlashcardActivity.this, "Saved to Google Sheets", Toast.LENGTH_SHORT).show();

                            // Update current flashcard
                            if (isShowingFront) {
                                currentFlashcard.setFrontSide(htmlContent);
                                frontSideText.setText(parseHtml(htmlContent));
                            } else {
                                currentFlashcard.setBackSide(htmlContent);
                                backSideText.setText(parseHtml(htmlContent));
                            }

                            // Update cache
                            cardCache.put(currentCardNumber, currentFlashcard);

                            // Exit edit mode
                            cancelEdit();
                        } else {
                            Log.w(TAG, "Failed to save card content");
                            Toast.makeText(FlashcardActivity.this, "Failed to save", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.google.gson.JsonObject> call, Throwable t) {
                        showLoading(false);
                        Log.e(TAG, "Error saving card content", t);
                        Toast.makeText(FlashcardActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void hideKeyboard() {
        android.view.View view = getCurrentFocus();
        if (view != null) {
            android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private String spannableToHtml(android.text.Spanned spanned) {
        StringBuilder html = new StringBuilder();
        String text = spanned.toString();
        int next;

        for (int i = 0; i < text.length(); i = next) {
            next = spanned.nextSpanTransition(i, text.length(), android.text.style.CharacterStyle.class);

            // Get all spans affecting this range
            android.text.style.CharacterStyle[] spans = spanned.getSpans(i, next, android.text.style.CharacterStyle.class);

            String segment = text.substring(i, next);

            // Apply styles to this segment
            boolean hasBold = false;
            boolean hasItalic = false;
            boolean hasUnderline = false;
            String color = null;

            for (android.text.style.CharacterStyle span : spans) {
                if (span instanceof android.text.style.StyleSpan) {
                    android.text.style.StyleSpan styleSpan = (android.text.style.StyleSpan) span;
                    if (styleSpan.getStyle() == android.graphics.Typeface.BOLD) {
                        hasBold = true;
                    } else if (styleSpan.getStyle() == android.graphics.Typeface.ITALIC) {
                        hasItalic = true;
                    }
                } else if (span instanceof android.text.style.UnderlineSpan) {
                    hasUnderline = true;
                } else if (span instanceof android.text.style.ForegroundColorSpan) {
                    android.text.style.ForegroundColorSpan colorSpan = (android.text.style.ForegroundColorSpan) span;
                    color = String.format("#%06X", (0xFFFFFF & colorSpan.getForegroundColor()));
                }
            }

            // Build HTML for this segment
            if (hasBold) segment = "<b>" + segment + "</b>";
            if (hasItalic) segment = "<i>" + segment + "</i>";
            if (hasUnderline) segment = "<u>" + segment + "</u>";
            if (color != null) segment = "<font color=\"" + color + "\">" + segment + "</font>";

            html.append(segment);
        }

        return html.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                Toast.makeText(this, "Microphone permission required for recording", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, retry saving to public folder
                saveToPublicMusicFolder();
            } else {
                Toast.makeText(this, "Storage permission denied - audio saved to app folder only", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isRecording) {
            stopRecording();
        }
        if (isPlaying) {
            stopPlayback();
        }
        if (isSpeaking) {
            stopTTS();
        }
        // Save progress when leaving the app
        saveCurrentProgress();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaRecorder();
        releaseMediaPlayer();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
