package com.nhancv.livetranscribe;

import android.support.annotation.NonNull;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.ClientStream;
import com.google.api.gax.rpc.ResponseObserver;
import com.google.api.gax.rpc.StreamController;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.StreamingRecognitionConfig;
import com.google.cloud.speech.v1.StreamingRecognitionResult;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

//https://cloud.google.com/speech-to-text/docs/reference/libraries
//https://github.com/googleapis/google-cloud-java/tree/master/google-cloud-clients/google-cloud-speech
public class SpeedRecognize {
    private static final String TAG = SpeedRecognize.class.getSimpleName();

    private static final int SAMPLE_RATE = 16000;
    private ArrayList<Listener> listeners;
    private ResponseObserver<StreamingRecognizeResponse> responseObserver;
    private SpeechSettings speechSettings;
    private SpeechClient speechClient;
    private ClientStream<StreamingRecognizeRequest> clientStream;

    public SpeedRecognize(InputStream credentialsStream) {
        try {
            // Instantiates a client
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);
            speechSettings =
                    SpeechSettings.newBuilder()
                            .setCredentialsProvider(credentialsProvider)
                            .setExecutorProvider(FixedExecutorProvider.create(Executors.newScheduledThreadPool(
                                    Math.max(2, Math.min(Runtime.getRuntime().availableProcessors() - 1, 4)))))
                            .build();
            speechClient = SpeechClient.create(speechSettings);

        } catch (IOException e) {
            e.printStackTrace();
        }

        listeners = new ArrayList<>();
        responseObserver = new ResponseObserver<StreamingRecognizeResponse>() {

            @Override
            public void onStart(StreamController controller) {
            }

            @Override
            public void onResponse(StreamingRecognizeResponse response) {
                List<StreamingRecognitionResult> resultList = response.getResultsList();
                if (resultList != null && !resultList.isEmpty()) {
                    StreamingRecognitionResult result = response.getResultsList().get(0);
                    SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                    for (Listener listener : listeners) {
                        listener.onSpeechRecognized(alternative.getTranscript(), result.getIsFinal());
                    }
                }

            }

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
        };
    }

    public void addListener(@NonNull Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(@NonNull Listener listener) {
        listeners.remove(listener);
    }

    public void start() {
        start(SAMPLE_RATE);
    }

    public void start(int sampleRateHertz) {
        if (speechClient != null && !speechClient.isShutdown()) {
            clientStream =
                    speechClient.streamingRecognizeCallable().splitCall(responseObserver);
            RecognitionConfig recognitionConfig =
                    RecognitionConfig.newBuilder()
                            .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                            .setLanguageCode("en-US")
                            .setSampleRateHertz(sampleRateHertz)
                            .build();
            StreamingRecognitionConfig streamingRecognitionConfig =
                    StreamingRecognitionConfig.newBuilder().setConfig(recognitionConfig).build();

            StreamingRecognizeRequest request =
                    StreamingRecognizeRequest.newBuilder()
                            .setStreamingConfig(streamingRecognitionConfig)
                            .build();
            clientStream.send(request);
        }
    }

    public void stop() {
        if (responseObserver != null) {
            responseObserver.onComplete();
        }
        if (clientStream != null) {
            clientStream.closeSend();
        }
    }

    public void destroy() {
        if (speechClient != null && !speechClient.isShutdown()) {
            speechClient.shutdown();
        }
    }

    /**
     * Performs microphone streaming speech recognition with a duration of 1 minute.
     */
    public void streamingMicRecognize(byte[] data, int size) {
        try {
            StreamingRecognizeRequest request =
                    StreamingRecognizeRequest.newBuilder()
                            .setAudioContent(ByteString.copyFrom(data, 0, size))
                            .build();
            clientStream.send(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        responseObserver.onComplete();
    }

    public interface Listener {

        /**
         * Called when a new piece of text was recognized by the Speech API.
         *
         * @param text The text.
         * @param isFinal {@code true} when the API finished processing audio.
         */
        void onSpeechRecognized(String text, boolean isFinal);

    }

    /**
     * Synchronous speech recognition returns the recognized text for short audio (less than ~1 minute) in the response as soon as it is processed.
     * https://cloud.google.com/speech-to-text/docs/sync-recognize
     * InputStream credentialsStream = context.getResources().openRawResource(R.raw.credentials);
     * InputStream inputStream = context.getResources().openRawResource(R.raw.audio);
     */
    public static void recognizeShortAudioFile(InputStream credentialsStream, InputStream rawAudioFileInputStream, Listener listener) throws IOException {
        // Instantiates a client
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
        FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        SpeechSettings speechSettings =
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(credentialsProvider)
                        .build();

        // Instantiates a client
        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
            // Builds the sync recognize request
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("en-US")
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.readFrom(rawAudioFileInputStream))
                    .build();

            // Performs speech recognition on the audio file
            RecognizeResponse response = speechClient.recognize(config, audio);
            List<SpeechRecognitionResult> results = response.getResultsList();

            for (int i = 0; i < results.size(); i++) {
                SpeechRecognitionResult result = results.get(i);
                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
                if (listener != null) {
                    listener.onSpeechRecognized(alternative.getTranscript(), true);
                }
            }
        }
    }

}
