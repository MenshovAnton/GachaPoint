package ru.menshovanton.gachapoint.fragments;

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
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class WishesCounterFragment extends Fragment {
    JournalFragment journalFragment;

    Button addOne;
    Button addTen;
    TextView savedWishesCounter;

    MaterialButton genshinImpact;
    MaterialButton honkaiStarRail;
    MaterialButton zenlessZoneZero;

    HorizontalScrollView scrollView;

    public int progress;

    public WishesCounterFragment() {}
    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        journalFragment = JournalFragment.journalFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishes_counter, container, false);

        addOne = view.findViewById(R.id.addOneWish);
        addTen = view.findViewById(R.id.addTenWishes);

        savedWishesCounter = view.findViewById(R.id.wishesCounter);

        genshinImpact = view.findViewById(R.id.genshinImpact);
        honkaiStarRail = view.findViewById(R.id.honkai);
        zenlessZoneZero = view.findViewById(R.id.zenless);

        scrollView = view.findViewById(R.id.gameTypeChangerWishesCounter);

        scrollView.post(() -> scrollView.scrollTo(MainActivity.subTypeScrollX, 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scrollView.setOnScrollChangeListener(this::onScrollChange);

        genshinImpact.setOnClickListener(this::onGenshinClick);
        honkaiStarRail.setOnClickListener(this::onHonkaiClick);
        zenlessZoneZero.setOnClickListener(this::onZenlessClick);

        getParentFragmentManager().setFragmentResultListener("refresh_wishes_counter_key", getViewLifecycleOwner(), (requestKey, result) -> {
            boolean shouldRefresh = result.getBoolean("shouldRefresh", false);
            if (shouldRefresh) {
                refreshData(view);
            }
        });

        refreshData(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        scrollView.post(() -> scrollView.scrollTo(MainActivity.subTypeScrollX, 0));
        refreshData(requireView());
    }

    private void refreshData(View view) {
        ImageView wishIcon = view.findViewById(R.id.wishIconWishCounter);

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

        updateProgress();
    }

    private void updateProgress() {
        savedWishesCounter.setText("0");
    }

    private void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        MainActivity.subTypeScrollX = scrollX;
    }

    public void onGenshinClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(genshinImpact);
        journalFragment.triggerRefresh("refresh_wishes_counter_key");
    }

    public void onHonkaiClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(honkaiStarRail);
        journalFragment.triggerRefresh("refresh_wishes_counter_key");
    }

    public void onZenlessClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(zenlessZoneZero);
        journalFragment.triggerRefresh("refresh_wishes_counter_key");
    }

    private void changeCheckedTab(MaterialButton view) {
        genshinImpact.setStrokeColorResource(R.color.accent);
        honkaiStarRail.setStrokeColorResource(R.color.accent);
        zenlessZoneZero.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }
}