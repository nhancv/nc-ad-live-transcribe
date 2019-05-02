package com.nhancv.livetranscribe;

import android.content.Context;
import android.util.Log;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.rpc.BidiStream;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.StreamingRecognizeRequest;
import com.google.cloud.speech.v1.StreamingRecognizeResponse;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

public class SpeedRecognize {
    private static final String TAG = SpeedRecognize.class.getSimpleName();

    public void test(Context context) throws IOException {

        // Instantiates a client
        InputStream credentialsStream = context.getResources().openRawResource(R.raw.credentials);
        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
        FixedCredentialsProvider credentialsProvider = FixedCredentialsProvider.create(credentials);

        SpeechSettings speechSettings =
                SpeechSettings.newBuilder()
                        .setCredentialsProvider(credentialsProvider)
                        .build();

        // Instantiates a client
        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {

            // Reads the audio file into memory
            InputStream inputStream = context.getResources().openRawResource(R.raw.audio);

            // Builds the sync recognize request
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(16000)
                    .setLanguageCode("en-US")
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(ByteString.readFrom(inputStream))
                    .build();

            // Performs speech recognition on the audio file
//        RecognizeResponse response = speechClient.recognize(config, audio);
//        List<SpeechRecognitionResult> results = response.getResultsList();
//
//        for (SpeechRecognitionResult result : results) {
//            // There can be several alternative transcripts for a given chunk of speech. Just use the
//            // first (most likely) one here.
//            SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//            System.out.printf("Transcription: %s%n", alternative.getTranscript());
//        }

//            LongRunningRecognizeRequest request = LongRunningRecognizeRequest.newBuilder()
//                    .setConfig(config)
//                    .setAudio(audio)
//                    .build();
            Log.e(TAG, "test: 0");
//            LongRunningRecognizeResponse response = speechClient.longRunningRecognizeAsync(request).get();

//            for (SpeechRecognitionResult result : response.getResultsList()) {
//                SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
//                Log.e(TAG, String.format("Transcription: %s%n", alternative.getTranscript()));
//            }


            BidiStream<StreamingRecognizeRequest, StreamingRecognizeResponse> bidiStream =
                    speechClient.streamingRecognizeCallable().call();

            StreamingRecognizeRequest request = StreamingRecognizeRequest.newBuilder().build();
            bidiStream.send(request);
            for (StreamingRecognizeResponse response : bidiStream) {
                // Do something when receive a response
            }


            Log.e(TAG, "test: 1");

        }
    }
}
