package com.lalagrass.dbmeter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

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
        @Override
        protected Void doInBackground(Void... params) {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                if (bufferSize < frequency)
                    bufferSize = frequency;
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);
                byte[] buffer = new byte[bufferSize];
                int offset = 0;
                audioRecord.startRecording();
                while (isRecording) {
                    while (offset < bufferSize) {
                        int bufferReadResult = audioRecord.read(buffer, offset,
                                bufferSize - offset);
                        offset+= bufferReadResult;
                    }


                        long sum = 0;
                        for (int i = 0; i < bufferSize; i += 2) {
                            sum += Math.pow((buffer[i] & 0xff) << 8 | buffer[i + 1], 2);
                        }
                        double amplitude = (double) (sum / (bufferSize / 2));
                        double amplitudeDb = 20 * Math
                                .log10(Math.abs(amplitude) / 32768);
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
            double value = progress[0];
            if (value < 0) {
                statusText.setText("0.0");
            } else {
                statusText.setText(String.format("%.01f", value));
            }
        }
        protected void onPostExecute(Void result) {
        }
    }
}
