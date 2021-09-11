package com.example.hamzaapp;

public class IntTracker {
    private int value;
    private OnIntegerChangeListener listener;
    public interface OnIntegerChangeListener {
        void onIntegerChange(int i);
    }

    public void onChange(OnIntegerChangeListener l) {
        this.listener = l;
    }

    public void setInt(int value) {
        if (this.value != value) {
            this.value = value;
            if (listener != null) {
                listener.onIntegerChange(value);
            }
        }
    }
}
