package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.adapters.WishAdapter;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.helpers.DateHelper;

public class WishesCounterFragment extends Fragment {
    private MainActivity mainActivity;

    private TextView savedWishesCounter;

    private Button addActions;

    private DatabaseHelper databaseHelper;
    private DateHelper dateHelper;

    private String[] wishes;

    private WishAdapter adapter;
    private RecyclerView recyclerView;

    public WishesCounterFragment() {}
    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        databaseHelper = new DatabaseHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishes_counter, container, false);

        addActions = view.findViewById(R.id.addActionsButton);

        savedWishesCounter = view.findViewById(R.id.wishesCounter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getParentFragmentManager().setFragmentResultListener("refresh_wishes_counter_key", getViewLifecycleOwner(), (requestKey, result) -> {
            boolean shouldRefresh = result.getBoolean("shouldRefresh", false);
            if (shouldRefresh) {
                refreshData(view);
            }
        });

        addActions.setOnClickListener(this::showMoreMenu);

        dateHelper = new DateHelper(mainActivity);

        wishes = new String[]{"Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань"};

        recyclerView = view.findViewById(R.id.wishesLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new WishAdapter(wishes);
        recyclerView.setAdapter(adapter);

        refreshData(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshData(requireView());
    }

    private void refreshData(View view) {
        ImageView wishIcon = view.findViewById(R.id.wishIconWishCounter);

        switch (mainActivity.getSubType()) {
            case 0:
                wishIcon.setImageResource(R.drawable.intertwined_fate);
                break;
            case 1:
                wishIcon.setImageResource(R.drawable.star_rail_special_pass);
                break;
            case 2:
                wishIcon.setImageResource(R.drawable.encrypted_master_tape);
                break;
        }

        updateProgress();

        wishes = new String[]{"Москва", "Санкт-Петербург", "Новосибирск", "Екатеринбург", "Казань"};
        adapter = new WishAdapter(wishes);
        recyclerView.setAdapter(adapter);
    }

    private void updateProgress() {
        savedWishesCounter.setText("0");
    }

    private void showMoreMenu(View view) {
        ((MainActivity) requireActivity()).showCounterMenu(new MainActivity.OnCounterMenuClickListener() {
            @Override
            public void onAddOneAttempt() { temp(); }

            @Override
            public void onAddTenAttempts() { temp(); }

            @Override
            public void onAddFiveStarDrop() { temp(); }

            @Override
            public void onAddFourStarDrop() { temp(); }
        });
    }

    void temp() {

    }
}