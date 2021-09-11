package com.example.hamzaapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Button collectorBtn = findViewById(R.id.btn_collector);
        Button trainModelBtn = findViewById(R.id.btn_train_model);
        Button classifierBtn = findViewById(R.id.btn_classifier);
        Button historyBtn = findViewById(R.id.btn_history);

        collectorBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, CollectorActivity.class);
            startActivity(intent);
        });

        trainModelBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, TrainActivity.class);
            startActivity(intent);
        });

        classifierBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, ClassifyActivity.class);
            startActivity(intent);
        });

        historyBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}