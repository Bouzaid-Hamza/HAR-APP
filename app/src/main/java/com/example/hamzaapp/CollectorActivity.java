package com.example.hamzaapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.io.File;

public class CollectorActivity extends AppCompatActivity {

    private TextView xView, yView, zView;
    private Button startBtn;
    private Button deleteBtn;
    private RadioButton[] radioBtns;
    private String classLabel;
    private Intent accServiceIntent;
//    private Intent senderServiceIntent;
    private boolean state = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collector);

        accServiceIntent = new Intent(this, AccService.class);
//        senderServiceIntent = new Intent(this, AccValuesSender.class);
        classLabel = Gl.CLASS_STANDING;

        xView = findViewById(R.id.x_value);
        yView = findViewById(R.id.y_value);
        zView = findViewById(R.id.z_value);

        startBtn = findViewById(R.id.btn_start);
        deleteBtn = findViewById(R.id.btn_delete);
        Button stopBtn = findViewById(R.id.btn_stop_classify);

        radioBtns = new RadioButton[4];
        radioBtns[0] = findViewById(R.id.standing);
        radioBtns[1] = findViewById(R.id.walking);
        radioBtns[2] = findViewById(R.id.running);
        radioBtns[3] = findViewById(R.id.others);

        RadioGroup radioGroup = findViewById(R.id.radio_classes);
        radioGroup.setOnCheckedChangeListener((rg, i) -> {
            RadioButton checkedRadio = findViewById(i);
            classLabel = checkedRadio.getText().toString();
        });

        startBtn.setOnClickListener(view -> {
            startAccService();
            updateUi(true);

//            startSenderService();
//            showValuesInUi(true);
        });

        stopBtn.setOnClickListener(view -> {
            stopAccService();
            updateUi(false);

//            stopSenderService();
//            showValuesInUi(false);
        });

        deleteBtn.setOnClickListener(view -> {
            boolean isDeleted = new File(getExternalFilesDir(null), Gl.FEATURES_FILE_NAME).delete();
            if (isDeleted) Fn.toast(this, Gl.FILE_DELETED_MSG);
        });

        Fetch fetch = new Fetch(new Handler(Looper.getMainLooper()));
        fetch.get((code, data) -> {
            if (data != null && code == 100) {
                String[] xyz = data.getStringArray(Gl.KEY_XYZ_VALUES);

                xView.setTextColor(Color.RED);
                yView.setTextColor(Color.DKGRAY);
                zView.setTextColor(Color.BLUE);

                xView.setText(String.format("X = %s", xyz[0]));
                yView.setText(String.format("Y = %s", xyz[1]));
                zView.setText(String.format("Z = %s", xyz[2]));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        stopAccService();
//        stopSenderService();
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(Gl.KEY_COLLECTOR_STATE, state);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle sis) {
        if (sis.getBoolean(Gl.KEY_COLLECTOR_STATE)) {
            super.onRestoreInstanceState(sis);
            startBtn.setEnabled(false);
            deleteBtn.setEnabled(false);
            for (RadioButton b : radioBtns) b.setEnabled(false);
            xView.setVisibility(View.VISIBLE);
            yView.setVisibility(View.VISIBLE);
            zView.setVisibility(View.VISIBLE);
//            startSenderService();
        }
    }

    private void startAccService() {
        state = true;
        accServiceIntent.putExtra(Gl.KEY_CLASS_LABEL, classLabel);
        ContextCompat.startForegroundService(this, accServiceIntent);
    }

    private void stopAccService() {
        state = false;
        accServiceIntent.removeExtra(Gl.KEY_CLASS_LABEL);
        stopService(accServiceIntent);
    }

//    private void startSenderService() {
//        state = true;
//        senderServiceIntent.putExtra(Gl.KEY_RESULT_RECEIVER, fetch);
//        startService(senderServiceIntent);
//    }

//    private void stopSenderService() {
//        state = false;
//        senderServiceIntent.removeExtra(Gl.KEY_RESULT_RECEIVER);
//        stopService(senderServiceIntent);
//    }

    private void updateUi(boolean bool) {
        startBtn.setEnabled(!bool);
        deleteBtn.setEnabled(!bool);
        for (RadioButton b : radioBtns) b.setEnabled(!bool);
        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }

//    private void showValuesInUi(boolean bool) {
//        xView.setVisibility(bool ? View.VISIBLE : View.INVISIBLE);
//        yView.setVisibility(bool ? View.VISIBLE : View.INVISIBLE);
//        zView.setVisibility(bool ? View.VISIBLE : View.INVISIBLE);
//    }
}
