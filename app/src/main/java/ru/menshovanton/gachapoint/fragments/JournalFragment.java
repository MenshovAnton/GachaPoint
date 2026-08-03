package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class JournalFragment extends Fragment {

    public static JournalFragment journalFragment;

    MaterialButton wishesButton;
    MaterialButton piggyBankButton;

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

        if (savedInstanceState == null) {
            replaceFragment(new WishesCounterFragment());
        }

        wishesButton = view.findViewById(R.id.wishesButton);
        piggyBankButton = view.findViewById(R.id.piggyBankButton);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        wishesButton.setOnClickListener(this::onWishesClick);
        piggyBankButton.setOnClickListener(this::onPiggyBankClick);

        changeCheckedTab(wishesButton);
    }

    public void replaceFragment(Fragment fragment) {
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.journalFrame, fragment)
                .addToBackStack(null)
                .commit();
    }

    public void onWishesClick(View view) {
        changeCheckedTab(wishesButton);
        replaceFragment(new WishesCounterFragment());
    }

    public void onPiggyBankClick(View view) {
        changeCheckedTab(piggyBankButton);
        replaceFragment(new PiggyBankFragment());
    }

    private void changeCheckedTab(MaterialButton view) {
        wishesButton.setStrokeColorResource(R.color.accent);
        piggyBankButton.setStrokeColorResource(R.color.accent);

        view.setStrokeColorResource(R.color.check);
    }
}