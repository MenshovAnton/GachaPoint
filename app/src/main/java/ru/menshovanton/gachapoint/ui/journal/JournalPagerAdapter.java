package ru.menshovanton.gachapoint.ui.journal;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ru.menshovanton.gachapoint.ui.journal.piggybank.PiggyBankView;
import ru.menshovanton.gachapoint.ui.journal.wishescounter.WishesCounterView;

public class JournalPagerAdapter extends FragmentStateAdapter {
    public JournalPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new WishesCounterView();
            case 1:
                return new PiggyBankView();
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
