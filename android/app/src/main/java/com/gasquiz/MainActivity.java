package com.gasquiz;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.gasquiz.ui.QuizActivity;

/**
 * Native version of MainActivity - launches QuizActivity instead of WebView
 *
 * To use: Rename this file to MainActivity.java and backup the old MainActivity.java
 */
public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "========================================");
        Log.i(TAG, "🚀 App Started - Patent Quiz (NATIVE)");
        Log.i(TAG, "========================================");

        // Launch native QuizActivity
        Intent intent = new Intent(this, QuizActivity.class);
        startActivity(intent);

        // Finish MainActivity so back button doesn't return here
        finish();
    }
}
