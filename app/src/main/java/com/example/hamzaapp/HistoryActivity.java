package com.example.hamzaapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class HistoryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Long[] times = new Long[4];

        TextView standingTime = findViewById(R.id.standing_time);
        TextView walkingTime = findViewById(R.id.walking_time);
        TextView runningTime = findViewById(R.id.running_time);
        TextView othersTime = findViewById(R.id.others_time);
        Button resetBtn = findViewById(R.id.btn_reset);

        File file = new File(getExternalFilesDir(null),Gl.HISTORY_FILE_NAME);
        
        if (file.exists()) resetBtn.setEnabled(true);

        new Thread(() -> {
            loadJsonHistory(times);
            runOnUiThread(() -> {
                standingTime.setText(formatTime(times[0]));
                walkingTime.setText(formatTime(times[1]));
                runningTime.setText(formatTime(times[2]));
                othersTime.setText(formatTime(times[3]));
            });
        }).start();

        resetBtn.setOnClickListener(view -> {
            boolean delete = file.delete();
            if (delete) {
                Fn.toast(this,Gl.HISTORY_FILE_DELETED_MSG);

                standingTime.setText(formatTime(0L));
                walkingTime.setText(formatTime(0L));
                runningTime.setText(formatTime(0L));
                othersTime.setText(formatTime(0L));

                resetBtn.setEnabled(false);
            } else {
                Fn.toast(this,Gl.HISTORY_FILE_DELETE_ERROR);
            }
        });
    }

    private void loadJsonHistory(Long[] times) {
        File file = new File(getExternalFilesDir(null),Gl.HISTORY_FILE_NAME);

        if (file.exists()) {
            try (Scanner scn = new Scanner(new FileInputStream(file))) {
                JSONParser parser = new JSONParser();
                JSONObject jsonHistory = (JSONObject) parser.parse(scn.next());

                if (jsonHistory != null) {
                    times[0] = (Long) jsonHistory.get(Gl.CLASS_STANDING);
                    times[1] = (Long) jsonHistory.get(Gl.CLASS_WALKING);
                    times[2] = (Long) jsonHistory.get(Gl.CLASS_RUNNING);
                    times[3] = (Long) jsonHistory.get(Gl.CLASS_OTHERS);
                }
            } catch (FileNotFoundException | ParseException e) {
                e.printStackTrace();
            }
        } else {
            Arrays.fill(times, 0L);
        }
    }

    private String formatTime(Long t) {
        long s = Math.round((double) (t / 1000) % 60);
        long m = Math.round((double) (t / (1000 * 60)) % 60);
        long h = Math.round((double) (t / (1000 * 60 * 60)) % 24);

        return Gl.FFT_DF.format(h) + ":" + Gl.FFT_DF.format(m) + ":" + Gl.FFT_DF.format(s);
    }
}