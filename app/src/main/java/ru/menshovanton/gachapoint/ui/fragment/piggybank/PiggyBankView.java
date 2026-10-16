package ru.menshovanton.gachapoint.ui.fragment.piggybank;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.animation.TextAnimator;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class PiggyBankView extends Fragment {

    private PiggyBankViewModel viewModel;
    private MainActivityView mainActivityView;

    private ImageButton btnBack;
    private RecyclerView rvGameTypes;

    private ProgressBar progressBar;
    private TextView savedWishesCounter;
    private TextView tvPercent;
    private ImageView wishIcon;
    private ImageView currencyIcon;
    private TextView tvCurrencyAmount;

    private Button btnEditTarget;
    private Button btnAddOne;
    private Button btnAddTen;
    private Button btnMinusOne;
    private Button btnMinusTen;

    private TextView tvConverterRate;
    private EditText etConvertPulls;
    private EditText etConvertCurrency;
    private boolean isUpdatingConverter = false;

    public PiggyBankView() {}

    public static PiggyBankView newInstance() {
        return new PiggyBankView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();
        viewModel = new ViewModelProvider(this).get(PiggyBankViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainActivityView = (MainActivityView) getActivity();
        View view = inflater.inflate(R.layout.fragment_piggy_bank, container, false);

        btnBack = view.findViewById(R.id.btn_back);
        rvGameTypes = view.findViewById(R.id.rv_game_types);

        btnEditTarget = view.findViewById(R.id.btn_edit_target);
        btnAddOne = view.findViewById(R.id.btn_add_one_pull);
        btnAddTen = view.findViewById(R.id.btn_add_ten_pulls);
        btnMinusOne = view.findViewById(R.id.btn_minus_one_pull);
        btnMinusTen = view.findViewById(R.id.btn_minus_ten_pulls);

        progressBar = view.findViewById(R.id.pb_saving_progress);
        savedWishesCounter = view.findViewById(R.id.tv_saved_counter);
        tvPercent = view.findViewById(R.id.tv_percent);
        wishIcon = view.findViewById(R.id.iv_pull);
        currencyIcon = view.findViewById(R.id.iv_currency_icon);
        tvCurrencyAmount = view.findViewById(R.id.tv_currency_amount);

        tvConverterRate = view.findViewById(R.id.tv_converter_rate);
        etConvertPulls = view.findViewById(R.id.et_convert_pulls);
        etConvertCurrency = view.findViewById(R.id.et_convert_currency);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());

        setupGameTypesCarousel();
        setupActionButtons();
        setupConverter();
        observeViewModels();
    }

    private void setupGameTypesCarousel() {
        List<String> categories = new ArrayList<>();
        for (GameType type : GameType.values()) {
            categories.add(getString(type.getNameResId()));
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvGameTypes.setLayoutManager(layoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : GameType.GENSHIN;

        viewModel.setGameType(currentType);

        PillsAdapter pillsAdapter = new PillsAdapter(categories, currentType.getCode(), (item, position) -> {
            GameType selectedGame = GameType.fromCode(position);
            if (mainActivityView != null) {
                mainActivityView.setSubType(selectedGame);
            }
            viewModel.setGameType(selectedGame);
            updateGameAppearance(selectedGame);
        });

        rvGameTypes.setAdapter(pillsAdapter);
        updateGameAppearance(currentType);
    }

    private void setupActionButtons() {
        btnAddOne.setOnClickListener(v -> viewModel.addProgress(1));
        btnAddTen.setOnClickListener(v -> viewModel.addProgress(10));
        btnMinusOne.setOnClickListener(v -> viewModel.removeProgress(1));
        btnMinusTen.setOnClickListener(v -> viewModel.removeProgress(10));
        btnEditTarget.setOnClickListener(this::showTargetMenu);
    }

    private void setupConverter() {
        etConvertPulls.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingConverter) return;
                isUpdatingConverter = true;
                if (!TextUtils.isEmpty(s)) {
                    try {
                        int pulls = Integer.parseInt(s.toString());
                        int cost = viewModel.getCurrentGameType().getPullCost();
                        etConvertCurrency.setText(String.valueOf(pulls * cost));
                    } catch (NumberFormatException ignored) {}
                } else {
                    etConvertCurrency.setText("");
                }
                isUpdatingConverter = false;
            }
        });

        etConvertCurrency.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdatingConverter) return;
                isUpdatingConverter = true;
                if (!TextUtils.isEmpty(s)) {
                    try {
                        int currency = Integer.parseInt(s.toString());
                        int cost = viewModel.getCurrentGameType().getPullCost();
                        etConvertPulls.setText(String.valueOf(currency / cost));
                    } catch (NumberFormatException ignored) {}
                } else {
                    etConvertPulls.setText("");
                }
                isUpdatingConverter = false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.refreshData();
            viewModel.syncAndRefresh();
        }
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModels() {
        viewModel.getStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            int target = state.getTarget();
            int progress = state.getProgress();

            progressBar.setMax(Math.max(1, target));
            progressBar.setProgress(progress, true);

            NumberCountAnimator.animateFraction(savedWishesCounter, progress, target);

            int percent = target > 0 ? Math.min(100, (int) ((progress / (float) target) * 100)) : 0;
            NumberCountAnimator.animatePercent(tvPercent, percent);

            GameType gameType = viewModel.getCurrentGameType();
            int currencyTotal = progress * gameType.getPullCost();
            NumberCountAnimator.animateFormatted(tvCurrencyAmount, currencyTotal);
        });

        viewModel.getGoalExceededEvent().observe(getViewLifecycleOwner(), unused ->
                Toast.makeText(getContext(), R.string.goal_not_set_or_reached, Toast.LENGTH_SHORT).show()
        );
    }

    @SuppressLint("SetTextI18n")
    private void updateGameAppearance(GameType gameType) {
        if (gameType == null) return;
        wishIcon.setImageResource(gameType.getPullIconResId());
        currencyIcon.setImageResource(gameType.getCurrencyIconResId());
        TextAnimator.crossfade(tvConverterRate, getString(R.string.savings_converter_desc, gameType.getPullCost()));

        isUpdatingConverter = true;
        etConvertPulls.setText("10");
        etConvertCurrency.setText(String.valueOf(10 * gameType.getPullCost()));
        isUpdatingConverter = false;
    }

    private void showTargetMenu(View view) {
        if (!(getActivity() instanceof MainActivityView)) return;

        MainActivityView activity = (MainActivityView) getActivity();
        activity.showPiggyBankMenu(new MainActivityView.OnPiggyBankMenuClickListener() {
            @Override public void onAddOnePity() { viewModel.addTargetMultiplier(1); }
            @Override public void onAddTwoPity() { viewModel.addTargetMultiplier(2); }
            @Override public void onAddSixPity() { viewModel.addTargetMultiplier(6); }
            @Override public void onReset() {
                Context themeContext = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
                new MaterialAlertDialogBuilder(themeContext, R.style.Dialog_GachaPoint_AlertDialog)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.savings_reset_confirm)
                        .setPositiveButton(R.string.reset, (d, w) -> viewModel.resetTarget())
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rvGameTypes != null) {
            rvGameTypes.setAdapter(null);
            rvGameTypes = null;
        }
        mainActivityView = null;
        btnBack = null;
        progressBar = null;
        savedWishesCounter = null;
        tvPercent = null;
        wishIcon = null;
        currencyIcon = null;
        tvCurrencyAmount = null;
        btnEditTarget = null;
        btnAddOne = null;
        btnAddTen = null;
        btnMinusOne = null;
        btnMinusTen = null;
        tvConverterRate = null;
        etConvertPulls = null;
        etConvertCurrency = null;
    }
}