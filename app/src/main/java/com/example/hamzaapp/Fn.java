package com.example.hamzaapp;

import android.content.Context;
import android.widget.Toast;

import org.json.simple.JSONObject;

public class Fn {
    public static void toast(Context context, CharSequence text) {
        Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    public static String fix(String string) {
        return String.format("%1$-6s", string);
    }

    public static int sum(int[] arr) {
        int sum = 0;
        for(int a : arr) sum += a;
        return sum;
    }

    public static void main(String[] args) {
        JSONObject obj = new JSONObject();
        obj.put(Gl.CLASS_STANDING,5);
        obj.put(Gl.CLASS_WALKING,10.6);
        obj.put(Gl.CLASS_RUNNING,74.65);
        obj.put(Gl.CLASS_OTHERS,10);

        System.out.println(obj);
    }
}
