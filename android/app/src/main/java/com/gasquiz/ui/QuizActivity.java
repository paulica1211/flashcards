package com.gasquiz.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.gasquiz.R;
import com.gasquiz.models.Question;
import com.gasquiz.models.StartingInfo;
import com.gasquiz.network.ApiClient;
import com.gasquiz.network.QuizApiService;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";
    private static final String PREFS_NAME = "QuizPreferences";
    private static final String KEY_SHEET_NAME = "sheetName";
    private static final String KEY_QUESTION_NUMBER = "questionNumber";
    private static final String KEY_QUESTION_NUMBER_PREFIX = "questionNumber_";

    // Question views
    private LinearLayout questionContainer;
    private TextView questionNumberText;
    private TextView questionImportanceText;
    private TextView questionYearText;
    private TextView articleText;
    private TextView questionText;
    private MaterialButton trueButton;
    private MaterialButton falseButton;
    private MaterialButton backButton;
    private MaterialButton nextButton;
    private MaterialButton settingsButton;

    // Result views
    private CardView resultCard;
    private TextView resultText;
    private TextView timeText;
    private TextView explanationText;
    private TextView lawSentenceText;
    private LinearLayout linksContainer;
    private MaterialButton nextQuestionButton;
    private MaterialButton importance3Button;
    private MaterialButton importance2Button;
    private MaterialButton importance1Button;
    private MaterialButton importance0Button;

    // Loading
    private ProgressBar progressBar;

    // API
    private QuizApiService apiService;

    // Data
    private Question currentQuestion;
    private Question prefetchedNextQuestion;
    private Question prefetchedPreviousQuestion;
    private String currentSheetName;
    private int currentQuestionNumber;
    private int importanceLevel = 0;
    private long startTime;

    // Navigation state
    private boolean cameFromResultCard = false;
    private boolean lastAnswerCorrect = false;
    private double lastAnswerTime = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initViews();
        setupClickListeners();

        apiService = ApiClient.getApiService();

        // Load initial question
        loadStartingInfo();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Called when returning from Settings with FLAG_ACTIVITY_CLEAR_TOP
        Log.d(TAG, "onNewIntent called - reloading settings");
        loadStartingInfo();
    }

    @Override
    public void onBackPressed() {
        // If we're showing the answer review screen, go back to question view
        if (resultCard.getVisibility() == View.VISIBLE) {
            showQuestionView();
            return;
        }

        // If we're on the question view
        if (questionContainer.getVisibility() == View.VISIBLE) {
            // If we just came from result card, go back to show it again
            if (cameFromResultCard) {
                Log.d(TAG, "Going back to result card");
                showResult(lastAnswerCorrect, lastAnswerTime);
                cameFromResultCard = false;
                return;
            }

            // Otherwise load previous question
            loadPreviousQuestion();
            return;
        }

        // Default behavior (exit app)
        super.onBackPressed();
    }

    private void initViews() {
        // Question views
        questionContainer = findViewById(R.id.questionContainer);
        questionNumberText = findViewById(R.id.questionNumberText);
        questionImportanceText = findViewById(R.id.questionImportanceText);
        questionYearText = findViewById(R.id.questionYearText);
        articleText = findViewById(R.id.articleText);
        questionText = findViewById(R.id.questionText);
        trueButton = findViewById(R.id.trueButton);
        falseButton = findViewById(R.id.falseButton);
        backButton = findViewById(R.id.backButton);
        nextButton = findViewById(R.id.nextButton);
        settingsButton = findViewById(R.id.settingsButton);

        // Result views
        resultCard = findViewById(R.id.resultCard);
        resultText = findViewById(R.id.resultText);
        timeText = findViewById(R.id.timeText);
        explanationText = findViewById(R.id.explanationText);
        lawSentenceText = findViewById(R.id.lawSentenceText);
        linksContainer = findViewById(R.id.linksContainer);
        nextQuestionButton = findViewById(R.id.nextQuestionButton);
        importance3Button = findViewById(R.id.importance3Button);
        importance2Button = findViewById(R.id.importance2Button);
        importance1Button = findViewById(R.id.importance1Button);
        importance0Button = findViewById(R.id.importance0Button);

        // Loading
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupClickListeners() {
        trueButton.setOnClickListener(v -> selectAnswer("true"));
        falseButton.setOnClickListener(v -> selectAnswer("false"));

        backButton.setOnClickListener(v -> loadPreviousQuestion());
        nextButton.setOnClickListener(v -> loadNextQuestion());

        nextQuestionButton.setOnClickListener(v -> {
            cameFromResultCard = true; // Mark that we're navigating from result card
            showQuestionView();
            loadNextQuestion();
        });

        importance3Button.setOnClickListener(v -> markImportance(3));
        importance2Button.setOnClickListener(v -> markImportance(2));
        importance1Button.setOnClickListener(v -> markImportance(1));
        importance0Button.setOnClickListener(v -> markImportance(0));

        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(QuizActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadStartingInfo() {
        // Check SharedPreferences first for saved settings
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedSheet = preferences.getString(KEY_SHEET_NAME, null);
        int savedQuestion = preferences.getInt(KEY_QUESTION_NUMBER, -1);

        if (savedSheet != null && savedQuestion > 0) {
            // Use saved settings from Settings screen
            currentSheetName = savedSheet;
            // Load the question number specific to this sheet
            currentQuestionNumber = preferences.getInt(KEY_QUESTION_NUMBER_PREFIX + savedSheet, savedQuestion);
            Log.d(TAG, "Using saved settings: Sheet=" + currentSheetName + ", Q=" + currentQuestionNumber);
            loadCurrentQuestion();
            return;
        }

        // Fall back to API if no saved settings
        showLoading(true);

        apiService.getStartingInfo("getStartingInfo").enqueue(new Callback<StartingInfo>() {
            @Override
            public void onResponse(Call<StartingInfo> call, Response<StartingInfo> response) {
                showLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    StartingInfo info = response.body();
                    currentSheetName = info.getSheetName();
                    currentQuestionNumber = info.getQuestionNumber();
                    Log.d(TAG, "Starting: Sheet=" + currentSheetName + ", Q=" + currentQuestionNumber);
                    loadCurrentQuestion();
                } else {
                    showError("Failed to load starting info");
                }
            }

            @Override
            public void onFailure(Call<StartingInfo> call, Throwable t) {
                showLoading(false);
                showError("Network error: " + t.getMessage());
                Log.e(TAG, "Error loading starting info", t);
            }
        });
    }

    private void loadCurrentQuestion() {
        showLoading(true);

        apiService.getQuestion("getQuestion", currentSheetName, currentQuestionNumber)
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(Call<Question> call, Response<Question> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            currentQuestion = response.body();
                            displayQuestion(currentQuestion);
                            startTime = System.currentTimeMillis();
                        } else {
                            showError("Failed to load question");
                        }
                    }

                    @Override
                    public void onFailure(Call<Question> call, Throwable t) {
                        showLoading(false);
                        showError("Network error: " + t.getMessage());
                        Log.e(TAG, "Error loading question", t);
                    }
                });
    }

    private void loadNextQuestion() {
        cameFromResultCard = false; // Clear flag when using next button

        // Check if we have a prefetched next question
        if (prefetchedNextQuestion != null) {
            // Use prefetched question instantly - no loading delay!
            currentQuestion = prefetchedNextQuestion;
            currentQuestionNumber = currentQuestion.getCurrentNum();
            prefetchedNextQuestion = null; // Clear cache
            prefetchedPreviousQuestion = null; // Invalidate previous cache when moving forward
            displayQuestion(currentQuestion);
            startTime = System.currentTimeMillis();
            Log.d(TAG, "Using prefetched question - instant load!");
            return;
        }

        // Fallback to network call if prefetch wasn't ready
        // Simple sequential navigation: just increment question number
        currentQuestionNumber++;
        showLoading(true);
        Log.d(TAG, "Loading next question sequentially: " + currentQuestionNumber);

        apiService.getQuestion("getQuestion", currentSheetName, currentQuestionNumber)
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(Call<Question> call, Response<Question> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            currentQuestion = response.body();
                            displayQuestion(currentQuestion);
                            startTime = System.currentTimeMillis();
                        } else {
                            currentQuestionNumber--; // Revert on failure
                            Toast.makeText(QuizActivity.this, "No more questions", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Question> call, Throwable t) {
                        showLoading(false);
                        currentQuestionNumber--; // Revert on failure
                        showError("Network error: " + t.getMessage());
                        Log.e(TAG, "Error loading next question", t);
                    }
                });
    }

    private void loadPreviousQuestion() {
        cameFromResultCard = false; // Clear flag when using back button

        // Check if we have a prefetched previous question
        if (prefetchedPreviousQuestion != null) {
            // Use prefetched question instantly - no loading delay!
            currentQuestion = prefetchedPreviousQuestion;
            currentQuestionNumber = currentQuestion.getCurrentNum();
            prefetchedPreviousQuestion = null; // Clear cache
            prefetchedNextQuestion = null; // Invalidate next cache when moving backward
            displayQuestion(currentQuestion);
            startTime = System.currentTimeMillis();
            Log.d(TAG, "Using prefetched previous question - instant load!");
            return;
        }

        // Fallback to network call if prefetch wasn't ready
        // Simple sequential navigation: just decrement question number
        if (currentQuestionNumber <= 1) {
            Toast.makeText(this, "No previous question", Toast.LENGTH_SHORT).show();
            return;
        }

        currentQuestionNumber--;
        showLoading(true);
        Log.d(TAG, "Loading previous question sequentially: " + currentQuestionNumber);

        apiService.getQuestion("getQuestion", currentSheetName, currentQuestionNumber)
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(Call<Question> call, Response<Question> response) {
                        showLoading(false);

                        if (response.isSuccessful() && response.body() != null) {
                            currentQuestion = response.body();
                            displayQuestion(currentQuestion);
                            startTime = System.currentTimeMillis();
                        } else {
                            currentQuestionNumber++; // Revert on failure
                            Toast.makeText(QuizActivity.this, "Failed to load previous question", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Question> call, Throwable t) {
                        showLoading(false);
                        currentQuestionNumber++; // Revert on failure
                        showError("Network error: " + t.getMessage());
                        Log.e(TAG, "Error loading previous question", t);
                    }
                });
    }

    private void displayQuestion(Question question) {
        questionNumberText.setText("Question " + question.getCurrentNum() + " of " + question.getAllQuestion());
        questionImportanceText.setText("Importance: " + question.getOfImportance());
        questionYearText.setText(question.getQuestionYear());
        articleText.setText(question.getArticle());
        questionText.setText(Html.fromHtml(question.getQuestion(), Html.FROM_HTML_MODE_LEGACY));

        // Save current progress locally
        saveCurrentProgress();

        // Prefetch next and previous questions in background
        prefetchNextQuestion();
        prefetchPreviousQuestion();

        showQuestionView();
    }

    private void prefetchNextQuestion() {
        // Load next question in background without showing loading indicator
        // Simple sequential: just load currentQuestionNumber + 1
        int nextQuestionNum = currentQuestionNumber + 1;
        apiService.getQuestion("getQuestion", currentSheetName, nextQuestionNum)
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(Call<Question> call, Response<Question> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            prefetchedNextQuestion = response.body();
                            Log.d(TAG, "Prefetched next question: " + prefetchedNextQuestion.getCurrentNum());
                        }
                    }

                    @Override
                    public void onFailure(Call<Question> call, Throwable t) {
                        Log.w(TAG, "Failed to prefetch next question", t);
                        prefetchedNextQuestion = null;
                    }
                });
    }

    private void prefetchPreviousQuestion() {
        // Load previous question in background without showing loading indicator
        // Simple sequential: just load currentQuestionNumber - 1
        if (currentQuestionNumber <= 1) {
            return; // No previous question to prefetch
        }

        int previousQuestionNum = currentQuestionNumber - 1;
        apiService.getQuestion("getQuestion", currentSheetName, previousQuestionNum)
                .enqueue(new Callback<Question>() {
                    @Override
                    public void onResponse(Call<Question> call, Response<Question> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            prefetchedPreviousQuestion = response.body();
                            Log.d(TAG, "Prefetched previous question: " + prefetchedPreviousQuestion.getCurrentNum());
                        }
                    }

                    @Override
                    public void onFailure(Call<Question> call, Throwable t) {
                        Log.w(TAG, "Failed to prefetch previous question", t);
                        prefetchedPreviousQuestion = null;
                    }
                });
    }

    private void saveCurrentProgress() {
        // Save the current question number for this specific sheet
        SharedPreferences preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        preferences.edit()
                .putInt(KEY_QUESTION_NUMBER_PREFIX + currentSheetName, currentQuestionNumber)
                .putInt(KEY_QUESTION_NUMBER, currentQuestionNumber)
                .putString(KEY_SHEET_NAME, currentSheetName)
                .apply();

        Log.d(TAG, "Saved progress: " + currentSheetName + " - Q" + currentQuestionNumber);

        // Optionally sync to Google Sheets (non-blocking)
        syncProgressToServer();
    }

    private void syncProgressToServer() {
        // Save to Google Sheets in the background (don't wait for response)
        apiService.saveSettings("saveSettings", currentSheetName, currentQuestionNumber)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Progress synced to server");
                        } else {
                            Log.w(TAG, "Failed to sync progress to server");
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.w(TAG, "Network error syncing progress", t);
                    }
                });
    }

    private void selectAnswer(String answer) {
        if (currentQuestion == null) return;

        cameFromResultCard = false; // Clear flag when answering a question

        long elapsedTime = System.currentTimeMillis() - startTime;
        double seconds = elapsedTime / 1000.0;

        boolean isCorrect = answer.equals(currentQuestion.getAnswer());

        // Save the answer result for potential back navigation
        lastAnswerCorrect = isCorrect;
        lastAnswerTime = seconds;

        // Record answer
        apiService.recordAnswer("recordAnswer", currentSheetName, currentQuestionNumber, isCorrect, seconds)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Log.d(TAG, "Answer recorded");
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Failed to record answer", t);
                    }
                });

        showResult(isCorrect, seconds);
    }

    private void showResult(boolean isCorrect, double seconds) {
        // Hide question, show result
        questionContainer.setVisibility(View.GONE);
        resultCard.setVisibility(View.VISIBLE);

        // Display result
        resultText.setText(isCorrect ? "Correct!" : "Incorrect...");
        resultText.setTextColor(getColor(isCorrect ? R.color.primary : android.R.color.holo_red_light));
        timeText.setText(String.format("Time: %.2f seconds", seconds));
        explanationText.setText(Html.fromHtml(currentQuestion.getExplanation(), Html.FROM_HTML_MODE_LEGACY));

        // Display links
        displayLinks();
    }

    private void displayLinks() {
        linksContainer.removeAllViews();

        String[] links = {
                currentQuestion.getLinkM(),
                currentQuestion.getLinkN(),
                currentQuestion.getLinkO(),
                currentQuestion.getLinkP()
        };

        for (String link : links) {
            if (link != null && !link.isEmpty() && link.contains("http")) {
                MaterialButton linkButton = new MaterialButton(this);
                linkButton.setText("View Reference");
                linkButton.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    startActivity(intent);
                });
                linksContainer.addView(linkButton);
            }
        }
    }

    private void markImportance(int importance) {
        apiService.markImportant("markImportant", currentSheetName, currentQuestionNumber, importance)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        Toast.makeText(QuizActivity.this, "Importance marked: " + importance, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        Log.e(TAG, "Failed to mark importance", t);
                    }
                });
    }

    private void showQuestionView() {
        questionContainer.setVisibility(View.VISIBLE);
        resultCard.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
