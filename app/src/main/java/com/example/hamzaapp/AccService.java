package com.example.hamzaapp;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;


public class AccService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private String classLabel;
    private Instances dataSet;
    private ArrayBlockingQueue<Double> accBuffer;
    private final FFT fft = new FFT(Gl.ACC_BlOCK_CAPACITY);
    private File file;
    private boolean running;

    @Override
    public void onCreate() {
        super.onCreate();

        accBuffer = new ArrayBlockingQueue<>(Gl.ACC_BUFFER_CAPACITY);
        file = new File(getExternalFilesDir(null), Gl.FEATURES_FILE_NAME);
        createAttributes();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);

        classLabel = intent.getStringExtra(Gl.KEY_CLASS_LABEL);
        Intent notifIntent = new Intent(this, CollectorActivity.class);

        notifIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
            notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_settings_foreground)
            .setContentTitle(Gl.NOTIFICATION_TITLE)
            .setContentText(Gl.NOTIFICATION_CONTENT)
            .setContentIntent(pendingIntent)
            .build();

        startForeground(1, notification);

        running = true;
        new Thread(() -> {
            while (running) createInstance();
            createFeaturesFile();
        }).start();


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;

        try { Thread.sleep(100); }
        catch (InterruptedException e) { e.printStackTrace(); }

        sensorManager.unregisterListener(this);

        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if(se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            fillAccBuffer(se);
        }
    }

    private void createAttributes() {
        ArrayList<Attribute> attributes = new ArrayList<>(Gl.ACC_BlOCK_CAPACITY + 2);
        for (int i = 0; i < Gl.ACC_BlOCK_CAPACITY; i++) {
            attributes.add(new Attribute(Gl.FFT_COEF_LABEL + Gl.FFT_DF.format(i)));
        }
        attributes.add(new Attribute(Gl.MAX_LABEL));

        ArrayList<String> labels = new ArrayList<>();
        labels.add(Gl.CLASS_STANDING);
        labels.add(Gl.CLASS_WALKING);
        labels.add(Gl.CLASS_RUNNING);
        labels.add(Gl.CLASS_OTHERS);
        attributes.add(new Attribute(Gl.CLASS_HEADER, labels));

        dataSet = new Instances(Gl.DATA_SET_NAME, attributes, Gl.DATASET_CAPACITY);
        dataSet.setClassIndex(dataSet.numAttributes() - 1);
    }

    private void fillAccBuffer(SensorEvent se) {
        double x = se.values[0];
        double y = se.values[1];
        double z = se.values[2];
        double magnitude = Math.sqrt(x*x + y*y + z*z);

        try { accBuffer.add(magnitude); }
        catch (IllegalStateException e) {
            ArrayBlockingQueue<Double> newBuffer = new ArrayBlockingQueue<>(accBuffer.size() * 2);
            accBuffer.drainTo(newBuffer);
            accBuffer = newBuffer;
            accBuffer.add(magnitude);
            e.printStackTrace();
        }
    }

    private void createInstance() {
        if(accBuffer.size() >= Gl.ACC_BlOCK_CAPACITY) {
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

            instance.setClassValue(classLabel.toLowerCase(Locale.ROOT));
            dataSet.add(instance);

            System.out.println(dataSet.size());
        }
    }

    private void createFeaturesFile() {
        String msg = Gl.FILE_CREATED_MSG;
        if (file.exists()) {
            try {
                DataSource source = new DataSource(new FileInputStream(file));
                Instances oldDataset = source.getDataSet();
                oldDataset.setClassIndex(dataSet.numAttributes() - 1);

                if (!oldDataset.equalHeaders(dataSet)) {
                    throw new Exception("The two datasets have different headers:\n");
                }

                oldDataset.addAll(dataSet);
                dataSet = oldDataset;
                boolean isDeleted = file.delete();

                msg = isDeleted ? Gl.FILE_UPDATED_MSG : Gl.OLD_FILE_ERROR_MSG;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArffSaver arffSaver = new ArffSaver();
        arffSaver.setInstances(dataSet);

        try {
            arffSaver.setFile(file);
            arffSaver.writeBatch();
        } catch (IOException e) {
            msg = Gl.FILE_ERROR_MSG;
            e.printStackTrace();
        }

        final String finalMsg = msg;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Fn.toast(this, finalMsg));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
