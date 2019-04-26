package com.nhancv.livetranscribe;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        HandlerThread handlerThread = new HandlerThread("Speed Recognize");
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                SpeedRecognize speedRecognize = new SpeedRecognize();
                try {
                    speedRecognize.test(MainActivity.this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
