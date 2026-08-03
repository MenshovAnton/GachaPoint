package ru.menshovanton.gachapoint.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.material.button.MaterialButton;

import ru.menshovanton.gachapoint.helpers.PiggyBankHelper;
import ru.menshovanton.gachapoint.helpers.PreferencesHelper;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class PiggyBankFragment extends Fragment {

    PreferencesHelper preferencesHelper;
    PiggyBankHelper piggyBankHelper;
    @SuppressLint("StaticFieldLeak")
    public static PiggyBankFragment instance;
    MainActivity mainActivity;
    JournalFragment journalFragment;

    ProgressBar progressBar;
    Button addOne;
    Button addTen;
    Button editGoal;
    TextView savedWishesCounter;

    MaterialButton genshinImpact;
    MaterialButton honkaiStarRail;
    MaterialButton zenlessZoneZero;

    HorizontalScrollView scrollView;

    public int progress;
    public int target;

    final int GARANT = 77;

    public PiggyBankFragment() {}

    public static PiggyBankFragment newInstance(String param1, String param2) {
        return new PiggyBankFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferencesHelper = new PreferencesHelper(MainActivity.mainActivity);
        mainActivity = MainActivity.mainActivity;
        journalFragment = JournalFragment.journalFragment;
        piggyBankHelper = new PiggyBankHelper();
        instance = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_piggy_bank, container, false);

        addOne = view.findViewById(R.id.addOneSavedWish);
        addTen = view.findViewById(R.id.addTenSavedWishes);
        editGoal = view.findViewById(R.id.editGoalButton);

        progressBar = view.findViewById(R.id.goalProgress);

        savedWishesCounter = view.findViewById(R.id.savedWishesCounter);

        genshinImpact = view.findViewById(R.id.genshinImpact);
        honkaiStarRail = view.findViewById(R.id.honkai);
        zenlessZoneZero = view.findViewById(R.id.zenless);

        scrollView = view.findViewById(R.id.gameTypeChangerPiggyBank);

        scrollView.post(() -> scrollView.scrollTo(MainActivity.subTypeScrollX, 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addOne.setOnClickListener(this::onAddOneSavedWish);
        addTen.setOnClickListener(this::onAddTenSavedWishes);
        editGoal.setOnClickListener(this::showMoreMenu);

        genshinImpact.setOnClickListener(this::onGenshinClick);
        honkaiStarRail.setOnClickListener(this::onHonkaiClick);
        zenlessZoneZero.setOnClickListener(this::onZenlessClick);

        scrollView.setOnScrollChangeListener(this::onScrollChange);

        ImageView wishIcon = view.findViewById(R.id.wishIconPiggyBank);

        switch (MainActivity.subType) {
            case 0:
                wishIcon.setImageResource(R.drawable.intertwined_fate);
                changeCheckedTab(genshinImpact);
                break;
            case 1:
                wishIcon.setImageResource(R.drawable.star_rail_special_pass);
                changeCheckedTab(honkaiStarRail);
                break;
            case 2:
                wishIcon.setImageResource(R.drawable.encrypted_master_tape);
                changeCheckedTab(zenlessZoneZero);
                break;
        }

        target = piggyBankHelper.getTarget();
        progress = piggyBankHelper.getProgress();

        progressBar.setProgress(progress);
        progressBar.setMax(target);

        updateProgress();
    }

    private void showMoreMenu(View view) {
        mainActivity.showPiggyBankMenu();
    }

    private void onAddOneSavedWish(View view) {
        progressAdd(1);
    }

    private void onAddTenSavedWishes(View view) {
        progressAdd(10);
    }

    private void progressAdd(int i) {
        if (piggyBankHelper.pushProgress(i)) {
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

    public void onGenshinClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(genshinImpact);
        journalFragment.replaceFragment(new PiggyBankFragment());
    }

    public void onHonkaiClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(honkaiStarRail);
        journalFragment.replaceFragment(new PiggyBankFragment());
    }

    public void onZenlessClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(zenlessZoneZero);
        journalFragment.replaceFragment(new PiggyBankFragment());
    }

    private void changeCheckedTab(MaterialButton view) {
        genshinImpact.setStrokeColorResource(R.color.accent);
        honkaiStarRail.setStrokeColorResource(R.color.accent);
        zenlessZoneZero.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }

    private void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        MainActivity.subTypeScrollX = scrollX;
    }

    private void addTarget() {
        updateProgress();
        mainActivity.closePiggyBankMenu();
    }

    public void addTargetOne(View view) {
        piggyBankHelper.pushTarget(GARANT);
        addTarget();
    }

    public void addTargetTwo(View view) {
        piggyBankHelper.pushTarget(GARANT*2);
        addTarget();
    }

    public void addTargetSix(View view) {
        piggyBankHelper.pushTarget(GARANT*6);
        addTarget();
    }

    public void resetTarget(View view) {
        piggyBankHelper.reset();
        updateProgress();
        mainActivity.closePiggyBankMenu();
    }
}