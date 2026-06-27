package com.example.pitchdetection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pitchdetection.models.Tuning;

import java.util.List;

public class TuningFragment extends Fragment implements TuningRecyclerAdapter.OnItemClickListener {

    public interface TuningSelectionListener {
        void onTuningSelected(Tuning tuning);
    }

    private RecyclerView recyclerView;
    private List<String> data;
    private TuningRecyclerAdapter adapter;
    private TuningSelectionListener listener;

    public TuningFragment(List<String> data) {
        this.data = data;
    }

    public TuningFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() instanceof TuningSelectionListener) {
            listener = (TuningSelectionListener) getParentFragment();
        } else if (getActivity() instanceof TuningSelectionListener) {
            listener = (TuningSelectionListener) getActivity();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tuning, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewer);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new TuningRecyclerAdapter(data);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onItemClick(String[] notes, String clickedItem, RecyclerView recyclerView) {
        if (listener != null) {
            Tuning tuning = Tuning.fromString(clickedItem);
            listener.onTuningSelected(tuning);
        }
    }

    public void updateData(List<String> newData) {
        this.data = newData;
        if (adapter != null) {
            adapter.updateItems(newData);
        }
    }
}