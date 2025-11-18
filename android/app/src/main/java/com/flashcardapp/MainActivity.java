package com.flashcardapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.flashcardapp.ui.FlashcardActivity;

/**
 * MainActivity - launches FlashcardActivity
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "========================================");
        Log.i(TAG, "🚀 App Started - Flashcard App");
        Log.i(TAG, "========================================");

        // Launch native FlashcardActivity
        Intent intent = new Intent(this, FlashcardActivity.class);
        startActivity(intent);

        // Finish MainActivity so back button doesn't return here
        finish();
    }
}
