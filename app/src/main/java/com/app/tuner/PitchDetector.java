package com.app.tuner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

public class PitchDetector {
    static final int BUFFER_SIZE = 1024 * 8;
    private static final int SAMPLE_RATE = 44100;
    private static final double THRESHOLD = 0.25;
    private static final int SUB_OCTAVES = 4;

    private final short[] buffer = new short[BUFFER_SIZE];
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private PitchDetectionListener listener;
    private Handler backgroundHandler;
    private Handler uiHandler;
    private HandlerThread handlerThread;

    private double lastAmplitude = 0;

    public interface PitchDetectionListener {
        void onPitchDetected(double pitchFrequency);
    }

    public void setPitchDetectionListener(PitchDetectionListener listener) {
        this.listener = listener;
    }

    public synchronized void start(Context context) {
        if (isRecording) return;

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        initializeAudioRecord();

        if (audioRecord == null || audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            return;
        }

        initializeHandlers();
        audioRecord.startRecording();
        isRecording = true;
        backgroundHandler.post(updatePitch);
    }

    @SuppressLint("MissingPermission")
    void initializeAudioRecord() {
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                BUFFER_SIZE
        );
    }

    void initializeHandlers() {
        uiHandler = new Handler(Looper.getMainLooper());
        handlerThread = new HandlerThread("PitchDetectionThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());
    }

    public synchronized void stop() {
        if (!isRecording) return;

        isRecording = false;

        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            audioRecord = null;
        }

        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacks(updatePitch);
            backgroundHandler = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }
    }

    final Runnable updatePitch = new Runnable() {
        @Override
        public void run() {
            if (!isRecording || audioRecord == null) return;

            int bytesRead = audioRecord.read(buffer, 0, BUFFER_SIZE);
            if (bytesRead > 0) {
                double amplitude = calculateAmplitude(buffer);
                lastAmplitude = amplitude;

                if (amplitude > 0.005) {
                    double pitchFrequency = computePitchFrequency(buffer);
                    if (pitchFrequency > 30 && pitchFrequency < 1000) {
                        notifyListener(pitchFrequency);
                    }
                }
            }

            if (isRecording && backgroundHandler != null) {
                backgroundHandler.post(this);
            }
        }
    };

    double calculateAmplitude(short[] audioBuffer) {
        double sum = 0;
        for (short value : audioBuffer) {
            sum += Math.abs(value);
        }
        return sum / audioBuffer.length / 32768.0;
    }

    void notifyListener(double pitchFrequency) {
        if (uiHandler != null && listener != null) {
            uiHandler.post(() -> listener.onPitchDetected(pitchFrequency));
        }
    }

    double computePitchFrequency(short[] audioBuffer) {
        double[] windowedBuffer = applyWindow(audioBuffer);
        int bufferSize = windowedBuffer.length;

        double[] difference = computeAutocorrelation(windowedBuffer);
        double[] cmndf = computeCumulativeMeanNormalizedDifference(difference, bufferSize);

        int lag = findAbsoluteThreshold(cmndf, bufferSize);
        lag = applyOctaveThreshold(bufferSize, lag, cmndf);

        double interpolatedPeak = parabolicInterpolation(cmndf, lag);
        return SAMPLE_RATE / interpolatedPeak;
    }

    double[] applyWindow(short[] audioBuffer) {
        double[] windowed = new double[audioBuffer.length];
        for (int i = 0; i < audioBuffer.length; i++) {
            windowed[i] = audioBuffer[i] / 32768.0;
        }
        return windowed;
    }

    double[] computeAutocorrelation(double[] windowedBuffer) {
        int bufferSize = windowedBuffer.length;
        double[] difference = new double[bufferSize];

        for (int lag = 0; lag < bufferSize; lag++) {
            double sum = 0;
            for (int index = 0; index < bufferSize - lag; index++) {
                double diff = windowedBuffer[index] - windowedBuffer[index + lag];
                sum += diff * diff;
            }
            difference[lag] = sum;
        }
        return difference;
    }

    double[] computeCumulativeMeanNormalizedDifference(double[] difference, int bufferSize) {
        double[] cmndf = new double[bufferSize];
        cmndf[0] = 1;

        for (int lag = 1; lag < bufferSize; lag++) {
            double cumulativeSum = 0;
            for (int i = 1; i <= lag; i++) {
                cumulativeSum += difference[i];
            }
            if (cumulativeSum / lag > 0) {
                cmndf[lag] = difference[lag] / (cumulativeSum / lag);
            } else {
                cmndf[lag] = 1;
            }
        }
        return cmndf;
    }

    int findAbsoluteThreshold(double[] cmndf, int bufferSize) {
        int lag;
        double threshold = THRESHOLD;

        if (lastAmplitude > 0.05) {
            threshold = 0.2;
        } else if (lastAmplitude > 0.02) {
            threshold = 0.25;
        }

        for (lag = 1; lag < bufferSize - 1; lag++) {
            if (cmndf[lag - 1] < threshold) {
                while (lag + 1 < bufferSize && cmndf[lag + 1] < cmndf[lag]) {
                    lag++;
                }
                break;
            }
        }
        return Math.min(lag, bufferSize - 1);
    }

    int applyOctaveThreshold(int bufferSize, int lag, double[] cmndf) {
        int subOctaveSize = bufferSize / SUB_OCTAVES;
        int subOctaveStart = (lag / subOctaveSize) * subOctaveSize;
        int subOctaveEnd = Math.min(subOctaveStart + subOctaveSize, cmndf.length);

        for (int i = subOctaveStart + 1; i < subOctaveEnd; i++) {
            if (cmndf[i] < cmndf[lag]) {
                lag = i;
            }
        }
        return lag;
    }

    double parabolicInterpolation(double[] cmndf, int lag) {
        int x0 = Math.max(lag - 1, 0);
        int x2 = Math.min(lag + 1, cmndf.length - 1);

        if (x0 == lag) {
            return cmndf[lag] <= cmndf[x2] ? lag : x2;
        }

        if (x2 == lag) {
            return cmndf[lag] <= cmndf[x0] ? lag : x0;
        }

        double s0 = cmndf[x0];
        double s1 = cmndf[lag];
        double s2 = cmndf[x2];

        double denominator = 2 * (2 * s1 - s2 - s0);
        if (Math.abs(denominator) < 0.0001) {
            return lag;
        }

        return lag + (s2 - s0) / denominator;
    }

    public boolean isRecording() {
        return isRecording;
    }
}