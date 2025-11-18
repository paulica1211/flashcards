package com.flashcardapp.ui;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.flashcardapp.R;
import com.flashcardapp.models.Flashcard;
import com.flashcardapp.models.FlashcardBatch;
import com.flashcardapp.models.StartingInfo;
import com.flashcardapp.network.ApiClient;
import com.flashcardapp.network.QuizApiService;
import com.google.android.material.button.MaterialButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashcardActivity extends AppCompatActivity {

    private static final String TAG = "FlashcardActivity";
    private static final String PREFS_NAME = "FlashcardPreferences";
    private static final String KEY_CARD_NUMBER = "current_card_number";
    private static final String KEY_SHEET_NAME = "selected_sheet_name";
    private static final int PREFETCH_COUNT = 10; // Number of cards to prefetch
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    // Views
    private TextView cardProgressText;
    private CardView flashcardContainer;
    private android.widget.ScrollView scrollView;
    private TextView frontSideText;
    private TextView backSideText;
    private MaterialButton previousButton;
    private MaterialButton nextButton;
    private MaterialButton settingsButton;
    private MaterialButton importance0Button;
    private MaterialButton importance1Button;
    private MaterialButton importance2Button;
    private MaterialButton importance3Button;
    private ProgressBar progressBar;

    // API
    private QuizApiService apiService;

    // Data
    private Flashcard currentFlashcard;
    private int currentCardNumber = 1;
    private boolean isShowingFront = true;

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

        apiService = ApiClient.getApiService();

        // Load starting position
        loadStartingInfo();
    }

    private void initViews() {
        cardProgressText = findViewById(R.id.cardProgressText);
        flashcardContainer = findViewById(R.id.flashcardContainer);
        scrollView = findViewById(R.id.scrollView);
        frontSideText = findViewById(R.id.frontSideText);
        backSideText = findViewById(R.id.backSideText);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        settingsButton = findViewById(R.id.settingsButton);
        importance0Button = findViewById(R.id.importance0Button);
        importance1Button = findViewById(R.id.importance1Button);
        importance2Button = findViewById(R.id.importance2Button);
        importance3Button = findViewById(R.id.importance3Button);
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
        settingsButton.setOnClickListener(v -> openSettings());

        // Importance level buttons
        importance0Button.setOnClickListener(v -> setImportance(0));
        importance1Button.setOnClickListener(v -> setImportance(1));
        importance2Button.setOnClickListener(v -> setImportance(2));
        importance3Button.setOnClickListener(v -> setImportance(3));
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Clear cache and reload if sheet was changed in settings
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");

        // Only clear cache if we have cards cached (avoid clearing on first load)
        if (!cardCache.isEmpty()) {
            cardCache.clear();
            Log.d(TAG, "Cache cleared due to potential sheet change");
        }

        loadCurrentCard();
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
            saveCurrentProgress();

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
                            saveCurrentProgress();

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
    }

    private void loadPreviousCard() {
        if (currentCardNumber <= 1) {
            Toast.makeText(this, "No previous card", Toast.LENGTH_SHORT).show();
            return;
        }
        currentCardNumber--;
        loadCurrentCard();
    }

    private void displayFlashcard(Flashcard flashcard) {
        cardProgressText.setText("Card " + flashcard.getCurrentNum() + " of " + flashcard.getTotalCards());

        // Parse HTML formatting for rich text (bold, colors, etc.)
        frontSideText.setText(parseHtml(flashcard.getFrontSide()));
        backSideText.setText(parseHtml(flashcard.getBackSide()));

        // Reset card state
        isShowingFront = true;
        isFlipping = false;
        frontSideText.setVisibility(View.VISIBLE);
        backSideText.setVisibility(View.GONE);

        // Reset any animations
        frontSideText.setAlpha(1.0f);
        backSideText.setAlpha(1.0f);
        frontSideText.setRotationY(0);
        backSideText.setRotationY(0);

        // Update importance level UI
        updateImportanceButtons(flashcard.getImportance());
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

        // Flip with animation
        final float scale = getResources().getDisplayMetrics().density;
        flashcardContainer.setCameraDistance(8000 * scale);

        AnimatorSet frontAnim;
        AnimatorSet backAnim;

        if (isShowingFront) {
            // Flip from front to back
            frontAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_out);
            backAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.card_flip_in);

            frontAnim.setTarget(frontSideText);
            backAnim.setTarget(backSideText);

            frontAnim.start();
            backAnim.start();

            backSideText.setVisibility(View.VISIBLE);
            frontSideText.postDelayed(() -> {
                frontSideText.setVisibility(View.GONE);
                isFlipping = false;
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
            backSideText.postDelayed(() -> {
                backSideText.setVisibility(View.GONE);
                isFlipping = false;
            }, 200);

            isShowingFront = true;
        }
    }

    private void resetCardState() {
        isShowingFront = true;
        isFlipping = false;
        frontSideText.setVisibility(View.VISIBLE);
        backSideText.setVisibility(View.GONE);

        // Reset any animations
        frontSideText.setAlpha(1.0f);
        backSideText.setAlpha(1.0f);
        frontSideText.setRotationY(0);
        backSideText.setRotationY(0);
    }

    private void saveCurrentProgress() {
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_CARD_NUMBER, currentCardNumber)
                .apply();

        Log.d(TAG, "Saved progress locally: Card=" + currentCardNumber);

        // Also save to Google Sheets with sheet name
        String sheetName = preferences.getString(KEY_SHEET_NAME, "Sheet1");
        apiService.saveProgress("saveProgress", currentCardNumber, sheetName)
                .enqueue(new Callback<com.google.gson.JsonObject>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonObject> call, Response<com.google.gson.JsonObject> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Progress saved to Google Sheets for " + sheetName + ": Card=" + currentCardNumber);
                        } else {
                            Log.w(TAG, "Failed to save progress to Google Sheets");
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
}
