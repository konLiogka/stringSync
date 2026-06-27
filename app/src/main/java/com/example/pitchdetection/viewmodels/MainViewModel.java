package com.example.pitchdetection.viewmodels;

import android.widget.Button;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.pitchdetection.models.Note;
import com.example.pitchdetection.models.Tuning;

import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends ViewModel {
    private final MutableLiveData<Tuning> currentTuning = new MutableLiveData<>();
    private final MutableLiveData<Note> selectedNote = new MutableLiveData<>();
    private final MutableLiveData<List<String>> tunings = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Button>> stringButtons = new MutableLiveData<>(new ArrayList<>());

    public LiveData<Tuning> getCurrentTuning() {
        return currentTuning;
    }

    public LiveData<Note> getSelectedNote() {
        return selectedNote;
    }

    public LiveData<List<String>> getTunings() {
        return tunings;
    }

    public LiveData<List<Button>> getStringButtons() {
        return stringButtons;
    }

    public void setCurrentTuning(Tuning tuning) {
        currentTuning.setValue(tuning);
    }

    public void setSelectedNote(Note note) {
        selectedNote.setValue(note);
    }

    public void setTunings(List<String> tunings) {
        this.tunings.setValue(tunings);
    }

    public void setStringButtons(List<Button> buttons) {
        this.stringButtons.setValue(buttons);
    }
}