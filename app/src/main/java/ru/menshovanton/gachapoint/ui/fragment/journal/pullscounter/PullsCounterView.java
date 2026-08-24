package ru.menshovanton.gachapoint.ui.fragment.journal.pullscounter;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.TextInputEditText;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.BannerType;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.Pull;
import ru.menshovanton.gachapoint.ui.fragment.journal.SharedJournalViewModel;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;

public class PullsCounterView extends Fragment {

    private PullsCounterViewModel viewModel;
    private SharedJournalViewModel sharedViewModel;
    private MainActivityView mainActivityView;

    private TextView savedWishesCounter;
    private Button addActions;
    private RecyclerView recyclerView;
    private AutoCompleteTextView bannerSelector;
    private ImageView wishIcon;
    private View emptyView;

    private PullsAdapter pullsAdapter;

    private boolean isFirstLaunch = true;

    public PullsCounterView() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivityView = (MainActivityView) getActivity();

        viewModel = new ViewModelProvider(this).get(PullsCounterViewModel.class);

        Fragment parentFragment = getParentFragment();
        if (parentFragment != null) {
            sharedViewModel = new ViewModelProvider(parentFragment).get(SharedJournalViewModel.class);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pulls_counter, container, false);
        addActions = view.findViewById(R.id.btn_add);
        savedWishesCounter = view.findViewById(R.id.tv_pull_counter);
        recyclerView = view.findViewById(R.id.rv_pulls_list);
        bannerSelector = view.findViewById(R.id.mac_banner_selector);
        wishIcon = view.findViewById(R.id.iv_pull);
        emptyView = view.findViewById(R.id.ll_empty_state);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        setupRecyclerView();
        setupBannerSelector();
        observeViewModels();

        addActions.setOnClickListener(this::showMoreMenu);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(200);
        animator.setRemoveDuration(150);
        animator.setMoveDuration(200);
        animator.setChangeDuration(150);
        recyclerView.setItemAnimator(animator);

        pullsAdapter = new PullsAdapter();
        pullsAdapter.setOnItemClickListener(pull -> showWishDialog(pull, null, false));
        recyclerView.setAdapter(pullsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isFirstLaunch && viewModel != null) {
            viewModel.refreshData();
        }
        isFirstLaunch = false;
    }

    private void setupBannerSelector() {
        String[] banners = new String[]{
                getString(R.string.type_event),
                getString(R.string.type_spec),
                getString(R.string.type_std),
                getString(R.string.type_novie)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, banners);
        bannerSelector.setAdapter(adapter);

        bannerSelector.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedLabel = (String) parent.getItemAtPosition(position);
            String bannerKey = BannerType.getKeyByLabel(requireContext(), selectedLabel);
            viewModel.setBannerType(bannerKey);
        });
    }

    private void observeViewModels() {
        viewModel.getWishesLiveData().observe(getViewLifecycleOwner(), pull -> {
            boolean isEmpty = (pull == null || pull.isEmpty());

            pullsAdapter.submitList(pull != null ? new ArrayList<>(pull) : new ArrayList<>(), () -> {
                if (recyclerView.getVisibility() == View.GONE && emptyView.getVisibility() == View.GONE) {
                    if (isEmpty) {
                        emptyView.setAlpha(1f);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setAlpha(1f);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (isEmpty) {
                        crossFadeViews(recyclerView, emptyView);
                    } else {
                        crossFadeViews(emptyView, recyclerView);
                    }
                }
            });
        });

        viewModel.getCurrentPityLiveData().observe(getViewLifecycleOwner(), pity ->
                savedWishesCounter.setText(String.valueOf(pity))
        );

        viewModel.getCurrentBannerTypeLiveData().observe(getViewLifecycleOwner(), bannerKey -> {
            String label = BannerType.getLabelByKey(requireContext(), bannerKey);
            bannerSelector.setText(label, false);
        });

        viewModel.getOpenDialogEvent().observe(getViewLifecycleOwner(), unused ->
                showWishDialog(null, getString(R.string.four_star), false)
        );

        if (sharedViewModel != null) {
            sharedViewModel.getSelectedGameType().observe(getViewLifecycleOwner(), gameType -> {
                updateGameIcon(gameType);
                viewModel.setGameType(gameType);
            });
        }
    }

    private void crossFadeViews(View viewToHide, View viewToShow) {
        if (viewToShow.getVisibility() == View.VISIBLE && viewToHide.getVisibility() == View.GONE) {
            return;
        }

        viewToShow.animate().cancel();
        viewToHide.animate().cancel();

        viewToShow.setAlpha(0f);
        viewToShow.setVisibility(View.VISIBLE);
        viewToShow.animate()
                .alpha(1f)
                .setDuration(150)
                .setListener(null);

        viewToHide.animate()
                .alpha(0f)
                .setDuration(150)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        viewToHide.setVisibility(View.GONE);
                    }
                });
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
        if (mainActivityView == null) return;

        mainActivityView.showWishCounterMenu(new MainActivityView.OnWishCounterMenuClickListener() {
            @Override
            public void onAddOneAttempt() {
                viewModel.addOneAttempt();
            }

            @Override
            public void onAddTenAttempts() {
                viewModel.addTenAttempts();
            }

            @Override
            public void onAddFiveStarDrop() {
                showWishDialog(null, getString(R.string.five_star), true);
            }

            @Override
            public void onAddFourStarDrop() {
                showWishDialog(null, getString(R.string.four_star), false);
            }
        });
    }

    private void showWishDialog(@Nullable Pull wishToEdit, @Nullable String defaultRarity, boolean autoCheckResetPity) {
        Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
        View dialogView = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.dialog_edit_pull_record, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.tv_dialog_header);
        TextInputEditText editDate = dialogView.findViewById(R.id.tiet_edit_date);
        TextInputEditText editDropType = dialogView.findViewById(R.id.tiet_drop_name_edit);
        RadioGroup radioGroupRarity = dialogView.findViewById(R.id.rg_rarity);
        MaterialRadioButton radioThreeStar = dialogView.findViewById(R.id.rb_three_star);
        MaterialRadioButton radioFourStar = dialogView.findViewById(R.id.rb_four_star);
        MaterialRadioButton radioFiveStar = dialogView.findViewById(R.id.rb_five_star);
        CheckBox checkResetPity = dialogView.findViewById(R.id.cb_reset_pity);

        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.getDefault());
        DateTimeFormatter dbFormatter = DateTimeFormatter.ISO_LOCAL_DATE;

        final LocalDate[] selectedDate = new LocalDate[1];
        boolean isEditMode = (wishToEdit != null);

        checkResetPity.setVisibility(View.VISIBLE);

        if (isEditMode) {
            dialogTitle.setText(getString(R.string.edit_pull));
            editDropType.setText(wishToEdit.getDropType());
            checkResetPity.setChecked(wishToEdit.isResetPity());

            try {
                selectedDate[0] = LocalDate.parse(wishToEdit.getDateTime(), dbFormatter);
            } catch (Exception e) {
                selectedDate[0] = LocalDate.now();
            }

            String rarity = wishToEdit.getDropRare();
            if (getString(R.string.four_star).equalsIgnoreCase(rarity)) {
                radioFourStar.setChecked(true);
            } else if (getString(R.string.five_star).equalsIgnoreCase(rarity)) {
                radioFiveStar.setChecked(true);
            } else {
                radioThreeStar.setChecked(true);
            }
        } else {
            dialogTitle.setText(getString(R.string.add_pull));
            checkResetPity.setChecked(autoCheckResetPity);
            selectedDate[0] = LocalDate.now();

            if (getString(R.string.five_star).equalsIgnoreCase(defaultRarity)) {
                radioFiveStar.setChecked(true);
            } else if (getString(R.string.four_star).equalsIgnoreCase(defaultRarity)) {
                radioFourStar.setChecked(true);
            } else {
                radioThreeStar.setChecked(true);
            }
        }

        editDate.setText(selectedDate[0].format(displayFormatter));

        radioGroupRarity.setOnCheckedChangeListener((group, checkedId) -> {
            String currentText = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            if (TextUtils.isEmpty(currentText)) {
                if (checkedId == R.id.rb_three_star) {
                    editDropType.setText(getString(R.string.default_wish_content));
                }
            }
        });

        editDate.setOnClickListener(v -> {
            long selectionEpochMilli = selectedDate[0]
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText(getString(R.string.select_date))
                    .setSelection(selectionEpochMilli)
                    .setTheme(R.style.DatePicker_GachaPoint_MaterialCalendar)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                selectedDate[0] = Instant.ofEpochMilli(selection)
                        .atZone(ZoneId.of("UTC"))
                        .toLocalDate();
                editDate.setText(selectedDate[0].format(displayFormatter));
            });

            datePicker.show(getParentFragmentManager(), "DATE_PICKER");
        });

        builder.setPositiveButton(isEditMode ? getString(R.string.save) : getString(R.string.add_to_bank), (dialog, which) -> {
            String dropType = editDropType.getText() != null ? editDropType.getText().toString().trim() : "";
            int checkedRadioId = radioGroupRarity.getCheckedRadioButtonId();

            String dropRare;
            if (checkedRadioId == R.id.rb_five_star) {
                dropRare = getString(R.string.five_star);
            } else if (checkedRadioId == R.id.rb_four_star) {
                dropRare = getString(R.string.four_star);
            } else {
                dropRare = getString(R.string.three_star);
            }

            boolean isResetPity = checkResetPity.isChecked();

            viewModel.saveWishFromUi(
                    wishToEdit,
                    selectedDate[0],
                    dropType,
                    dropRare,
                    isResetPity
            );
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}