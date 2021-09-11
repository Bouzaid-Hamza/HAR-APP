package com.example.hamzaapp;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;

public class AccValuesSender extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private ResultReceiver fetch;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            fetch = intent.getParcelableExtra(Gl.KEY_RESULT_RECEIVER);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            sendValuesToUi(se);
        }
    }

    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        super.onDestroy();
    }

    private void sendValuesToUi(SensorEvent se) {
        String[] values = new String[3];
        values[0] = Gl.DF2.format(se.values[0]);
        values[1] = Gl.DF2.format(se.values[1]);
        values[2] = Gl.DF2.format(se.values[2]);

        Bundle bundle = new Bundle();
        bundle.putStringArray(Gl.KEY_XYZ_VALUES, values);
        fetch.send(100, bundle);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

}
