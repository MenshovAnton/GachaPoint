package ru.menshovanton.gachapoint.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.menshovanton.gachapoint.R;

public class WishesCounterFragment extends Fragment {


    public WishesCounterFragment() {}
    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wishes_counter, container, false);
    }
}