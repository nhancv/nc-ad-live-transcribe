package com.nhancv.livetranscribe;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (isServiceReady()) {
                speedRecognizeService.getSpeedRecognize().start(voiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (isServiceReady()) {
                speedRecognizeService.getSpeedRecognize().streamingMicRecognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (isServiceReady()) {
                speedRecognizeService.getSpeedRecognize().stop();
            }
        }
    };
    private final SpeedRecognize.Listener speechServiceListener = new SpeedRecognize.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if (isFinal) {
                voiceRecorder.dismiss();
            }
            Log.e(TAG, "onSpeechRecognized: " + text + " isFinal: " + isFinal);
        }
    };
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            speedRecognizeService = SpeedRecognizeService.from(binder);
            speedRecognizeService.getSpeedRecognize().addListener(speechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            speedRecognizeService = null;
        }

    };

    private VoiceRecorder voiceRecorder;
    private SpeedRecognizeService speedRecognizeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeedRecognizeService.class), serviceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
        if (speedRecognizeService != null) {
            speedRecognizeService.getSpeedRecognize().removeListener(speechServiceListener);
        }
        unbindService(serviceConnection);
        speedRecognizeService = null;

        super.onStop();
    }


    private void startVoiceRecorder() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
        }
        voiceRecorder = new VoiceRecorder(voiceCallback);
        voiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder = null;
        }
    }

    public boolean isServiceReady() {
        return speedRecognizeService != null && speedRecognizeService.getSpeedRecognize() != null;
    }

    public void showStatus(boolean status) {
        Log.e(TAG, "showStatus: " + status);
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance("This app needs to record audio and recognize your speech.")
                .show(getSupportFragmentManager(), "message_dialog");
    }

    @Override
    public void onMessageDialogDismissed() {

    }
}
