package com.example.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_splash);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } catch (Exception e) {
                    android.util.Log.e("SplashActivity", "Error starting MainActivity", e);
                    finish();
                }
            }, SPLASH_DELAY);
        } catch (Exception e) {
            android.util.Log.e("SplashActivity", "Error in onCreate", e);
            // If splash fails, go directly to MainActivity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}