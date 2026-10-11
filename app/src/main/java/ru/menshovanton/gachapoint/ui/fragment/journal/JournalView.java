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
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
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
    public void onResume() {
        super.onResume();
        if (mainActivityView != null && mainActivityView.getSubType() != null) {
            GameType current = mainActivityView.getSubType();
            if (pillsAdapter != null && pillsAdapter.getSelectedPosition() != current.getCode()) {
                pillsAdapter.setSelectedPosition(current.getCode());
                centerPositionInstantly(current.getCode());
            }
            if (sharedViewModel != null) {
                sharedViewModel.selectGameType(current);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journal, container, false);
        tabLayout = view.findViewById(R.id.tl_journal_tabs);
        viewPager = view.findViewById(R.id.vp2_journal);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        JournalPagerAdapter adapter = new JournalPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText(getString(R.string.tab_pulls));
            } else {
                tab.setText(getString(R.string.tab_piggy_bank));
            }
        }).attach();

        RecyclerView gameTypeChanger = view.findViewById(R.id.rv_game_types);
        List<String> categories = new ArrayList<>();
        for (GameType type : GameType.values()) {
            categories.add(getString(type.getNameResId()));
        }

        layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        gameTypeChanger.setLayoutManager(layoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : GameType.GENSHIN;

        pillsAdapter = new PillsAdapter(categories, currentType.getCode(), (item, position) -> {
            GameType selectedGame = GameType.fromCode(position);
            if (mainActivityView != null) mainActivityView.setSubType(selectedGame);
            if (sharedViewModel != null) sharedViewModel.selectGameType(selectedGame);
            smoothScrollToCenter(position);
        });

        gameTypeChanger.setAdapter(pillsAdapter);
        centerPositionInstantly(currentType.getCode());

        if (sharedViewModel != null) {
            sharedViewModel.selectGameType(currentType);
        }
    }

    private void centerPositionInstantly(int position) {
        if (layoutManager == null) return;
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int estimatedPillWidth = (int) (130 * getResources().getDisplayMetrics().density);
        int centerOffsetPx = Math.max(0, (screenWidth - estimatedPillWidth) / 2);
        layoutManager.scrollToPositionWithOffset(position, centerOffsetPx);
    }

    private void smoothScrollToCenter(int targetPosition) {
        if (pillsAdapter == null || layoutManager == null) return;
        pillsAdapter.setSelectedPosition(targetPosition);

        RecyclerView recyclerView = getView() != null ? getView().findViewById(R.id.rv_game_types) : null;
        if (recyclerView == null || getContext() == null) return;

        View targetChild = layoutManager.findViewByPosition(targetPosition);
        if (targetChild != null) {
            int childCenter = (targetChild.getLeft() + targetChild.getRight()) / 2;
            int rvCenter = (recyclerView.getPaddingStart() + (recyclerView.getWidth() - recyclerView.getPaddingEnd())) / 2;
            int dx = childCenter - rvCenter;

            if (Math.abs(dx) <= 3) {
                return;
            }
            if (dx < 0 && !recyclerView.canScrollHorizontally(-1)) {
                return;
            }
            if (dx > 0 && !recyclerView.canScrollHorizontally(1)) {
                return;
            }

            recyclerView.smoothScrollBy(dx, 0);
            return;
        }

        LinearSmoothScroller smoothScroller = new LinearSmoothScroller(requireContext()) {
            @Override
            public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
                return (boxStart + (boxEnd - boxStart) / 2) - (viewStart + (viewEnd - viewStart) / 2);
            }
        };
        smoothScroller.setTargetPosition(targetPosition);
        layoutManager.startSmoothScroll(smoothScroller);
    }
}