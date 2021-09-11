package com.example.hamzaapp;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SerializationHelper;
import weka.core.converters.ConverterUtils;

public class ClassifyActivity extends AppCompatActivity implements SensorEventListener {

    private J48 j48;
    private Instances dataSet;
    private Attribute classLabels;
    private SensorManager sensorManager;
    private TextView viewActivity;
    private ProgressBar progressBar;
    private TextView disStanding;
    private TextView disWalking;
    private TextView disRunning;
    private TextView disOthers;
    private ArrayBlockingQueue<Double> accBuffer = new ArrayBlockingQueue<>(Gl.ACC_BUFFER_CAPACITY);
    private final FFT fft = new FFT(Gl.ACC_BlOCK_CAPACITY);
    private boolean running;
    private final int[] predictCount = {0,0,0,0};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);

        viewActivity = findViewById(R.id.activity);
        progressBar = findViewById(R.id.progress_bar_classify);
        disStanding = findViewById(R.id.dis_standing);
        disWalking = findViewById(R.id.dis_walking);
        disRunning = findViewById(R.id.dis_running);
        disOthers = findViewById(R.id.dis_others);
        Button classifyBtn = findViewById(R.id.btn_classify);
        Button stopClassifyBtn = findViewById(R.id.btn_stop_classify);

        classifyBtn.setEnabled(false);
        stopClassifyBtn.setEnabled(false);
        viewActivity.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            loadTrainSet();
            loadJ48Model();
            classLabels = dataSet.classAttribute();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                viewActivity.setVisibility(View.VISIBLE);
                classifyBtn.setEnabled(true);
                stopClassifyBtn.setEnabled(true);
            });
        }).start();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        HandlerThread mSensorThread = new HandlerThread("sensor_thread");
        mSensorThread.start();
        Handler mHandler = new Handler(mSensorThread.getLooper());

        classifyBtn.setOnClickListener(view -> {
            classifyBtn.setEnabled(false);
            viewActivity.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);

            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, mHandler);
            running = true;

            predictActivity();
        });

        stopClassifyBtn.setOnClickListener(view -> {
            running = false;
            sensorManager.unregisterListener(this);
            viewActivity.setText(R.string.predict_activity);
            classifyBtn.setEnabled(true);

            disStanding.setText(R.string.three_pts);
            disWalking.setText(R.string.three_pts);
            disRunning.setText(R.string.three_pts);
            disOthers.setText(R.string.three_pts);
        });
    }

    @Override
    protected void onDestroy() {
        running = false;
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if(running && se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            fillAccBuffer(se);
        }
    }

    private void loadTrainSet() {
        File featuresFile = new File(getExternalFilesDir(null), Gl.FEATURES_FILE_NAME);
        try {
            ConverterUtils.DataSource source;
            if (featuresFile.exists()) {
                source = new ConverterUtils.DataSource(new FileInputStream(featuresFile));
            } else {
                InputStream inputStream = getAssets().open(Gl.FEATURES_FILE_NAME);
                source = new ConverterUtils.DataSource(inputStream);
            }
            dataSet = source.getDataSet();
            dataSet.setClassIndex(dataSet.numAttributes() - 1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadJ48Model() {
        File modelFile = new File(getExternalFilesDir(null), Gl.MODEL_FILE_NAME);
        if (modelFile.exists()) {
            try {
                j48 = (J48) SerializationHelper.read(modelFile.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                InputStream inputStream = getAssets().open(Gl.MODEL_FILE_NAME);
                j48 = (J48) SerializationHelper.read(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fillAccBuffer(SensorEvent se) {
        double x = se.values[0];
        double y = se.values[1];
        double z = se.values[2];
        double magnitude = Math.sqrt(x*x + y*y + z*z);

        try { accBuffer.add(magnitude); }
        catch (IllegalStateException e) {
            ArrayBlockingQueue<Double> newBuffer = new ArrayBlockingQueue<>(accBuffer.size()*2);
            accBuffer.drainTo(newBuffer);
            accBuffer = newBuffer;
            accBuffer.add(magnitude);
            e.printStackTrace();
        }
    }

    private Instance createInstance() {
        if (accBuffer.size() >= Gl.ACC_BlOCK_CAPACITY) {
            double[] re = new double[Gl.ACC_BlOCK_CAPACITY];
            double[] im = new double[Gl.ACC_BlOCK_CAPACITY];
            double max = .0;

            for (int i = 0; i < Gl.ACC_BlOCK_CAPACITY; i++) {
                try { re[i] = accBuffer.take(); }
                catch (InterruptedException e) { e.printStackTrace(); }
                im[i] = .0;
            }

            for (double v : re) if (max < v) max = v;
            fft.convert(re,im);

            Instance instance = new DenseInstance(dataSet.numAttributes());
            instance.setDataset(dataSet);
            for (int i = 0; i < Gl.ACC_BlOCK_CAPACITY; i++) {
                double module = Math.sqrt(re[i]*re[i] + im[i]*im[i]);
                instance.setValue(i, module);
            }
            instance.setValue(Gl.ACC_BlOCK_CAPACITY, max);
            return instance;
        }
        return null;
    }

    private void classifyInstance(Instance instance) {
        if (instance != null) {
            try {
                int classIndex = (int) j48.classifyInstance(instance);
                String className = classLabels.value(classIndex);
                predictCount[classIndex]++;
                runOnUiThread(() -> viewActivity.setText(className));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void distributionForInstance(Instance instance) {
        try {
            double[] distribution = j48.distributionForInstance(instance);
            String standingPct = Gl.DF2.format(distribution[0]*100) + "%";
            String walkingPct = Gl.DF2.format(distribution[1]*100) + "%";
            String runningPct = Gl.DF2.format(distribution[2]*100) + "%";
            String othersPct = Gl.DF2.format(distribution[3]*100) + "%";
            runOnUiThread(() -> {
                disStanding.setText(standingPct);
                disWalking.setText(walkingPct);
                disRunning.setText(runningPct);
                disOthers.setText(othersPct);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void predictActivity() {
        AtomicReference<Instance> atomRef = new AtomicReference<>(createInstance());
        AtomicInteger currLoop = new AtomicInteger();
        new Thread(() -> {
            long start = SystemClock.currentThreadTimeMillis();
            
            while (running) {
                atomRef.set(createInstance());
                if (atomRef.get() != null) {
                    classifyInstance(atomRef.get());
                    distributionForInstance(atomRef.get());
                    if (currLoop.get() == 0) {
                        currLoop.set(1);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            viewActivity.setVisibility(View.VISIBLE);
                        });
                    }
                }
            }

            long end = SystemClock.currentThreadTimeMillis();
            long time = end - start;
            int sum = Fn.sum(predictCount);
            Long[] history = new Long[predictCount.length];

            loadJsonHistory(history);

            for (int i = 0; i < predictCount.length; i++) {
                history[i] += Math.round(((double) predictCount[i]/sum)*time);
            }

            saveJsonHistory(history);

        }).start();
    }

    private void loadJsonHistory(Long[] history) {
        File file = new File(getExternalFilesDir(null),Gl.HISTORY_FILE_NAME);

        if (file.exists()) {
            try (Scanner scn = new Scanner(new FileInputStream(file))) {
                JSONParser parser = new JSONParser();
                JSONObject jsonHistory = (JSONObject) parser.parse(scn.next());

                if (jsonHistory != null) {
                    history[0] = (Long) jsonHistory.get(Gl.CLASS_STANDING);
                    history[1] = (Long) jsonHistory.get(Gl.CLASS_WALKING);
                    history[2] = (Long) jsonHistory.get(Gl.CLASS_RUNNING);
                    history[3] = (Long) jsonHistory.get(Gl.CLASS_OTHERS);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            Arrays.fill(history, 0L);
        }
    }

    private void saveJsonHistory(Long[] history) {
        File file = new File(getExternalFilesDir(null),Gl.HISTORY_FILE_NAME);

        Map<String, Long> mapHistory = new HashMap<>();
        mapHistory.put(Gl.CLASS_STANDING, history[0]);
        mapHistory.put(Gl.CLASS_WALKING, history[1]);
        mapHistory.put(Gl.CLASS_RUNNING, history[2]);
        mapHistory.put(Gl.CLASS_OTHERS, history[3]);

        String strHistory = JSONObject.toJSONString(mapHistory);

        try (PrintStream ps = new PrintStream(file)) {
            ps.print(strHistory);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}