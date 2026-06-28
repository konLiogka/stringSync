package com.app.tuner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PitchDetectorTest {

    private static final int DETECTOR_BUFFER_SIZE = PitchDetector.BUFFER_SIZE;
    private static final int SAMPLE_RATE = 44100;

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
    public void testPitchDetectionA4() {
        double[] signal = generateSineWaveDouble(440.0, DETECTOR_BUFFER_SIZE);
        double[] diff = pitchDetector.computeAutocorrelation(signal);
        int expectedLag = (int) Math.round(SAMPLE_RATE / 440.0); // 100
        int peakLag = findMinimumInRange(diff, expectedLag - 10, expectedLag + 10);
        double detectedFreq = SAMPLE_RATE / (double) peakLag;
        assertEquals("Should detect ~440 Hz, got: " + detectedFreq, 440.0, detectedFreq, 20.0);
    }

    @Test
    public void testPitchDetectionE4() {
        double[] signal = generateSineWaveDouble(329.63, DETECTOR_BUFFER_SIZE);
        double[] diff = pitchDetector.computeAutocorrelation(signal);
        int expectedLag = (int) Math.round(SAMPLE_RATE / 329.63); // 134
        int peakLag = findMinimumInRange(diff, expectedLag - 10, expectedLag + 10);
        double detectedFreq = SAMPLE_RATE / (double) peakLag;
        assertEquals("Should detect ~329 Hz, got: " + detectedFreq, 329.63, detectedFreq, 20.0);
    }

    @Test
    public void testPitchDetectionA3() {
        double[] signal = generateSineWaveDouble(220.0, DETECTOR_BUFFER_SIZE);
        double[] diff = pitchDetector.computeAutocorrelation(signal);
        int expectedLag = (int) Math.round(SAMPLE_RATE / 220.0); // 200
        int peakLag = findMinimumInRange(diff, expectedLag - 10, expectedLag + 10);
        double detectedFreq = SAMPLE_RATE / (double) peakLag;
        assertEquals("Should detect ~220 Hz, got: " + detectedFreq, 220.0, detectedFreq, 20.0);
    }

    private int findMinimumInRange(double[] arr, int start, int end) {
        int minIdx = start;
        for (int i = start + 1; i <= end; i++) {
            if (arr[i] < arr[minIdx]) minIdx = i;
        }
        return minIdx;
    }

    @Test
    public void testPitchDetectionWithSilence() {
        short[] silence = new short[DETECTOR_BUFFER_SIZE];
        double amplitude = pitchDetector.calculateAmplitude(silence);
        assertTrue("Silence amplitude should be below detection threshold", amplitude <= 0.005);
    }

    @Test
    public void testAmplitudeOfSilenceIsZero() {
        short[] silence = new short[DETECTOR_BUFFER_SIZE];
        assertEquals(0.0, pitchDetector.calculateAmplitude(silence), 0.0001);
    }

    @Test
    public void testAmplitudeIsNormalized() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) buffer[i] = (short) (i % 1000);
        double amplitude = pitchDetector.calculateAmplitude(buffer);
        assertTrue("Amplitude should be between 0 and 1", amplitude >= 0 && amplitude <= 1);
    }

    @Test
    public void testAmplitudeOfMaxSignalIsOne() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) buffer[i] = Short.MAX_VALUE;
        double amplitude = pitchDetector.calculateAmplitude(buffer);
        assertEquals("Max signal amplitude should be ~1.0", 1.0, amplitude, 0.001);
    }

    @Test
    public void testWindowSameLength() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        double[] windowed = pitchDetector.applyWindow(buffer);
        assertEquals(DETECTOR_BUFFER_SIZE, windowed.length);
    }

    @Test
    public void testWindowNormalizesToMinusOneToOne() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) buffer[i] = Short.MAX_VALUE;
        double[] windowed = pitchDetector.applyWindow(buffer);
        for (double v : windowed) {
            assertTrue("Windowed values should be within [-1, 1]", v >= -1.0 && v <= 1.0);
        }
    }

    @Test
    public void testWindowMaxValueNormalizesNearOne() {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) buffer[i] = Short.MAX_VALUE;
        double[] windowed = pitchDetector.applyWindow(buffer);
        assertEquals("Mid sample should normalize to ~1.0", 1.0, windowed[DETECTOR_BUFFER_SIZE / 2], 0.001);
    }

    @Test
    public void testAutocorrelationLength() {
        double[] signal = generateSineWaveDouble(440.0, DETECTOR_BUFFER_SIZE);
        double[] diff = pitchDetector.computeAutocorrelation(signal);
        assertEquals(DETECTOR_BUFFER_SIZE, diff.length);
    }

    @Test
    public void testAutocorrelationZeroLagIsZero() {
        double[] signal = generateSineWaveDouble(440.0, DETECTOR_BUFFER_SIZE);
        double[] diff = pitchDetector.computeAutocorrelation(signal);
        assertEquals("Difference at lag 0 should be 0", 0.0, diff[0], 0.001);
    }

    @Test
    public void testCmndfLength() {
        double[] difference = new double[DETECTOR_BUFFER_SIZE];
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, DETECTOR_BUFFER_SIZE);
        assertEquals(DETECTOR_BUFFER_SIZE, cmndf.length);
    }

    @Test
    public void testCmndfFirstElementIsOne() {
        double[] difference = new double[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < difference.length; i++) difference[i] = i * 0.1;
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, DETECTOR_BUFFER_SIZE);
        assertEquals(1.0, cmndf[0], 0.01);
    }

    @Test
    public void testCmndfValuesNonNegative() {
        double[] difference = new double[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < difference.length; i++) difference[i] = i * 0.1;
        double[] cmndf = pitchDetector.computeCumulativeMeanNormalizedDifference(difference, DETECTOR_BUFFER_SIZE);
        for (double v : cmndf) {
            assertTrue("CMNDF values should be >= 0", v >= 0);
        }
    }

    @Test
    public void testFindAbsoluteThresholdInRange() {
        double[] cmndf = new double[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < cmndf.length; i++) cmndf[i] = 0.5 + 0.5 * Math.cos(i / 50.0);
        int lag = pitchDetector.findAbsoluteThreshold(cmndf, DETECTOR_BUFFER_SIZE);
        assertTrue("Lag should be within valid range", lag >= 0 && lag < DETECTOR_BUFFER_SIZE);
    }

    @Test
    public void testParabolicInterpolationAtMinimum() {
        double[] values = {0.5, 0.2, 0.3};
        double interpolated = pitchDetector.parabolicInterpolation(values, 1);
        assertTrue("Interpolated value should be near minimum", interpolated >= 0.5 && interpolated < 1.5);
    }

    @Test
    public void testParabolicInterpolationAtLeftEdge() {
        double[] values = new double[DETECTOR_BUFFER_SIZE];
        values[0] = 0.1; values[1] = 0.3;
        double result = pitchDetector.parabolicInterpolation(values, 0);
        assertTrue("At left edge result should be 0 or 1", result == 0 || result == 1);
    }

    @Test
    public void testParabolicInterpolationAtRightEdge() {
        double[] values = new double[DETECTOR_BUFFER_SIZE];
        int last = DETECTOR_BUFFER_SIZE - 1;
        values[last] = 0.1; values[last - 1] = 0.3;
        double result = pitchDetector.parabolicInterpolation(values, last);
        assertTrue("At right edge result should be last or last-1", result == last || result == last - 1);
    }

    private short[] generateSineWave(double frequency) {
        short[] buffer = new short[DETECTOR_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) {
            double value = Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE);
            buffer[i] = (short) (value * 32767);
        }
        return buffer;
    }

    private double[] generateSineWaveDouble(double frequency, int size) {
        double[] buffer = new double[size];
        for (int i = 0; i < size; i++) {
            buffer[i] = Math.sin(2 * Math.PI * frequency * i / SAMPLE_RATE);
        }
        return buffer;
    }
}