package com.example.mainactivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HistoryActivity extends AppCompatActivity {

    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_months);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupMonthClickListeners();
    }

    private void setupMonthClickListeners() {
        int[] monthCardIds = {
                R.id.monthJan, R.id.monthFeb, R.id.monthMar,
                R.id.monthApr, R.id.monthMay, R.id.monthJun,
                R.id.monthJul, R.id.monthAug, R.id.monthSep,
                R.id.monthOct, R.id.monthNov, R.id.monthDec
        };

        String[] monthNames = {
                "January", "February", "March",
                "April", "May", "June",
                "July", "August", "September",
                "October", "November", "December"
        };

        for (int i = 0; i < monthCardIds.length; i++) {
            CardView card = findViewById(monthCardIds[i]);
            final int monthIndex = i;
            final String monthName = monthNames[i];

            card.setOnClickListener(v -> {
                Intent intent = new Intent(HistoryActivity.this, HistoryMonthDetailActivity.class);
                intent.putExtra("month_index", monthIndex);
                intent.putExtra("month_name", monthName);
                startActivity(intent);
            });
        }
    }
}

