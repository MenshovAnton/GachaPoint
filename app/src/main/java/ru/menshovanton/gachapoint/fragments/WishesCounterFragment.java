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
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.adapters.WishAdapter;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.helpers.DateHelper;
import ru.menshovanton.gachapoint.models.Wish;

public class WishesCounterFragment extends Fragment {
    private MainActivity mainActivity;

    private TextView savedWishesCounter;

    private Button addActions;

    private DatabaseHelper databaseHelper;
    private DateHelper dateHelper;

    private RecyclerView recyclerView;

    public WishesCounterFragment() {}
    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        assert mainActivity != null;
        databaseHelper = new DatabaseHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishes_counter, container, false);

        addActions = view.findViewById(R.id.addActionsButton);

        savedWishesCounter = view.findViewById(R.id.wishesCounter);

        recyclerView = view.findViewById(R.id.wishesLog);
        View emptyView = view.findViewById(R.id.emptyStateView);

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

        List<Wish> wishes;
        try (DatabaseHelper dbHelper = new DatabaseHelper(mainActivity)) {
            wishes = dbHelper.getAllWishes(mainActivity.getSubType());
        }

        View emptyView = view.findViewById(R.id.emptyStateView);

        WishAdapter adapter = new WishAdapter(wishes);

        if (wishes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(adapter);
        }
    }

    private void updateProgress() {
        savedWishesCounter.setText("0");
    }

    private void showMoreMenu(View view) {
        ((MainActivity) requireActivity()).showCounterMenu(new MainActivity.OnCounterMenuClickListener() {
            @Override
            public void onAddOneAttempt() { addOneAttempt(); }

            @Override
            public void onAddTenAttempts() { addTenAttempts(); }

            @Override
            public void onAddFiveStarDrop() { addFiveStar(); }

            @Override
            public void onAddFourStarDrop() { addFourStar(); }
        });
    }

    private void addOneAttempt() {
        databaseHelper.addWish(LocalDate.now().toString(), getString(R.string.default_wish_content), mainActivity.getSubType());
        assert getView() != null;
        refreshData(getView());
    }

    private void addTenAttempts() {
        databaseHelper.addMultipleWishes(LocalDate.now().toString(), getString(R.string.default_wish_content), 9, mainActivity.getSubType());
        databaseHelper.addWish(LocalDate.now().toString(), getString(R.string.four_star), mainActivity.getSubType());
        assert getView() != null;
        refreshData(getView());
    }

    private void addFiveStar() {
        databaseHelper.addWish(LocalDate.now().toString(), getString(R.string.five_star), mainActivity.getSubType());
        assert getView() != null;
        refreshData(getView());
    }

    private void addFourStar() {
        databaseHelper.addWish(LocalDate.now().toString(), getString(R.string.four_star), mainActivity.getSubType());
        assert getView() != null;
        refreshData(getView());
    }
}