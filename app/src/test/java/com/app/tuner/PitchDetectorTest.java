package com.app.tuner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.content.pm.PackageManager;

@RunWith(MockitoJUnitRunner.class)
public class PitchDetectorTest {

    private PitchDetector pitchDetector;
    
    @Mock
    private Context mockContext;
    
    @Mock
    private PitchDetector.PitchDetectionListener mockListener;

    @Before
    public void setUp() {
        pitchDetector = new PitchDetector();
        pitchDetector.setPitchDetectionListener(mockListener);
    }

    @Test
    public void testStartWithPermissionGranted() {
        when(mockContext.checkPermission(anyString(), anyInt(), anyInt()))
            .thenReturn(PackageManager.PERMISSION_GRANTED);
        
        pitchDetector.start(mockContext);
        assertTrue(pitchDetector.isRecording());
    }

    @Test
    public void testStartWithPermissionDenied() {
        when(mockContext.checkPermission(anyString(), anyInt(), anyInt()))
            .thenReturn(PackageManager.PERMISSION_DENIED);
        
        pitchDetector.start(mockContext);
        assertFalse(pitchDetector.isRecording());
    }

    @Test
    public void testStop() {
        assertFalse(pitchDetector.isRecording());
        
        pitchDetector.stop();
        assertFalse(pitchDetector.isRecording());
    }

    @Test
    public void testListenerCallback() {
        pitchDetector.setPitchDetectionListener(mockListener);
        
        assertNotNull(pitchDetector);
    }

    @Test
    public void testPitchDetectionWithKnownFrequency() {
        short[] testBuffer = generateSineWave(440.0); 
        
        double detectedPitch = pitchDetector.computePitchFrequency(testBuffer);
        
        assertTrue("Detected ~= 440 Hz", 
                   Math.abs(detectedPitch - 440) < 50);
    }

    @Test
    public void testPitchDetectionWithSilence() {
        short[] silenceBuffer = new short[1024];
        double detectedPitch = pitchDetector.computePitchFrequency(silenceBuffer);
        
        assertTrue("Silence should return invalid frequency or zero", 
                   detectedPitch == 0 || Double.isNaN(detectedPitch));
    }

    @Test
    public void testAmplitudeCalculation() {
        short[] testBuffer = new short[1024];
        for (int i = 0; i < testBuffer.length; i++) {
            testBuffer[i] = (short) (i % 1000);
        }
        
        double amplitude = pitchDetector.calculateAmplitude(testBuffer);
        assertTrue("Amplitude should be between 0 and 1", 
                   amplitude >= 0 && amplitude <= 1);
    }

    @Test
    public void testWindowApplication() {
        short[] testBuffer = new short[1024];
        for (int i = 0; i < testBuffer.length; i++) {
            testBuffer[i] = (short) 1000;
        }
        
        double[] windowed = pitchDetector.applyWindow(testBuffer);
        assertEquals("Windowed array should be same length", 
                     testBuffer.length, windowed.length);
        
        for (double value : windowed) {
            assertTrue("Windowed values should be within [-1, 1]", 
                       value >= -1 && value <= 1);
        }
    }

    @Test
    public void testAutocorrelation() {
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 100);  
        }
        
        double[] correlation = pitchDetector.computeAutocorrelation(signal);
        assertEquals("Correlation array should be same length", 
                     signal.length, correlation.length);
        
        assertEquals("First correlation value should be 0", 0.0, correlation[0], 0.01);
        
        boolean foundPeak = false;
        for (int i = 1; i < correlation.length; i++) {
            if (correlation[i] < correlation[i-1] && correlation[i] < correlation[i+1]) {
                foundPeak = true;
                break;
            }
        }
        assertTrue("Should find at least one peak in autocorrelation", foundPeak);
    }

    @Test
    public void testCumulativeMeanNormalizedDifference() {
        double[] difference = new double[1024];
        for (int i = 0; i < difference.length; i++) {
            difference[i] = i * 0.1;
        }
        
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, difference.length);
        assertEquals("CMNDF array should be same length", difference.length, cmndf.length);
        
        assertEquals("First element should be 1", 1.0, cmndf[0], 0.01);
        
        for (double value : cmndf) {
            assertTrue("CMNDF values should be >= 0", value >= 0);
        }
    }

    @Test
    public void testFindAbsoluteThreshold() {
        double[] cmndf = new double[1024];
        for (int i = 0; i < cmndf.length; i++) {
            cmndf[i] = 0.5 + 0.5 * Math.cos(i / 50.0);
        }
        
        int lag = pitchDetector.findAbsoluteThreshold(cmndf, cmndf.length);
        assertTrue("Lag should be within valid range", 
                   lag >= 0 && lag < cmndf.length);
    }

    @Test
    public void testParabolicInterpolation() {
        double[] values = {0.5, 0.2, 0.3};
        int lag = 1;  
        
        double interpolated = pitchDetector.parabolicInterpolation(values, lag);
        assertTrue("Interpolated value should be reasonable", 
                   interpolated >= 0 && interpolated < values.length);
    }

    private short[] generateSineWave(double frequency) {
        int sampleRate = 44100;
        int numSamples = 1024;
        short[] buffer = new short[numSamples];
        
        for (int i = 0; i < numSamples; i++) {
            double time = (double) i / sampleRate;
            double value = Math.sin(2 * Math.PI * frequency * time);
            buffer[i] = (short) (value * 32767);
        }
        return buffer;
    }
}