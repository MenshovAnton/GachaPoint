package ru.menshovanton.gachapoint.fragments;

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
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.enums.BannerType;
import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.activities.MainActivity;
import ru.menshovanton.gachapoint.adapters.WishAdapter;
import ru.menshovanton.gachapoint.helpers.DatabaseHelper;
import ru.menshovanton.gachapoint.models.Wish;

public class WishesCounterFragment extends Fragment {
    private MainActivity mainActivity;
    private TextView savedWishesCounter;
    private Button addActions;
    private DatabaseHelper databaseHelper;
    private RecyclerView recyclerView;
    private AutoCompleteTextView bannerSelector;

    private String currentBannerType = BannerType.EVENT.getDbKey();

    public WishesCounterFragment() {}

    public static WishesCounterFragment newInstance(String param1, String param2) {
        return new WishesCounterFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        assert mainActivity != null;
        databaseHelper = new DatabaseHelper(mainActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishes_counter, container, false);
        addActions = view.findViewById(R.id.addActionsButton);
        savedWishesCounter = view.findViewById(R.id.wishesCounter);
        recyclerView = view.findViewById(R.id.wishesLog);
        bannerSelector = view.findViewById(R.id.bannerSelector);
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

        String[] banners = new String[]{
                getString(R.string.type_event),
                getString(R.string.type_spec),
                getString(R.string.type_std),
                getString(R.string.type_novie)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, banners);
        bannerSelector.setAdapter(adapter);
        bannerSelector.setText(getBannerLabel(currentBannerType), false);

        bannerSelector.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedLabel = (String) parent.getItemAtPosition(position);
            currentBannerType = getBannerKey(selectedLabel);
            refreshData(requireView());
        });

        addActions.setOnClickListener(this::showMoreMenu);
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
                wishIcon.setImageResource(R.drawable.icon_intertwined_fate);
                break;
            case 1:
                wishIcon.setImageResource(R.drawable.icon_star_rail_special_pass);
                break;
            case 2:
                wishIcon.setImageResource(R.drawable.icon_encrypted_master_tape);
                break;
        }

        List<Wish> wishes = databaseHelper.getWishesByBanner(mainActivity.getSubType(), currentBannerType);

        int currentPity = calculatePityAndNumbers(wishes);
        savedWishesCounter.setText(String.valueOf(currentPity));

        View emptyView = view.findViewById(R.id.emptyStateView);
        WishAdapter wishAdapter = new WishAdapter(wishes);
        wishAdapter.setOnItemClickListener(wish -> showWishDialog(wish, null, false));

        if (wishes.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            recyclerView.setAdapter(wishAdapter);
        }
    }

    private int calculatePityAndNumbers(List<Wish> wishes) {
        if (wishes.isEmpty()) return 0;

        Collections.reverse(wishes);

        int counter = 0;
        for (Wish wish : wishes) {
            counter++;
            wish.setPityNumber(counter);

            if (wish.isResetPity()) {
                counter = 0;
            }
        }

        Collections.reverse(wishes);

        return counter;
    }

    private void showMoreMenu(View view) {
        ((MainActivity) requireActivity()).showWishCounterMenu(new MainActivity.OnWishCounterMenuClickListener() {
            @Override
            public void onAddOneAttempt() {
                addOneAttempt();
            }

            @Override
            public void onAddTenAttempts() {
                addTenAttempts();
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

    private void showWishDialog(@Nullable Wish wishToEdit, @Nullable String defaultRarity, boolean autoCheckResetPity) {
        Context contextThemeWrapper = new ContextThemeWrapper(requireContext(), R.style.Dialog_GachaPoint_AlertDialog);
        View dialogView = LayoutInflater.from(contextThemeWrapper).inflate(R.layout.dialog_edit_wish, null);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(contextThemeWrapper, R.style.Dialog_GachaPoint_AlertDialog);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextInputEditText editDate = dialogView.findViewById(R.id.editDate);
        TextInputEditText editDropType = dialogView.findViewById(R.id.editDropType);
        RadioGroup radioGroupRarity = dialogView.findViewById(R.id.radioGroupRarity);
        MaterialRadioButton radioThreeStar = dialogView.findViewById(R.id.radioThreeStar);
        MaterialRadioButton radioFourStar = dialogView.findViewById(R.id.radioFourStar);
        MaterialRadioButton radioFiveStar = dialogView.findViewById(R.id.radioFiveStar);
        CheckBox checkResetPity = dialogView.findViewById(R.id.checkResetPity);

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
                if (checkedId == R.id.radioThreeStar) {
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
            if (TextUtils.isEmpty(dropType)) {
                dropType = getString(R.string.default_wish_content);
            }

            int checkedRadioId = radioGroupRarity.getCheckedRadioButtonId();
            String dropRare;
            if (checkedRadioId == R.id.radioFiveStar) {
                dropRare = getString(R.string.five_star);
            } else if (checkedRadioId == R.id.radioFourStar) {
                dropRare = getString(R.string.four_star);
            } else {
                dropRare = getString(R.string.three_star);
            }

            boolean isResetPity = checkResetPity.isChecked();
            String dateForDb = selectedDate[0].format(dbFormatter);

            if (isEditMode) {
                databaseHelper.updateWish(
                        wishToEdit.getId(),
                        dateForDb,
                        dropType,
                        dropRare,
                        mainActivity.getSubType(),
                        currentBannerType,
                        isResetPity
                );
            } else {
                databaseHelper.addWishes(
                        dateForDb,
                        dropType,
                        dropRare,
                        1,
                        mainActivity.getSubType(),
                        currentBannerType,
                        isResetPity
                );
            }

            if (getView() != null) {
                refreshData(getView());
            }
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private String getBannerKey(String label) {
        return BannerType.getKeyByLabel(requireContext(), label);
    }

    private String getBannerLabel(String key) {
        return BannerType.getLabelByKey(requireContext(), key);
    }

    private void addWish(String dropRare, String dropType) {
        databaseHelper.addWishes(
                LocalDate.now().toString(),
                dropType,
                dropRare,
                1,
                mainActivity.getSubType(),
                currentBannerType,
                false
        );
        if (getView() != null) {
            refreshData(getView());
        }
    }

    private void addOneAttempt() {
        addWish(getString(R.string.three_star), getString(R.string.default_wish_content));
    }

    private void addTenAttempts() {
        databaseHelper.addWishes(
                LocalDate.now().toString(),
                getString(R.string.default_wish_content),
                getString(R.string.three_star),
                9,
                mainActivity.getSubType(),
                currentBannerType,
                false
        );
        showWishDialog(null, getString(R.string.four_star), false);
    }
}