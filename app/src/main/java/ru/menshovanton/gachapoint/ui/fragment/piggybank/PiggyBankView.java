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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Collections;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.animation.CarouselAnimator;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.animation.TextAnimator;
import ru.menshovanton.gachapoint.ui.main.GamesConfigBottomSheetDialog;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class PiggyBankView extends Fragment {

    private PiggyBankViewModel viewModel;
    private MainActivityView mainActivityView;

    private ImageButton btnBack;
    private RecyclerView rvGameTypes;
    private PillsAdapter pillsAdapter;

    private ProgressBar progressBar;
    private TextView savedWishesCounter;
    private MaterialCardView cardRemainderGems;
    private ImageView ivRemainderGemIcon;
    private TextView tvRemainderGems;
    private TextView tvPercent;
    private ImageView wishIcon;
    private ImageView currencyIcon;
    private TextView tvCurrencyAmount;

    private Button btnEditTarget;
    private ViewGroup mcvGoalCard;
    private ScrollView svPiggyBank;
    private androidx.constraintlayout.widget.Group groupPiggyBankContent;
    private View llPiggyBankEmpty;
    private TextView tvEmptySavingsTitle;
    private TextView tvEmptySavingsDesc;
    private View btnEmptySavingsSetGoal;
    private boolean isFirstLoad = true;

    private MaterialButtonToggleGroup toggleActionMode;
    private MaterialButton btnModePulls;
    private MaterialButton btnModeGems;
    private View llPullsActions;
    private View llGemsActions;

    private Button btnAddOne;
    private Button btnAddTen;
    private Button btnMinusOne;
    private Button btnMinusTen;

    private Button btnAddSixtyGems;
    private Button btnAddOnePullGems;
    private Button btnMinusSixtyGems;
    private Button btnMinusOnePullGems;

    private Button btnCustomInput;

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

        mcvGoalCard = view.findViewById(R.id.mcv_goal_card);
        btnEditTarget = view.findViewById(R.id.btn_edit_target);
        svPiggyBank = view.findViewById(R.id.sv_piggy_bank);
        groupPiggyBankContent = view.findViewById(R.id.group_piggy_bank_content);
        llPiggyBankEmpty = view.findViewById(R.id.ll_piggy_bank_empty);
        tvEmptySavingsTitle = view.findViewById(R.id.tv_empty_savings_title);
        tvEmptySavingsDesc = view.findViewById(R.id.tv_empty_savings_desc);
        btnEmptySavingsSetGoal = view.findViewById(R.id.btn_empty_savings_set_goal);

        toggleActionMode = view.findViewById(R.id.toggle_action_mode);
        btnModePulls = view.findViewById(R.id.btn_mode_pulls);
        btnModeGems = view.findViewById(R.id.btn_mode_gems);
        llPullsActions = view.findViewById(R.id.ll_pulls_actions);
        llGemsActions = view.findViewById(R.id.ll_gems_actions);

        btnAddOne = view.findViewById(R.id.btn_add_one_pull);
        btnAddTen = view.findViewById(R.id.btn_add_ten_pulls);
        btnMinusOne = view.findViewById(R.id.btn_minus_one_pull);
        btnMinusTen = view.findViewById(R.id.btn_minus_ten_pulls);

        btnAddSixtyGems = view.findViewById(R.id.btn_add_sixty_gems);
        btnAddOnePullGems = view.findViewById(R.id.btn_add_one_pull_gems);
        btnMinusSixtyGems = view.findViewById(R.id.btn_minus_sixty_gems);
        btnMinusOnePullGems = view.findViewById(R.id.btn_minus_one_pull_gems);

        btnCustomInput = view.findViewById(R.id.btn_custom_input);

        progressBar = view.findViewById(R.id.pb_saving_progress);
        savedWishesCounter = view.findViewById(R.id.tv_saved_counter);
        cardRemainderGems = view.findViewById(R.id.card_remainder_gems);
        ivRemainderGemIcon = view.findViewById(R.id.iv_remainder_gem_icon);
        tvRemainderGems = view.findViewById(R.id.tv_remainder_gems);
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
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        rvGameTypes.setLayoutManager(layoutManager);

        GameType currentType = (mainActivityView != null && mainActivityView.getSubType() != null)
                ? mainActivityView.getSubType()
                : GameType.GENSHIN;

        List<GameType> enabledGames = mainActivityView != null ? mainActivityView.getEnabledGames() : Collections.singletonList(GameType.GENSHIN);

        viewModel.setGameType(currentType);

        pillsAdapter = new PillsAdapter(
                enabledGames,
                currentType,
                (selectedGame, position) -> {
                    if (mainActivityView != null) {
                        mainActivityView.setSubType(selectedGame);
                    }
                    viewModel.setGameType(selectedGame);
                    updateGameAppearance(selectedGame);
                },
                () -> GamesConfigBottomSheetDialog.show(requireContext(), () -> {
                    if (mainActivityView != null) mainActivityView.refreshGamesConfig();
                })
        );

        CarouselAnimator.setupItemAnimator(rvGameTypes);
        rvGameTypes.setAdapter(pillsAdapter);
        updateGameAppearance(currentType);
        updatePillsVisibility();
    }

    private void updatePillsVisibility() {
        if (rvGameTypes == null || mainActivityView == null) return;
        List<GameType> enabled = mainActivityView.getEnabledGames();
        ViewGroup root = (getView() instanceof ViewGroup) ? (ViewGroup) getView() : null;
        boolean isMultiGame = enabled.size() > 1;

        CarouselAnimator.setCarouselVisibility(rvGameTypes, root, isMultiGame);

        if (!isMultiGame) {
            GameType monoGame = !enabled.isEmpty() ? enabled.get(0) : GameType.GENSHIN;
            if (mainActivityView.getSubType() != monoGame) {
                mainActivityView.setSubType(monoGame);
            }
            if (viewModel != null && viewModel.getCurrentGameType() != monoGame) {
                viewModel.setGameType(monoGame);
                updateGameAppearance(monoGame);
            }
        } else {
            if (pillsAdapter != null) {
                pillsAdapter.updateData(enabled, mainActivityView.getSubType());
            }
        }
    }

    private void setupActionButtons() {
        if (toggleActionMode != null) {
            toggleActionMode.check(R.id.btn_mode_pulls);
            toggleActionMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
                if (!isChecked) return;
                boolean inGems = (checkedId == R.id.btn_mode_gems);
                if (llPullsActions != null) llPullsActions.setVisibility(inGems ? View.GONE : View.VISIBLE);
                if (llGemsActions != null) llGemsActions.setVisibility(inGems ? View.VISIBLE : View.GONE);
            });
        }

        btnAddOne.setOnClickListener(v -> viewModel.addProgress(1));
        btnAddTen.setOnClickListener(v -> viewModel.addProgress(10));
        btnMinusOne.setOnClickListener(v -> viewModel.removeProgress(1));
        btnMinusTen.setOnClickListener(v -> viewModel.removeProgress(10));

        if (btnAddSixtyGems != null) {
            btnAddSixtyGems.setOnClickListener(v -> {
                GameType game = viewModel.getCurrentGameType();
                viewModel.addGems(game != null ? game.getDailyReward() : 60);
            });
        }
        if (btnAddOnePullGems != null) {
            btnAddOnePullGems.setOnClickListener(v -> {
                GameType game = viewModel.getCurrentGameType();
                viewModel.addGems(game != null ? game.getPullCost() : 160);
            });
        }
        if (btnMinusSixtyGems != null) {
            btnMinusSixtyGems.setOnClickListener(v -> {
                GameType game = viewModel.getCurrentGameType();
                viewModel.removeGems(game != null ? game.getDailyReward() : 60);
            });
        }
        if (btnMinusOnePullGems != null) {
            btnMinusOnePullGems.setOnClickListener(v -> {
                GameType game = viewModel.getCurrentGameType();
                viewModel.removeGems(game != null ? game.getPullCost() : 160);
            });
        }

        if (btnCustomInput != null) {
            btnCustomInput.setOnClickListener(v -> showQuickInputDialog());
        }

        btnEditTarget.setOnClickListener(this::showTargetMenu);
        if (btnEmptySavingsSetGoal != null) {
            btnEmptySavingsSetGoal.setOnClickListener(this::showTargetMenu);
        }
    }

    private void showQuickInputDialog() {
        GameType gameType = viewModel.getCurrentGameType();
        if (gameType == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        @SuppressLint("InflateParams")
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_quick_add_gems, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        dialog.setOnShowListener(d -> {
            BottomSheetDialog bsd = (BottomSheetDialog) d;
            FrameLayout bottomSheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

        ImageView ivIcon = dialogView.findViewById(R.id.iv_dialog_icon);
        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_header);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_dialog_subtitle);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_dialog_close);
        MaterialButtonToggleGroup toggleUnit = dialogView.findViewById(R.id.toggle_unit);
        MaterialButton btnUnitPulls = dialogView.findViewById(R.id.btn_unit_pulls);
        MaterialButton btnUnitGems = dialogView.findViewById(R.id.btn_unit_gems);
        MaterialButtonToggleGroup toggleOp = dialogView.findViewById(R.id.toggle_operation);
        TextInputLayout tilAmount = dialogView.findViewById(R.id.til_amount);
        TextInputEditText etAmount = dialogView.findViewById(R.id.et_amount);
        LinearLayout llPresets = dialogView.findViewById(R.id.ll_preset_chips);
        TextView tvPreview = dialogView.findViewById(R.id.tv_preview_calc);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btn_cancel);
        MaterialButton btnApply = dialogView.findViewById(R.id.btn_apply);

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        tvSubtitle.setText(gameType.getName());
        btnUnitPulls.setIconResource(gameType.getPullIconResId());
        btnUnitGems.setIconResource(gameType.getCurrencyIconResId());

        boolean isGemsMode = (toggleActionMode != null && toggleActionMode.getCheckedButtonId() == R.id.btn_mode_gems);
        toggleUnit.check(isGemsMode ? R.id.btn_unit_gems : R.id.btn_unit_pulls);
        toggleOp.check(R.id.btn_op_add);

        final int pullCost = gameType.getPullCost();

        @SuppressLint("SetTextI18n") Runnable updateUi = () -> {
            boolean inGems = (toggleUnit.getCheckedButtonId() == R.id.btn_unit_gems);
            boolean isAdd = (toggleOp.getCheckedButtonId() == R.id.btn_op_add);

            ivIcon.setImageResource(inGems ? gameType.getCurrencyIconResId() : gameType.getPullIconResId());
            tvTitle.setText(inGems ? getString(R.string.dialog_quick_add_title_gems) : getString(R.string.dialog_quick_add_title_pulls));
            tilAmount.setHint(inGems ? getString(R.string.dialog_quick_add_hint_gems) : getString(R.string.dialog_quick_add_hint_pulls));
            btnApply.setText(isAdd ? getString(R.string.action_add) : getString(R.string.action_deduct));

            llPresets.removeAllViews();
            int daily = gameType.getDailyReward();
            int subDaily = gameType.getCurrencyPerDay();
            int[] presets = inGems ? new int[]{daily, subDaily, pullCost, 300, 600, 10 * pullCost} : new int[]{1, 5, 10, 50};
            for (int p : presets) {
                MaterialButton chip = new MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
                chip.setText("+" + p);
                chip.setTextSize(13);
                chip.setCornerRadius((int) (10 * getResources().getDisplayMetrics().density));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (int) (36 * getResources().getDisplayMetrics().density)
                );
                lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
                chip.setLayoutParams(lp);
                chip.setPadding(
                        (int) (12 * getResources().getDisplayMetrics().density), 0,
                        (int) (12 * getResources().getDisplayMetrics().density), 0
                );
                chip.setOnClickListener(v -> {
                    int cur = 0;
                    String text = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
                    if (!TextUtils.isEmpty(text)) {
                        try { cur = Integer.parseInt(text); } catch (NumberFormatException ignored) {}
                    }
                    etAmount.setText(String.valueOf(cur + p));
                    if (etAmount.getText() != null) {
                        etAmount.setSelection(etAmount.getText().length());
                    }
                });
                llPresets.addView(chip);
            }

            int val = 0;
            String text = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(text)) {
                try { val = Integer.parseInt(text); } catch (NumberFormatException ignored) {}
            }
            if (val <= 0) {
                tvPreview.setText(getString(R.string.savings_converter_desc, pullCost));
            } else {
                if (inGems) {
                    int pulls = val / pullCost;
                    int remGems = val % pullCost;
                    if (isAdd) {
                        tvPreview.setText(getString(R.string.dialog_quick_add_preview, pulls, remGems));
                    } else {
                        tvPreview.setText(getString(R.string.dialog_quick_remove_preview, pulls, remGems));
                    }
                } else {
                    int gems = val * pullCost;
                    tvPreview.setText(getString(R.string.savings_currency_approx_format, String.valueOf(gems), getString(gameType.getCurrencyNameResId())));
                }
            }
        };

        toggleUnit.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateUi.run();
        });
        toggleOp.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) updateUi.run();
        });

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { updateUi.run(); }
        });

        updateUi.run();

        btnApply.setOnClickListener(v -> {
            int amount = 0;
            String text = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(text)) {
                try { amount = Integer.parseInt(text); } catch (NumberFormatException ignored) {}
            }
            if (amount <= 0) {
                dialog.dismiss();
                return;
            }

            boolean inGems = (toggleUnit.getCheckedButtonId() == R.id.btn_unit_gems);
            boolean isAdd = (toggleOp.getCheckedButtonId() == R.id.btn_op_add);

            if (inGems) {
                if (isAdd) {
                    viewModel.addGems(amount);
                } else {
                    viewModel.removeGems(amount);
                }
            } else {
                if (isAdd) {
                    viewModel.addProgress(amount);
                } else {
                    viewModel.removeProgress(amount);
                }
            }
            dialog.dismiss();
        });

        dialog.show();
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
        updatePillsVisibility();
        if (viewModel != null) {
            viewModel.refreshData();
            viewModel.syncAndRefresh();
        }
    }

    @SuppressLint("SetTextI18n")
    private void observeViewModels() {
        if (mainActivityView != null) {
            mainActivityView.getMainActivityViewModel().getEnabledGamesLiveData().observe(getViewLifecycleOwner(), games ->
                    updatePillsVisibility());
            mainActivityView.getMainActivityViewModel().getSubTypeLiveData().observe(getViewLifecycleOwner(), game -> {
                if (pillsAdapter != null) {
                    pillsAdapter.setSelectedGame(game);
                }
                if (viewModel != null && viewModel.getCurrentGameType() != game) {
                    viewModel.setGameType(game);
                    updateGameAppearance(game);
                }
            });
        }

        viewModel.getStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;

            int target = state.getTarget();
            int progress = state.getProgress();
            int remainderGems = state.getRemainderGems();
            int totalGems = state.getTotalGems();

            GameType gameType = viewModel.getCurrentGameType();
            boolean wasEmpty = llPiggyBankEmpty != null && llPiggyBankEmpty.getVisibility() == View.VISIBLE;
            boolean wasContentVisible = groupPiggyBankContent != null && groupPiggyBankContent.getVisibility() == View.VISIBLE;
            boolean nowEmpty = target <= 0;

            if (!isFirstLoad && (wasEmpty || wasContentVisible) && wasEmpty != nowEmpty && getView() instanceof ViewGroup) {
                android.transition.TransitionManager.beginDelayedTransition((ViewGroup) getView(),
                        new android.transition.AutoTransition().setDuration(240));
            }
            isFirstLoad = false;

            if (target <= 0) {
                if (llPiggyBankEmpty != null) llPiggyBankEmpty.setVisibility(View.VISIBLE);
                if (groupPiggyBankContent != null) groupPiggyBankContent.setVisibility(View.GONE);
                if (svPiggyBank != null) {
                    svPiggyBank.setNestedScrollingEnabled(false);
                    svPiggyBank.scrollTo(0, 0);
                }

                if (tvEmptySavingsDesc != null && gameType != null) {
                    tvEmptySavingsDesc.setText(gameType.getEmptySavingsGoalDescResId());
                }
                return;
            } else {
                if (llPiggyBankEmpty != null) llPiggyBankEmpty.setVisibility(View.GONE);
                if (groupPiggyBankContent != null) groupPiggyBankContent.setVisibility(View.VISIBLE);
                if (svPiggyBank != null) {
                    svPiggyBank.setNestedScrollingEnabled(true);
                }
            }

            int pullCost = gameType != null ? gameType.getPullCost() : 160;
            int targetGems = target * pullCost;

            progressBar.setMax(targetGems > 0 ? targetGems : target);
            progressBar.setProgress(targetGems > 0 ? totalGems : progress, true);

            NumberCountAnimator.animateFraction(savedWishesCounter, progress, target);

            if (cardRemainderGems != null && tvRemainderGems != null) {
                if (remainderGems > 0) {
                    cardRemainderGems.setVisibility(View.VISIBLE);
                    tvRemainderGems.setText("+" + remainderGems);
                } else {
                    cardRemainderGems.setVisibility(View.GONE);
                }
            }

            int percent = targetGems > 0 ? Math.min(100, (int) Math.round(((double) totalGems / targetGems) * 100)) : 0;
            NumberCountAnimator.animatePercent(tvPercent, percent);

            NumberCountAnimator.animateFormatted(tvCurrencyAmount, totalGems);
        });

        viewModel.getGoalExceededEvent().observe(getViewLifecycleOwner(), unused ->
                Toast.makeText(getContext(), R.string.goal_not_set_or_reached, Toast.LENGTH_SHORT).show()
        );
    }

    @SuppressLint("SetTextI18n")
    private void updateGameAppearance(GameType gameType) {
        if (gameType == null) return;
        if (tvEmptySavingsDesc != null) {
            tvEmptySavingsDesc.setText(gameType.getEmptySavingsGoalDescResId());
        }
        wishIcon.setImageResource(gameType.getPullIconResId());
        currencyIcon.setImageResource(gameType.getCurrencyIconResId());
        if (ivRemainderGemIcon != null) {
            ivRemainderGemIcon.setImageResource(gameType.getCurrencyIconResId());
        }
        if (btnModePulls != null) {
            btnModePulls.setIconResource(gameType.getPullIconResId());
        }
        if (btnModeGems != null) {
            btnModeGems.setIconResource(gameType.getCurrencyIconResId());
        }
        if (btnAddSixtyGems != null) {
            btnAddSixtyGems.setText("+" + gameType.getDailyReward());
        }
        if (btnMinusSixtyGems != null) {
            btnMinusSixtyGems.setText("-" + gameType.getDailyReward());
        }
        if (btnAddOnePullGems != null) {
            btnAddOnePullGems.setText("+" + gameType.getPullCost());
        }
        if (btnMinusOnePullGems != null) {
            btnMinusOnePullGems.setText("-" + gameType.getPullCost());
        }
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
            @Override public void onClearAutoProgress() {
                Context themeContext = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
                new MaterialAlertDialogBuilder(themeContext, R.style.Dialog_GachaPoint_AlertDialog)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.savings_clear_auto_confirm)
                    .setPositiveButton(R.string.reset, (d, w) -> {
                        viewModel.clearAutoProgress();
                        Toast.makeText(requireContext(), R.string.savings_clear_auto_success, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            }
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
        pillsAdapter = null;
        super.onDestroyView();
        if (rvGameTypes != null) {
            rvGameTypes.setAdapter(null);
            rvGameTypes = null;
        }
        mainActivityView = null;
        btnBack = null;
        progressBar = null;
        savedWishesCounter = null;
        cardRemainderGems = null;
        ivRemainderGemIcon = null;
        tvRemainderGems = null;
        tvPercent = null;
        wishIcon = null;
        currencyIcon = null;
        tvCurrencyAmount = null;
        btnEditTarget = null;
        mcvGoalCard = null;
        svPiggyBank = null;
        groupPiggyBankContent = null;
        llPiggyBankEmpty = null;
        tvEmptySavingsTitle = null;
        tvEmptySavingsDesc = null;
        btnEmptySavingsSetGoal = null;
        isFirstLoad = true;
        toggleActionMode = null;
        btnModePulls = null;
        btnModeGems = null;
        llPullsActions = null;
        llGemsActions = null;
        btnAddOne = null;
        btnAddTen = null;
        btnMinusOne = null;
        btnMinusTen = null;
        btnAddSixtyGems = null;
        btnAddOnePullGems = null;
        btnMinusSixtyGems = null;
        btnMinusOnePullGems = null;
        btnCustomInput = null;
        tvConverterRate = null;
        etConvertPulls = null;
        etConvertCurrency = null;
    }
}
