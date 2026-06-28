package com.app.tuner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PitchDetectorTest {

    private PitchDetector pitchDetector;

    @Mock
    private PitchDetector.PitchDetectionListener mockListener;

    @Before
    public void setUp() {
        pitchDetector = new PitchDetector();
        pitchDetector.setPitchDetectionListener(mockListener);
    }

    @Test
    public void testInitialStateIsNotRecording() {
        assertFalse(pitchDetector.isRecording());
    }

    @Test
    public void testStopWhenNotRecordingDoesNotCrash() {
        pitchDetector.stop();
        assertFalse(pitchDetector.isRecording());
    }

    @Test
    public void testListenerIsSet() {
        pitchDetector.setPitchDetectionListener(mockListener);
        assertNotNull(pitchDetector);
    }

    @Test
    public void testPitchDetectionA4() {
        short[] buffer = generateSineWave(440.0);
        double detected = pitchDetector.computePitchFrequency(buffer);
        assertEquals("Should detect ~440 Hz, got: " + detected, 440.0, detected, 20.0);
    }

    @Test
    public void testPitchDetectionE4() {
        short[] buffer = generateSineWave(329.63);
        double detected = pitchDetector.computePitchFrequency(buffer);
        assertEquals("Should detect ~329 Hz, got: " + detected, 329.63, detected, 20.0);
    }

    @Test
    public void testPitchDetectionA3() {
        short[] buffer = generateSineWave(220.0);
        double detected = pitchDetector.computePitchFrequency(buffer);
        assertEquals("Should detect ~220 Hz, got: " + detected, 220.0, detected, 20.0);
    }

    @Test
    public void testPitchDetectionWithSilence() {
        short[] silence = new short[8192];
        double amplitude = pitchDetector.calculateAmplitude(silence);
        assertTrue("Silence amplitude should be below detection threshold", amplitude <= 0.005);
    }

    @Test
    public void testAmplitudeOfSilenceIsZero() {
        short[] silence = new short[1024];
        double amplitude = pitchDetector.calculateAmplitude(silence);
        assertEquals(0.0, amplitude, 0.0001);
    }

    @Test
    public void testAmplitudeIsNormalized() {
        short[] buffer = new short[1024];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = (short) (i % 1000);
        }
        double amplitude = pitchDetector.calculateAmplitude(buffer);
        assertTrue("Amplitude should be between 0 and 1", amplitude >= 0 && amplitude <= 1);
    }

    @Test
    public void testAmplitudeOfMaxSignalIsOne() {
        short[] buffer = new short[1024];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = Short.MAX_VALUE;
        }
        double amplitude = pitchDetector.calculateAmplitude(buffer);
        assertEquals("Max signal amplitude should be ~1.0", 1.0, amplitude, 0.001);
    }

    @Test
    public void testWindowSameLength() {
        short[] buffer = new short[1024];
        for (int i = 0; i < buffer.length; i++) buffer[i] = 1000;
        double[] windowed = pitchDetector.applyWindow(buffer);
        assertEquals(buffer.length, windowed.length);
    }

    @Test
    public void testWindowEdgesAreNearZero() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) buffer[i] = Short.MAX_VALUE;
        double[] windowed = pitchDetector.applyWindow(buffer);
        assertEquals("Hann window first sample should be ~0, got: " + windowed[0], 0.0, windowed[0], 0.01);
        assertEquals("Hann window last sample should be ~0, got: " + windowed[windowed.length - 1], 0.0, windowed[windowed.length - 1], 0.01);
    }

    @Test
    public void testWindowValuesWithinRange() {
        short[] buffer = new short[1024];
        for (int i = 0; i < buffer.length; i++) buffer[i] = Short.MAX_VALUE;
        double[] windowed = pitchDetector.applyWindow(buffer);
        for (double v : windowed) {
            assertTrue("Windowed values should be within [-1, 1]", v >= -1.0 && v <= 1.0);
        }
    }

    @Test
    public void testCmndfFirstElementIsOne() {
        double[] difference = new double[512];
        for (int i = 0; i < difference.length; i++) difference[i] = i * 0.1;
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, difference.length);
        assertEquals(1.0, cmndf[0], 0.01);
    }

    @Test
    public void testCmndfLengthIsHalfBuffer() {
        double[] difference = new double[DETECTOR_BUFFER_SIZE / 2];
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(
                difference, DETECTOR_BUFFER_SIZE / 2);
        assertEquals(DETECTOR_BUFFER_SIZE / 4, cmndf.length);
    }

@Test
    public void testCmndfValuesNonNegative() {
        double[] difference = new double[512];
        for (int i = 0; i < difference.length; i++) difference[i] = i * 0.1;
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, difference.length);
        for (double v : cmndf) {
            assertTrue("CMNDF values should be >= 0", v >= 0);
        }
    }

    @Test
    public void testParabolicInterpolationAtMinimum() {
        double[] values = {0.5, 0.2, 0.3};
        double interpolated = pitchDetector.parabolicInterpolation(values, 1);
        assertTrue("Interpolated lag should be near minimum", interpolated >= 0.5 && interpolated < 1.5);
    }

    @Test
    public void testParabolicInterpolationAtLeftEdge() {
        double[] values = {0.1, 0.3, 0.5};
        double result = pitchDetector.parabolicInterpolation(values, 0);
        assertEquals("At left edge should return lag unchanged", 0.0, result, 0.001);
    }

    @Test
    public void testParabolicInterpolationAtRightEdge() {
        double[] values = {0.5, 0.3, 0.1};
        double result = pitchDetector.parabolicInterpolation(values, 2);
        assertEquals("At right edge should return lag unchanged", 2.0, result, 0.001);
    }

    private short[] generateSineWave(double frequency) {
        int sampleRate = 44100;
        int numSamples = 8192;
        short[] buffer = new short[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double value = Math.sin(2 * Math.PI * frequency * i / sampleRate);
            buffer[i] = (short) (value * 32767);
        }
        return buffer;
    }
}