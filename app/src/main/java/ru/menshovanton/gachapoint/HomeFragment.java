package ru.menshovanton.gachapoint;

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

public class HomeFragment extends Fragment {
    MainActivity mainActivity;

    MaterialButton genshinImpact;
    MaterialButton honkaiStarRail;
    MaterialButton zenlessZoneZero;

    ImageView gemIconHome;
    ImageView wishIconHome;

    HorizontalScrollView scrollView;

    Preferences settings;

    public HomeFragment() {}
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = MainActivity.mainActivity;
        settings = new Preferences(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        genshinImpact = view.findViewById(R.id.genshinImpact);
        honkaiStarRail = view.findViewById(R.id.honkai);
        zenlessZoneZero = view.findViewById(R.id.zenless);
        gemIconHome = view.findViewById(R.id.gemIconHome);
        wishIconHome = view.findViewById(R.id.wishIconHome);
        scrollView = view.findViewById(R.id.gameTypeChangerHome);

        scrollView.post(() -> scrollView.scrollTo(MainActivity.subTypeScrollX, 0));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        genshinImpact.setOnClickListener(this::onGenshinClick);
        honkaiStarRail.setOnClickListener(this::onHonkaiClick);
        zenlessZoneZero.setOnClickListener(this::onZenlessClick);

        scrollView.setOnScrollChangeListener(this::onScrollChange);

        switch (MainActivity.subType) {
            case 0:
                gemIconHome.setImageResource(R.drawable.primogem);
                wishIconHome.setImageResource(R.drawable.intertwined_fate);
                changeCheckedTab(genshinImpact);
                break;
            case 1:
                gemIconHome.setImageResource(R.drawable.stellar_jade);
                wishIconHome.setImageResource(R.drawable.star_rail_special_pass);
                changeCheckedTab(honkaiStarRail);
                break;
            case 2:
                gemIconHome.setImageResource(R.drawable.polychrome);
                wishIconHome.setImageResource(R.drawable.encrypted_master_tape);
                changeCheckedTab(zenlessZoneZero);
                break;
        }
    }

    public void onGenshinClick(View view) {
        MainActivity.subType = 0;
        changeCheckedTab(genshinImpact);
        mainActivity.updateFragment(HomeFragment.newInstance());
    }

    public void onHonkaiClick(View view) {
        MainActivity.subType = 1;
        changeCheckedTab(honkaiStarRail);
        mainActivity.updateFragment(HomeFragment.newInstance());
    }

    public void onZenlessClick(View view) {
        MainActivity.subType = 2;
        changeCheckedTab(zenlessZoneZero);
        mainActivity.updateFragment(HomeFragment.newInstance());
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
}