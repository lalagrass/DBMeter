package com.lalagrass.dbmeter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final int minimum = 1;
    private static volatile boolean isRecording = false;
    private static final int frequency = 44100;
    private static final int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusText = (TextView) this.findViewById(R.id.StatusTextView);
    }

    @Override
    protected void onPause() {
        SetRecordingState(false);
        super.onPause();
    }

    @Override
    protected void onResume() {
        SetRecordingState(true);
        super.onResume();
    }

    private synchronized void SetRecordingState(boolean on) {
        isRecording = on;
        if (isRecording) {
            new RecordAudio().execute();
        }
    }

    private class RecordAudio extends AsyncTask<Void, Double, Void> {
        private int avgCount = 1;
        private double avg = 0;
        private int count = 0;
        @Override
        protected Void doInBackground(Void... params) {
            try {

                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                avgCount = frequency / bufferSize;
                if (avgCount < 1)
                    avgCount = 1;
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);
                short[] buffer = new short[bufferSize];
                int offset = 0;
                audioRecord.startRecording();
                while (isRecording) {
                    while (offset < bufferSize) {
                        int bufferReadResult = audioRecord.read(buffer, offset,
                                bufferSize - offset);
                        offset += bufferReadResult;
                    }

                    double sum = 0;
                    for (int i = 0; i < bufferSize; i++) {
                        sum += Math.abs(buffer[i]);
                    }
                    double amplitude = sum / bufferSize;
                    double amplitudeDb = 20 * Math
                            .log10(amplitude / minimum);
                    offset = 0;
                    publishProgress(amplitudeDb);
                }
                audioRecord.stop();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        protected void onProgressUpdate(Double... progress) {
            avg += progress[0];
            if (++count >= avgCount) {
                double value = avg / avgCount;
                avg = 0;
                count = 0;
                statusText.setText(String.format("%.01f", value));
            }
        }

        protected void onPostExecute(Void result) {
        }
    }
}
