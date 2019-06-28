package com.nhancv.livetranscribe;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import java.util.Locale;

public class SpeedRecognizeService extends Service {
    private static final String TAG = SpeedRecognizeService.class.getSimpleName();

    private SpeedRecognize speedRecognize;
    private final SpeechBinder binder = new SpeechBinder();

    public static SpeedRecognizeService from(IBinder binder) {
        return ((SpeedRecognizeService.SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (speedRecognize == null) {
            speedRecognize = new SpeedRecognize(getApplicationContext().getResources().openRawResource(R.raw.credentials));
        }
    }

    @Override
    public void onDestroy() {
        if (speedRecognize != null) {
            speedRecognize.destroy();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public SpeedRecognize getSpeedRecognize() {
        return speedRecognize;
    }

    private class SpeechBinder extends Binder {
        SpeedRecognizeService getService() {
            return SpeedRecognizeService.this;
        }

    }

    private String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

}
