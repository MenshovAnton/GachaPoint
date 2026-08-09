package ru.menshovanton.gachapoint.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import ru.menshovanton.gachapoint.fragments.PiggyBankFragment;
import ru.menshovanton.gachapoint.fragments.WishesCounterFragment;

public class JournalPagerAdapter extends FragmentStateAdapter {
    public JournalPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new WishesCounterFragment();
            case 1:
                return new PiggyBankFragment();
            default:
                throw new IllegalArgumentException("Invalid position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
