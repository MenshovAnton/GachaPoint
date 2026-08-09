package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;

public class WishesCounterFragment extends Fragment {
    private MainActivity mainActivity;

    private TextView savedWishesCounter;

    public WishesCounterFragment() {}
    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishes_counter, container, false);

        Button addOne = view.findViewById(R.id.addOneWish);
        Button addTen = view.findViewById(R.id.addTenWishes);

        savedWishesCounter = view.findViewById(R.id.wishesCounter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        refreshData(requireView());
    }

    private void refreshData(View view) {
        ImageView wishIcon = view.findViewById(R.id.wishIconWishCounter);

        switch (mainActivity.getSubType()) {
            case 0:
                wishIcon.setImageResource(R.drawable.intertwined_fate);
                break;
            case 1:
                wishIcon.setImageResource(R.drawable.star_rail_special_pass);
                break;
            case 2:
                wishIcon.setImageResource(R.drawable.encrypted_master_tape);
                break;
        }

        updateProgress();
    }

    private void updateProgress() {
        savedWishesCounter.setText("0");
    }
}