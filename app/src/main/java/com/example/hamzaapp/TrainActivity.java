package com.example.hamzaapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.J48;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils.DataSource;

public class TrainActivity extends AppCompatActivity {

    private final J48 j48 = new J48();
    private final SummaryRVA summaryRVA = new SummaryRVA();
    private RecyclerView summaryRecView;
    private Instances dataset;
    private Instances trainSet;
    private Instances testSet;
    private Evaluation eval;
    private final ArrayList<String> summary = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        summaryRecView = findViewById(R.id.summary_rec_view);
        Button trainBtn = findViewById(R.id.btn_train);
        Button saveBtn = findViewById(R.id.btn_save);
        ProgressBar progressBar = findViewById(R.id.progress_bar_train);

        trainBtn.setOnClickListener(view -> {
            trainBtn.setEnabled(false);
            saveBtn.setEnabled(false);
            summaryRecView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            new Thread(() -> {
                loadDataset();
                splitDataset();
                trainJ48Model();
                saveJ48Model();
                evaluateJ48Model();
                updateSummary();
                saveSummaryText();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    summaryRecView.setVisibility(View.VISIBLE);
                    saveBtn.setEnabled(true);
                    trainBtn.setEnabled(true);
                });
            }).start();
        });

        saveBtn.setOnClickListener(view -> {
            progressBar.setVisibility(View.VISIBLE);
            saveJ48Model();
            progressBar.setVisibility(View.GONE);
        });

        showInitSummary();
    }

    private void loadDataset() {
        try {
            File featuresFile = new File(getExternalFilesDir(null), Gl.FEATURES_FILE_NAME);
            DataSource source;
            if (featuresFile.exists()) {
                source = new DataSource(new FileInputStream(featuresFile));
            } else {
                InputStream inputStream = getAssets().open(Gl.FEATURES_FILE_NAME);
                source = new DataSource(inputStream);
            }
            dataset = source.getDataSet();
            dataset.setClassIndex(dataset.numAttributes() - 1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void splitDataset() {
        Instances copyDataset = new Instances(dataset);
        copyDataset.randomize(new Random(0));

        int trainSize = (int) Math.round(copyDataset.numInstances() * 0.8);
        int testSize = copyDataset.numInstances() - trainSize;

        trainSet = new Instances(copyDataset, 0, trainSize);
        testSet = new Instances(copyDataset, trainSize, testSize);
    }

    private void trainJ48Model() {
        try { j48.buildClassifier(trainSet); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void evaluateJ48Model() {
        try {
            eval = new Evaluation(dataset);
            eval.evaluateModel(j48, testSet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveJ48Model() {
        File file = new File(getExternalFilesDir(null), Gl.MODEL_FILE_NAME);
        String msg = Gl.MODEL_SAVED_MSG;
        try {
            SerializationHelper.write(file.getAbsolutePath(), j48);
        } catch (Exception e) {
            msg = Gl.MODEL_SAVED_ERROR;
            e.printStackTrace();
        }
        String finalMsg = msg;
        runOnUiThread(() -> Fn.toast(this, finalMsg));
    }

    private void showInitSummary() {
        File file = new File(getExternalFilesDir(null),Gl.SUMMARY_FILE_NAME);
        String line;

        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                while ((line = reader.readLine()) != null) {
                    summary.add(line);
                }
                reader.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
        } else {
            summary.add(Gl.PCT_CORR_INSTS + "...");
            summary.add(Gl.PCT_INCORR_INSTS + "...");
            summary.add(Gl.TOTAL_INSTS + "...");
        }

        summaryRVA.setSummaryItems(summary);
        summaryRecView.setAdapter(summaryRVA);
        summaryRecView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void updateSummary() {
        summary.clear();
        String[] rows = {"","","",""};
        double[][] cm = eval.confusionMatrix();

        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < cm.length; j++) {
                rows[j] += Fn.fix((int) cm[j][i] + "");
            }
        }

        summary.add(Gl.PCT_CORR_INSTS + Gl.DF3.format(eval.pctCorrect()) + "%");
        summary.add(Gl.PCT_INCORR_INSTS + Gl.DF3.format(eval.pctIncorrect()) + "%");
        summary.add(Gl.TOTAL_INSTS + (int) eval.numInstances());
        summary.add("=== Confusion Matrix ===");
        summary.add(Fn.fix("a" + "") + Fn.fix("b" + "") + Fn.fix("c" + "") + Fn.fix("d" + "") + "<-- classified as");
        summary.add(rows[0] + "a = standing");
        summary.add(rows[1] + "b = walking");
        summary.add(rows[2] + "c = running");
        summary.add(rows[3] + "d = others");

        runOnUiThread(() -> {
            summaryRVA.setSummaryItems(summary);
            summaryRecView.setAdapter(summaryRVA);
            summaryRecView.setLayoutManager(new LinearLayoutManager(this));
        });
    }

    private void saveSummaryText() {
        try {
            File file = new File(getExternalFilesDir(null),Gl.SUMMARY_FILE_NAME);
            FileWriter writer = new FileWriter(file);

            for (String stat : summary) writer.write(stat + "\n");

            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}