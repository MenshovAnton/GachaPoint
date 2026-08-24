package ru.menshovanton.gachapoint.ui.fragment.journal.piggybank;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.fragment.journal.SharedJournalViewModel;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class PiggyBankView extends Fragment {

    private PiggyBankViewModel viewModel;
    private SharedJournalViewModel sharedViewModel;

    private ProgressBar progressBar;
    private Button addOne;
    private Button addTen;
    private Button editGoal;
    private TextView savedWishesCounter;
    private ImageView wishIcon;

    public PiggyBankView() {}

    public static PiggyBankView newInstance() {
        return new PiggyBankView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PiggyBankViewModel.class);

        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            sharedViewModel = new ViewModelProvider(parentFragment).get(SharedJournalViewModel.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_piggy_bank, container, false);

        addOne = view.findViewById(R.id.btn_add_one_pull);
        addTen = view.findViewById(R.id.btn_add_ten_pulls);
        editGoal = view.findViewById(R.id.btn_edit_target);
        progressBar = view.findViewById(R.id.pb_saving_progress);
        savedWishesCounter = view.findViewById(R.id.tv_saved_counter);
        wishIcon = view.findViewById(R.id.iv_pull);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        addOne.setOnClickListener(v -> viewModel.addProgress(1));
        addTen.setOnClickListener(v -> viewModel.addProgress(10));
        editGoal.setOnClickListener(this::showMoreMenu);

        observeViewModels();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshData();
        }
    }

    private void observeViewModels() {
        viewModel.getStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            progressBar.setMax(state.getTarget());
            progressBar.setProgress(state.getProgress(), false);

            String counterText = state.getProgress() + "/" + state.getTarget();
            savedWishesCounter.setText(counterText);
        });

        viewModel.getGoalExceededEvent().observe(getViewLifecycleOwner(), unused ->
                Toast.makeText(getContext(), R.string.goal_not_set_or_reached, Toast.LENGTH_SHORT).show()
        );

        if (sharedViewModel != null) {
            sharedViewModel.getSelectedGameType().observe(getViewLifecycleOwner(), gameType -> {
                updateGameIcon(gameType);
                viewModel.setGameType(gameType);
            });
        }
    }

    private void updateGameIcon(GameType gameType) {
        if (gameType == null) return;
        switch (gameType) {
            case GENSHIN:
                wishIcon.setImageResource(R.drawable.icon_intertwined_fate);
                break;
            case HSR:
                wishIcon.setImageResource(R.drawable.icon_star_rail_special_pass);
                break;
            case ZZZ:
                wishIcon.setImageResource(R.drawable.icon_encrypted_master_tape);
                break;
        }
    }

    private void showMoreMenu(View view) {
        if (!(getActivity() instanceof MainActivityView)) return;

        ((MainActivityView) getActivity()).showPiggyBankMenu(new MainActivityView.OnPiggyBankMenuClickListener() {
            @Override public void onAddOnePity() { viewModel.addTargetMultiplier(1); }
            @Override public void onAddTwoPity() { viewModel.addTargetMultiplier(2); }
            @Override public void onAddSixPity() { viewModel.addTargetMultiplier(6); }
            @Override public void onReset() { viewModel.resetTarget(); }
        });
    }
}