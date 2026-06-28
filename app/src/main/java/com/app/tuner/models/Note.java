package com.app.tuner.models;

import java.util.Arrays;
import java.util.List;

public class Note {
    private final String name;
    private final double frequency;

    public Note(String name, double frequency) {
        this.name = name;
        this.frequency = frequency;
    }

    public String getName() {
        return name;
    }

    public double getFrequency() {
        return frequency;
    }

    private static final List<Note> NOTES = Arrays.asList(
            new Note("E1", 41.02),
            new Note("B1", 61.74),
            new Note("C2", 65.41),
            new Note("C#2", 69.30),
            new Note("D2", 73.42),
            new Note("D#2", 77.78),
            new Note("E2", 82.41),
            new Note("F2", 87.31),
            new Note("F#2", 92.50),
            new Note("G2", 98.00),
            new Note("G#2", 103.83),
            new Note("A2", 110.00),
            new Note("A#2", 116.54),
            new Note("B2", 123.47),
            new Note("C3", 130.81),
            new Note("C#3", 138.59),
            new Note("D3", 146.83),
            new Note("D#3", 155.56),
            new Note("E3", 164.81),
            new Note("F3", 174.61),
            new Note("F#3", 185.00),
            new Note("G3", 196.00),
            new Note("G#3", 207.65),
            new Note("A3", 220.00),
            new Note("A#3", 233.08),
            new Note("B3", 246.94),
            new Note("C4", 261.63),
            new Note("C#4", 277.18),
            new Note("D4", 293.66),
            new Note("D#4", 311.13),
            new Note("E4", 329.63),
            new Note("F4", 349.23),
            new Note("F#4", 369.99),
            new Note("G4", 392.00),
            new Note("G#4", 415.30),
            new Note("A4", 440.00)
    );

    public static List<Note> getAllNotes() {
        return NOTES;
    }

    public static Note findByName(String name) {
        if (name == null) return null;
        for (Note note : NOTES) {
            if (note.name.equalsIgnoreCase(name)) {
                return note;
            }
        }
        return null;
    }

    public static Note findByFrequency(double frequency) {
        Note closest = null;
        double minDiff = Double.MAX_VALUE;
        for (Note note : NOTES) {
            double diff = Math.abs(note.frequency - frequency);
            if (diff < minDiff) {
                minDiff = diff;
                closest = note;
            }
        }
        return closest;
    }
}