package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.Objects;

import ru.menshovanton.gachapoint.helpers.CalendarHelper;
import ru.menshovanton.gachapoint.helpers.PiggyBankHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class PiggyBankFragment extends Fragment {

    private PiggyBankHelper piggyBankHelper;

    private MainActivity mainActivity;

    private ProgressBar progressBar;
    private Button addOne;
    private Button addTen;
    private Button editGoal;
    private TextView savedWishesCounter;

    private int progress;
    private int target;

    private final int PITY = 77;

    public PiggyBankFragment() {}

    public static PiggyBankFragment newInstance(String param1, String param2) {
        return new PiggyBankFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        PreferencesHelper preferencesHelper = new PreferencesHelper(Objects.requireNonNull(mainActivity));
        piggyBankHelper = new PiggyBankHelper(mainActivity);
        CalendarHelper calendarHelper = new CalendarHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_piggy_bank, container, false);

        addOne = view.findViewById(R.id.addOneSavedWish);
        addTen = view.findViewById(R.id.addTenSavedWishes);
        editGoal = view.findViewById(R.id.editTargetButton);

        progressBar = view.findViewById(R.id.savingProgress);

        savedWishesCounter = view.findViewById(R.id.savedWishesCounter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addOne.setOnClickListener(this::onAddOneSavedWish);
        addTen.setOnClickListener(this::onAddTenSavedWishes);
        editGoal.setOnClickListener(this::showMoreMenu);

        getParentFragmentManager().setFragmentResultListener("refresh_piggy_bank_key", getViewLifecycleOwner(), (requestKey, result) -> {
            boolean shouldRefresh = result.getBoolean("shouldRefresh", false);
            if (shouldRefresh) {
                refresh(view);
            }
        });

        target = piggyBankHelper.getTarget();
        progress = piggyBankHelper.getProgress();

        progressBar.setProgress(progress);
        progressBar.setMax(target);

        refresh(view);
    }

    @Override
    public void onResume() {
        super.onResume();

        target = piggyBankHelper.getTarget();
        progress = piggyBankHelper.getProgress();

        progressBar.setProgress(progress);
        progressBar.setMax(target);

        refresh(requireView());
    }

    private void refresh(View view) {
        ImageView wishIcon = view.findViewById(R.id.wishIconPiggyBank);

        switch (mainActivity.getSubType()) {
            case GENSHIN:
                wishIcon.setImageResource(R.drawable.icon_intertwined_fate);
                break;
            case HSR:
                wishIcon.setImageResource(R.drawable.icon_star_rail_special_pass);
                break;
            case ZZZ:
                wishIcon.setImageResource(R.drawable.icon_encrypted_master_tape);
                break;
        }

        updateProgress();
    }

    private void showMoreMenu(View view) {
        ((MainActivity) requireActivity()).showPiggyBankMenu(new MainActivity.OnPiggyBankMenuClickListener() {
            @Override public void onAddOnePity() { addTargetOne(); }
            @Override public void onAddTwoPity() { addTargetTwo(); }
            @Override public void onAddSixPity() { addTargetSix(); }
            @Override public void onReset() { resetTarget(); }
        });
    }

    private void onAddOneSavedWish(View view) {
        progressAdd(1);
    }

    private void onAddTenSavedWishes(View view) {
        progressAdd(10);
    }

    private void progressAdd(int i) {
        if (piggyBankHelper.pushManualProgress(i)) {
            updateProgress();
        } else {
            Toast.makeText(getContext(), "Цель не установлена или достигнута!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress() {
        target = piggyBankHelper.getTarget();
        progress = piggyBankHelper.getProgress();

        progressBar.setMax(target);
        progressBar.setProgress(progress);
        String text = progress + "/" + target;
        savedWishesCounter.setText(text);
    }

    public void addTargetOne() {
        piggyBankHelper.pushTarget(PITY);
        updateProgress();
    }

    public void addTargetTwo() {
        piggyBankHelper.pushTarget(PITY *2);
        updateProgress();
    }

    public void addTargetSix() {
        piggyBankHelper.pushTarget(PITY *6);
        updateProgress();
    }

    public void resetTarget() {
        piggyBankHelper.reset();
        updateProgress();
    }
}