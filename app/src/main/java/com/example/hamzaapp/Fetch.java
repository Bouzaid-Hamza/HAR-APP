package com.example.hamzaapp;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class Fetch extends ResultReceiver {
    private ReceiveListener listener;

    public Fetch(Handler handler) {
        super(handler);
    }

    public void get(ReceiveListener listener) {
        this.listener = listener;
    }

    public interface ReceiveListener {
        void onResultReceived(int c, Bundle d);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        listener.onResultReceived(resultCode, resultData);
    }
}
