package com.app.tuner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.tuner.models.Tuning;

import java.util.List;

public class EditTuningFragment extends Fragment {

    public interface TuningEditListener {
        void onTuningEdited(Tuning tuning, int position);
        void onTuningDeleted(int position);
        void onCloseEdit();
    }

    private RecyclerView recyclerView;
    private List<String> data;
    private EditTuningRecyclerAdapter adapter;
    private TuningEditListener listener;

    public EditTuningFragment(List<String> data) {
        this.data = data;
    }

    public EditTuningFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof TuningEditListener) {
            listener = (TuningEditListener) getParentFragment();
        } else if (getActivity() instanceof TuningEditListener) {
            listener = (TuningEditListener) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_tuning, container, false);

        recyclerView = view.findViewById(R.id.editRecyclerViewer);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        MainActivity activity = (MainActivity) getActivity();
        adapter = new EditTuningRecyclerAdapter(data, activity);
        recyclerView.setAdapter(adapter);

        Button close = view.findViewById(R.id.close);
        close.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCloseEdit();
            }
        });

        return view;
    }

    public void updateData(List<String> newData) {
        this.data = newData;
        if (adapter != null) {
            adapter.updateItems(newData);
        }
    }
}