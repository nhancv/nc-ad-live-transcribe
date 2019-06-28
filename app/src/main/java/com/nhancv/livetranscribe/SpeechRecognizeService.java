package com.nhancv.livetranscribe;


import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;

import java.util.Locale;

public class SpeechRecognizeService extends Service {
    private static final String TAG = SpeechRecognizeService.class.getSimpleName();

    private SpeechRecognize speechRecognize;
    private final SpeechBinder binder = new SpeechBinder();

    public static SpeechRecognizeService from(IBinder binder) {
        return ((SpeechRecognizeService.SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (speechRecognize == null) {
            speechRecognize = new SpeechRecognize(getApplicationContext().getResources().openRawResource(R.raw.credentials));
        }
    }

    @Override
    public void onDestroy() {
        if (speechRecognize != null) {
            speechRecognize.destroy();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public SpeechRecognize getSpeechRecognize() {
        return speechRecognize;
    }

    private class SpeechBinder extends Binder {
        SpeechRecognizeService getService() {
            return SpeechRecognizeService.this;
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
