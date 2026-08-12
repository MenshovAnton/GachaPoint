package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.adapters.JournalPagerAdapter;
import ru.menshovanton.gachapoint.adapters.PillsAdapter;
import ru.menshovanton.gachapoint.enums.GameType;

public class JournalFragment extends Fragment {

    private MainActivity mainActivity;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    public JournalFragment() {}

    public static JournalFragment newInstance() {
        return new JournalFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JournalFragment journalFragment = this;
        mainActivity = (MainActivity) getActivity();
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

        RecyclerView gameTypeChanger = view.findViewById(R.id.gameTypeChangerJournal);

        List<String> categories = Arrays.asList(
                getString(R.string.genshin),
                getString(R.string.hsr),
                getString(R.string.zzz)
        );

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        pillsAdapter = new PillsAdapter(categories, (item, position) -> {
            mainActivity.setSubType(GameType.fromCode(position));

            switch (tabLayout.getSelectedTabPosition()) {
                case 0:
                    triggerRefresh("refresh_wishes_counter_key");
                    break;
                case 1:
                    triggerRefresh("refresh_piggy_bank_key");
                    break;
            }
        });
        selectAndScrollIfNeeded(mainActivity.getSubType().getCode());

        gameTypeChanger.setAdapter(pillsAdapter);
    }

    private void selectAndScrollIfNeeded(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;

        pillsAdapter.setSelectedPosition(targetPosition);

        int firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastCompletelyVisible = layoutManager.findLastCompletelyVisibleItemPosition();

        boolean isNotFullyVisible = targetPosition < firstCompletelyVisible || targetPosition > lastCompletelyVisible;

        if (isNotFullyVisible) {
            int offsetPx = (int) (16 * getResources().getDisplayMetrics().density);
            layoutManager.scrollToPositionWithOffset(targetPosition, offsetPx);
        }
    }

    public void triggerRefresh(String key) {
        Bundle result = new Bundle();
        result.putBoolean("shouldRefresh", true);

        getChildFragmentManager().setFragmentResult(key, result);
    }
}