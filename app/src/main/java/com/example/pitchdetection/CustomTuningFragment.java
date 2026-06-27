package com.example.pitchdetection;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

import com.example.pitchdetection.models.Tuning;

import java.util.Arrays;

public class CustomTuningFragment extends Fragment {

    public interface TuningAddListener {
        void onTuningAdded(Tuning tuning);
        void onTuningEdited(Tuning tuning, int position);
        void onCancel();
    }

    private String tuningString = "";
    private int position = 0;
    private TuningAddListener listener;

    public CustomTuningFragment(String tuningString, int position) {
        this.tuningString = tuningString;
        this.position = position;
    }

    public CustomTuningFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof TuningAddListener) {
            listener = (TuningAddListener) getParentFragment();
        } else if (getActivity() instanceof TuningAddListener) {
            listener = (TuningAddListener) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_custom_tuning, container, false);

        EditText editText = view.findViewById(R.id.customName);
        Spinner spin6 = view.findViewById(R.id.spin6);
        Spinner spin5 = view.findViewById(R.id.spin5);
        Spinner spin4 = view.findViewById(R.id.spin4);
        Spinner spin3 = view.findViewById(R.id.spin3);
        Spinner spin2 = view.findViewById(R.id.spin2);
        Spinner spin1 = view.findViewById(R.id.spin1);
        Button cancel = view.findViewById(R.id.cancel);
        Button add = view.findViewById(R.id.add);

        setTextSpinner(spin6, R.array.spin6);
        setTextSpinner(spin5, R.array.spin5);
        setTextSpinner(spin4, R.array.spin4);
        setTextSpinner(spin3, R.array.spin3);
        setTextSpinner(spin2, R.array.spin2);
        setTextSpinner(spin1, R.array.spin1);

        if (tuningString.isEmpty()) {
            for (Spinner spin : Arrays.asList(spin1, spin2, spin3, spin4, spin5, spin6)) {
                spin.setSelection(5);
            }

            add.setOnClickListener(v -> {
                String name = editText.getText().toString();
                if (name.isEmpty()) {
                    editText.setHintTextColor(Color.parseColor("#DC3423"));
                    return;
                }

                String s6 = spin6.getSelectedItem().toString();
                String s5 = spin5.getSelectedItem().toString();
                String s4 = spin4.getSelectedItem().toString();
                String s3 = spin3.getSelectedItem().toString();
                String s2 = spin2.getSelectedItem().toString();
                String s1 = spin1.getSelectedItem().toString();
                String displayName = name + "(" + s6 + " " + s5 + " " + s4 + " " + s3 + " " + s2 + " " + s1.toLowerCase() + ")";

                Tuning tuning = Tuning.fromString(displayName);
                if (listener != null) {
                    listener.onTuningAdded(tuning);
                }
            });
        } else {
            String nameString = tuningString.substring(0, tuningString.indexOf("(") - 1);
            editText.setText(nameString);

            String notesString = tuningString.substring(tuningString.indexOf("(") + 1, tuningString.indexOf(")"));
            String[] notes = notesString.split(" ");

            int spinIndex = 0;
            for (Spinner spin : Arrays.asList(spin6, spin5, spin4, spin3, spin2, spin1)) {
                for (int i = 0; i < spin.getCount(); i++) {
                    String spinnerItem = spin.getItemAtPosition(i).toString();
                    if (spinnerItem.equalsIgnoreCase(notes[spinIndex])) {
                        spin.setSelection(i);
                        break;
                    }
                }
                spinIndex++;
            }

            add.setText("Update");
            add.setOnClickListener(v -> {
                String name = editText.getText().toString();
                if (name.isEmpty()) {
                    editText.setHintTextColor(Color.parseColor("#DC3423"));
                    return;
                }

                String s6 = spin6.getSelectedItem().toString();
                String s5 = spin5.getSelectedItem().toString();
                String s4 = spin4.getSelectedItem().toString();
                String s3 = spin3.getSelectedItem().toString();
                String s2 = spin2.getSelectedItem().toString();
                String s1 = spin1.getSelectedItem().toString();
                String displayName = name + "(" + s6 + " " + s5 + " " + s4 + " " + s3 + " " + s2 + " " + s1.toLowerCase() + ")";

                Tuning tuning = Tuning.fromString(displayName);
                if (listener != null) {
                    listener.onTuningEdited(tuning, position);
                }
            });
        }

        cancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCancel();
            }
        });

        InputFilter filter = (source, start, end, dest, dstart, dend) -> {
            int length = editText.getText().length();
            if (length > 8) {
                return "";
            }
            for (int i = start; i < end; i++) {
                if (!Character.isLetterOrDigit(source.charAt(i))) {
                    return "";
                }
            }
            return null;
        };
        editText.setFilters(new InputFilter[]{filter});

        return view;
    }

    public void setTextSpinner(Spinner spinner, int array) {
        Context context = spinner.getContext();
        String[] dataArray = context.getResources().getStringArray(array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                R.layout.custom_spinner_text, R.id.textView1, dataArray);
        spinner.setAdapter(adapter);
    }
}