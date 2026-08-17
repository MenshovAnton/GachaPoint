package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Arrays;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class JournalView extends Fragment {

    private SharedJournalViewModel sharedViewModel;
    private MainActivityView mainActivityView;

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private PillsAdapter pillsAdapter;
    private LinearLayoutManager layoutManager;

    public JournalView() {}

    public static JournalView newInstance() {
        return new JournalView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
        sharedViewModel = new ViewModelProvider(this).get(SharedJournalViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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
            if (position == 0) {
                tab.setText(getString(R.string.tab_wishes));
            } else {
                tab.setText(getString(R.string.tab_piggy_bank));
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
            GameType selectedGame = GameType.fromCode(position);
            mainActivityView.setSubType(selectedGame);

            sharedViewModel.selectGameType(selectedGame);
        });

        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            selectAndScrollIfNeeded(mainActivityView.getSubType().getCode());
            sharedViewModel.selectGameType(mainActivityView.getSubType());
        }

        gameTypeChanger.setAdapter(pillsAdapter);
    }

    private void selectAndScrollIfNeeded(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;
        pillsAdapter.setSelectedPosition(targetPosition);

        int firstCompletelyVisible = layoutManager.findFirstCompletelyVisibleItemPosition();
        int lastCompletelyVisible = layoutManager.findLastCompletelyVisibleItemPosition();

        if (targetPosition < firstCompletelyVisible || targetPosition > lastCompletelyVisible) {
            int offsetPx = (int) (16 * getResources().getDisplayMetrics().density);
            layoutManager.scrollToPositionWithOffset(targetPosition, offsetPx);
        }
    }
}