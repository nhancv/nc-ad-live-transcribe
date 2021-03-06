package com.nhancv.livetranscribe;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private TextView tvStatus;
    private ImageView ivRecord;

    // TODO: 2019-06-29 Add speech recognition
    /** SpeechRecognizeService setup **/
    private final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (isServiceReady()) {
                speechRecognizeService.getSpeechRecognize().start(voiceRecorder.getSampleRate());
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (isServiceReady()) {
                speechRecognizeService.getSpeechRecognize().streamingMicRecognize(data, size);
            }
        }

        @Override
        public void onVoiceEnd() {
            showStatus(false);
            if (isServiceReady()) {
                speechRecognizeService.getSpeechRecognize().stop();
            }
        }
    };
    private final SpeechRecognize.Listener speechServiceListener = new SpeechRecognize.Listener() {
        @Override
        public void onSpeechRecognized(final String text, boolean isFinal) {
            if (isFinal) {
                voiceRecorder.dismiss();
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvStatus.setText(text);
                }
            });
            Log.d(TAG, "onSpeechRecognized: " + text + " isFinal: " + isFinal);
        }
    };
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            speechRecognizeService = SpeechRecognizeService.from(binder);
            speechRecognizeService.getSpeechRecognize().addListener(speechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            speechRecognizeService = null;
        }

    };

    private SpeechRecognizeService speechRecognizeService;
    /** SpeechRecognizeService end-setup **/

    private VoiceRecorder voiceRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tv_status);
        ivRecord = findViewById(R.id.iv_record);

        Button button = findViewById(R.id.ib_file);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                v.setEnabled(false);
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            SpeechRecognize.recognizeShortAudioFile(getResources().openRawResource(R.raw.credentials),
                                    getResources().openRawResource(R.raw.audio),
                                    new SpeechRecognize.Listener() {
                                        @Override
                                        public void onSpeechRecognized(final String text, boolean isFinal) {
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    v.setEnabled(true);
                                                    tvStatus.setText(text);
                                                }
                                            });
                                        }
                                    }
                            );
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechRecognizeService.class), serviceConnection, BIND_AUTO_CREATE);

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
        if (speechRecognizeService != null) {
            speechRecognizeService.getSpeechRecognize().removeListener(speechServiceListener);
        }
        unbindService(serviceConnection);
        speechRecognizeService = null;

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
        return speechRecognizeService != null && speechRecognizeService.getSpeechRecognize() != null;
    }

    public void showStatus(final boolean status) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ivRecord.setImageResource(status ? R.drawable.ic_record_voice_active : R.drawable.ic_record_voice);
            }
        });
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance("This app needs to record audio and recognize your speech.")
                .show(getSupportFragmentManager(), "message_dialog");
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }
}
