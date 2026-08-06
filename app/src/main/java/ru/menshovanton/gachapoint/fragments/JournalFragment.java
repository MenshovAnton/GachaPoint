package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.adapters.JournalPagerAdapter;

public class JournalFragment extends Fragment {

    public static JournalFragment journalFragment;

    TabLayout tabLayout;
    ViewPager2 viewPager;

    public JournalFragment() {}

    public static JournalFragment newInstance() {
        return new JournalFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        journalFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.journalViewPager);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        JournalPagerAdapter adapter = new JournalPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.tab_wishes));
                    triggerRefresh("refresh_wishes_counter_key");
                    break;
                case 1:
                    tab.setText(getString(R.string.tab_piggy_bank));
                    triggerRefresh("refresh_piggy_bank_key");
                    break;
            }
        }).attach();
    }

    public void triggerRefresh(String key) {
        Bundle result = new Bundle();
        result.putBoolean("shouldRefresh", true);

        getChildFragmentManager().setFragmentResult(key, result);
    }
}