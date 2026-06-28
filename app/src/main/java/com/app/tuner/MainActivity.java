package com.app.tuner;

import static com.app.tuner.TuningsList.*;

import android.Manifest;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.app.tuner.models.Note;
import com.app.tuner.models.Tuning;
import com.app.tuner.viewmodels.MainViewModel;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        PitchDetector.PitchDetectionListener,
        View.OnClickListener,
        TuningFragment.TuningSelectionListener,
        EditTuningFragment.TuningEditListener,
        CustomTuningFragment.TuningAddListener {

    private static final double MIN_PITCH_FREQUENCY = 40;
    private static final double MAX_PITCH_FREQUENCY = 500;
    private static final int MAX_NOTE_CENTS = 500;
    private static final int PERMISSION_REQUEST_CODE = 123;

    private TextView pitchTextView;
    private TextView noteTextView;
    private TextView noteFreqTextView;
    private TextView tuningText;
    private Button s1, s2, s3, s4, s5, s6;
    private ImageView pointer;
    private ConstraintLayout visibleArea;

    private PitchDetector pitchDetector;
    private MainViewModel viewModel;

    private Fragment currentFragment = null;
    private List<String> data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initializeViews();
        initializeViewModel();
        setupObservers();
        setupUI();
        setupListeners();
        setupPitchDetector();
        loadTunings();
    }

    private void initializeViews() {
        pitchTextView = findViewById(R.id.freq);
        noteFreqTextView = findViewById(R.id.freq2);
        noteTextView = findViewById(R.id.note);
        tuningText = findViewById(R.id.tuningText);
        pointer = findViewById(R.id.pointer);
        visibleArea = findViewById(R.id.constraintL);

        s1 = findViewById(R.id.s1);
        s2 = findViewById(R.id.s2);
        s3 = findViewById(R.id.s3);
        s4 = findViewById(R.id.s4);
        s5 = findViewById(R.id.s5);
        s6 = findViewById(R.id.s6);
    }

    private void initializeViewModel() {
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        List<Button> buttons = Arrays.asList(s1, s2, s3, s4, s5, s6);
        viewModel.setStringButtons(buttons);
    }

    private void setupObservers() {
        viewModel.getCurrentTuning().observe(this, tuning -> {
            if (tuning != null) {
                updateUIForTuning(tuning);
            }
        });

        viewModel.getSelectedNote().observe(this, note -> {
            if (note != null) {
                updateUIForNote(note);
            }
        });

        viewModel.getTunings().observe(this, tunings -> {
            this.data = tunings;
            updateFragmentsWithData(tunings);
        });
    }

    private void setupUI() {
        Tuning defaultTuning = Tuning.fromString("E Standard(E2 A2 D3 G3 B3 e4)");
        viewModel.setCurrentTuning(defaultTuning);
    }

    private void setupListeners() {
        for (Button button : Arrays.asList(s1, s2, s3, s4, s5, s6)) {
            button.setOnClickListener(this);
        }

        CardView cardView = findViewById(R.id.tuningCardView);
        cardView.setOnClickListener(view -> onClickTuningsView());

        ImageView options = findViewById(R.id.options);
        options.setOnClickListener(view -> showOptionsMenu());
    }

    private void setupPitchDetector() {
        pitchDetector = new PitchDetector();
        pitchDetector.setPitchDetectionListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
        } else {
            pitchDetector.start(this);
        }
    }

    private void loadTunings() {
        data = getList(this);
        if (data.isEmpty()) {
            data = TuningsList.fillList(this);
            saveList(this, data);
        }
        viewModel.setTunings(data);
    }

    private void updateUIForTuning(Tuning tuning) {
        List<String> notes = tuning.getNotes();
        tuningText.setText(tuning.getDisplayName());

        List<Button> buttons = Arrays.asList(s1, s2, s3, s4, s5, s6);
        for (int i = 0; i < buttons.size() && i < notes.size(); i++) {
            buttons.get(i).setText(notes.get(i));
        }

        if (!tuning.isAutomatic() && !notes.isEmpty()) {
            Button firstButton = buttons.get(0);
            selectedString(firstButton);
        } else {
            clearStringSelection();
        }
    }

    private void updateUIForNote(Note note) {
        noteTextView.setText(note.getName());
        noteFreqTextView.setText(String.format("%.2f Hz", note.getFrequency()));
    }

    private void selectedString(Button button) {
        resetButtonColors();
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#A2FF86")));
        Note note = Note.findByName(button.getText().toString());
        if (note != null) {
            viewModel.setSelectedNote(note);
        }
    }

    private void resetButtonColors() {
        int color = Color.parseColor("#404040");
        for (Button button : Arrays.asList(s1, s2, s3, s4, s5, s6)) {
            button.setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    private void clearStringSelection() {
        resetButtonColors();
        viewModel.setSelectedNote(null);
        noteTextView.setText("");
        noteFreqTextView.setText("");
    }

    private void updateFragmentsWithData(List<String> tunings) {
        if (currentFragment instanceof TuningFragment) {
            ((TuningFragment) currentFragment).updateData(tunings);
        } else if (currentFragment instanceof EditTuningFragment) {
            ((EditTuningFragment) currentFragment).updateData(tunings);
        }
    }

    private void showOptionsMenu() {
        Context wrapper = new ContextThemeWrapper(getApplicationContext(), R.style.PopupMenu);
        PopupMenu popupMenu = new PopupMenu(wrapper, findViewById(R.id.options));
        popupMenu.getMenuInflater().inflate(R.menu.popup_menu, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(menuItem -> {
            String title = menuItem.getTitle().toString();
            if (title.equals("Add Tuning")) {
                onClickCustomTView("", 0);
            } else if (title.equals("Edit Tuning")) {
                onClickEditTView();
            }
            return true;
        });
        popupMenu.show();
    }

    public void onClickTuningsView() {
        if (currentFragment instanceof TuningFragment) {
            closeFragment(currentFragment);
        } else {
            openFragment(new TuningFragment(data), R.id.fragmentTuning);
            resetButtonColors();
        }
    }

    public void onClickCustomTView(String string, int position) {
        if (currentFragment instanceof CustomTuningFragment) {
            closeFragment(currentFragment);
        } else {
            openFragment(new CustomTuningFragment(string, position), R.id.fragmentCustomTuning);
        }
    }

    public void onClickEditTView() {
        if (currentFragment instanceof EditTuningFragment) {
            closeFragment(currentFragment);
        } else {
            openFragment(new EditTuningFragment(data), R.id.fragmentEditTuning);
        }
    }

    private void openFragment(Fragment fragment, int containerViewId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if (currentFragment != null) {
            fragmentTransaction.remove(currentFragment);
        }
        pitchDetector.stop();
        fragmentTransaction.replace(containerViewId, fragment).commit();
        currentFragment = fragment;
    }

    private void closeFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.remove(fragment).commit();
        pitchDetector.start(this);
        currentFragment = null;
    }

    @Override
    public void onTuningSelected(Tuning tuning) {
        viewModel.setCurrentTuning(tuning);
        closeFragment(currentFragment);
    }

    @Override
    public void onTuningEdited(Tuning tuning, int position) {
        String tuningString = tuning.toString();
        replaceToList(tuningString, this, position + 1); // +1 for Automatic Tuning
        data = getList(this);
        viewModel.setTunings(data);
        closeFragment(currentFragment);
    }

    @Override
    public void onTuningDeleted(int position) {
        removeFromList(this, position + 1); // +1 for Automatic Tuning
        data = getList(this);
        viewModel.setTunings(data);
        closeFragment(currentFragment);
    }

    @Override
    public void onCloseEdit() {
        closeFragment(currentFragment);
    }

    @Override
    public void onTuningAdded(Tuning tuning) {
        addToList(tuning.toString(), this);
        data = getList(this);
        viewModel.setTunings(data);
        closeFragment(currentFragment);
    }

    @Override
    public void onCancel() {
        closeFragment(currentFragment);
    }

    @Override
    public void onPitchDetected(final double pitchFrequency) {
        Tuning currentTuning = viewModel.getCurrentTuning().getValue();
        Note selectedNote = viewModel.getSelectedNote().getValue();

        if (currentTuning == null) return;

        if (selectedNote != null) {
            noteTextView.setText(selectedNote.getName());
        }

        if (pitchFrequency > MIN_PITCH_FREQUENCY && pitchFrequency < MAX_PITCH_FREQUENCY) {
            if (currentTuning.isAutomatic()) {
                handleAutomaticTuning(pitchFrequency);
            } else if (selectedNote != null) {
                handleSpecificTuning(pitchFrequency, selectedNote);
            }
        }
    }

    private void handleAutomaticTuning(double pitchFrequency) {
        for (Note note : Note.getAllNotes()) {
            double cents = calculateCents(pitchFrequency, note.getFrequency());
            if (Math.abs(cents) <= 50) {
                pitchTextView.setText(String.format("%.2f", pitchFrequency) + " Hz");
                viewModel.setSelectedNote(note);
                updatePointerPosition(cents);
                break;
            }
        }
    }

    private void handleSpecificTuning(double pitchFrequency, Note targetNote) {
        double cents = calculateCents(pitchFrequency, targetNote.getFrequency());
        if (Math.abs(cents) <= MAX_NOTE_CENTS) {
            pitchTextView.setText(String.format("%.2f", pitchFrequency) + " Hz");
            updatePointerPosition(cents);
            updateTuningStatus(cents);
        }
    }

    private double calculateCents(double pitchFrequency, double targetFrequency) {
        return 1200 * Math.log(pitchFrequency / targetFrequency) / Math.log(2);
    }

    private void updatePointerPosition(double cents) {
        int maxOffset = 500;
        double maxCents = 50.0;
        double minCents = -50.0;

        double rangeCents = maxCents - minCents;
        double offset = (cents - minCents) / rangeCents * (2 * maxOffset) - maxOffset;

        int visibleWidth = visibleArea.getWidth();
        int indicatorWidth = pointer.getWidth();
        int maxVisibleOffset = visibleWidth - indicatorWidth;

        offset = Math.max(-maxVisibleOffset / 2.0, Math.min(maxVisibleOffset / 2.0, offset));
        float centerX = visibleArea.getX() + visibleWidth / 2f;
        float targetX = centerX + (float) offset;
        pointer.setTranslationX(targetX);
    }

    private void updateTuningStatus(double cents) {
        Tuning currentTuning = viewModel.getCurrentTuning().getValue();
        if (currentTuning == null || currentTuning.isAutomatic()) return;

        float centerX = visibleArea.getX() + visibleArea.getWidth() / 2f;
        float threshold = 0.2f * centerX;
        float pointerX = pointer.getTranslationX();

        if (pointerX > centerX + threshold) {
            pitchTextView.setText("Tune DOWN!");
        } else if (pointerX < centerX - threshold) {
            pitchTextView.setText("Tune UP!");
        } else {
            pitchTextView.setText("OK");
        }
    }

    @Override
    public void onClick(View v) {
        Tuning currentTuning = viewModel.getCurrentTuning().getValue();
        if (currentTuning == null || currentTuning.isAutomatic()) {
            return;
        }

        Button clickedButton = (Button) v;
        if (Arrays.asList(s1, s2, s3, s4, s5, s6).contains(clickedButton)) {
            selectedString(clickedButton);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            pitchDetector.start(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        pitchDetector.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pitchDetector.start(this);
            }
        }
    }

    public void addTuning(String string, Context context) {
        addToList(string, context);
        data = getList(context);
        viewModel.setTunings(data);
        onClickTuningsView();
    }

    public void replaceTuning(String string, Context context, int position) {
        replaceToList(string, context, position);
        data = getList(context);
        viewModel.setTunings(data);
        onClickTuningsView();
    }

    public void removeTuning(Context context, int position) {
        removeFromList(context, position);
        data = getList(context);
        viewModel.setTunings(data);
        onClickTuningsView();
    }
}