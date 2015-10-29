package com.lalagrass.dbmeter;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String LogTag = "DBMeterLog";
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
Log.i(LogTag, "RecordAudio");
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);
                Log.i(LogTag, "getMinBufferSize " + bufferSize);
                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);
                Log.i(LogTag, "AudioRecord");
                short[] buffer = new short[bufferSize];
                audioRecord.startRecording();
                Log.i(LogTag, "AudioRecord start");
                while (isRecording) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            bufferSize);
                    long sum = 0;
                    for (int i = 0; i < bufferReadResult; i+=2) {
                        sum += (buffer[i] & 0xff) << 8 | buffer[i+1];
                    }
                    double amplitude = (double) (sum / (bufferSize / 2));
                    double amplitudeDb = 20 * Math
                            .log10(Math.abs(amplitude) / 32768);
                    publishProgress(amplitudeDb);
                }
                audioRecord.stop();
            } catch (Throwable t) {
                Log.e("AudioRecord", "Recording Failed");
            }
            return null;
        }
        protected void onProgressUpdate(Double... progress) {
            statusText.setText(String.format("%.02f", progress[0]));
        }
        protected void onPostExecute(Void result) {
        }
    }
}
