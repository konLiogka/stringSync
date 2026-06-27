package com.example.pitchdetection.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tuning {
    private final String displayName;
    private final List<String> notes;
    private final boolean isAutomatic;

    public Tuning(String displayName, List<String> notes, boolean isAutomatic) {
        this.displayName = displayName;
        this.notes = notes != null ? notes : new ArrayList<>();
        this.isAutomatic = isAutomatic;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getNotes() {
        return notes;
    }

    public boolean isAutomatic() {
        return isAutomatic;
    }

    public static Tuning createAutomatic() {
        return new Tuning("Automatic Tuning", Arrays.asList("", "", "", "", "", ""), true);
    }

    public static Tuning fromString(String tuningString) {
        if (tuningString == null || tuningString.isEmpty()) {
            return createAutomatic();
        }

        try {
            if (tuningString.equals("Automatic Tuning")) {
                return createAutomatic();
            }

            String[] parts = tuningString.split("\\(");
            if (parts.length < 2) {
                return new Tuning(tuningString, Arrays.asList("", "", "", "", "", ""), false);
            }

            String name = parts[0].trim();
            String notesString = parts[1].replace(")", "").trim();
            List<String> notes = Arrays.asList(notesString.split(" "));

            return new Tuning(name, notes, false);
        } catch (Exception e) {
            return new Tuning(tuningString, Arrays.asList("", "", "", "", "", ""), false);
        }
    }

    @Override
    public String toString() {
        if (isAutomatic) {
            return "Automatic Tuning";
        }
        return displayName + " (" + String.join(" ", notes) + ")";
    }
}