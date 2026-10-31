package ru.menshovanton.gachapoint.ui.fragment.promocodes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.PromoCode;
import ru.menshovanton.gachapoint.ui.common.HapticHelper;
import ru.menshovanton.gachapoint.ui.main.PillsAdapter;

public class PromocodesView extends Fragment {

    private PromocodesViewModel viewModel;
    private PromocodesAdapter adapter;

    private RecyclerView rvGameTypes;
    private ChipGroup chipGroupStatus;
    private Chip chipActive;
    private Chip chipStream;
    private Chip chipPermanent;
    private Chip chipExpired;
    private Chip chipAll;
    private Chip chipRedeemed;
    private RecyclerView rvPromocodes;
    private View emptyStateView;
    private ImageButton btnRefresh;

    private static final GameType[] SUPPORTED_GAMES = {
            GameType.GENSHIN,
            GameType.HSR,
            GameType.ZZZ,
            GameType.HI3
    };

    public PromocodesView() {}

    public static PromocodesView newInstance() {
        return new PromocodesView();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_promocodes, container, false);

        rvGameTypes = root.findViewById(R.id.rv_game_types);
        chipGroupStatus = root.findViewById(R.id.chip_group_status);
        chipActive = root.findViewById(R.id.chip_active);
        chipStream = root.findViewById(R.id.chip_stream);
        chipPermanent = root.findViewById(R.id.chip_permanent);
        chipExpired = root.findViewById(R.id.chip_expired);
        chipAll = root.findViewById(R.id.chip_all);
        chipRedeemed = root.findViewById(R.id.chip_redeemed);
        rvPromocodes = root.findViewById(R.id.rv_promocodes);
        emptyStateView = root.findViewById(R.id.ll_empty_state);
        btnRefresh = root.findViewById(R.id.btn_refresh);

        ImageButton btnBack = root.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(requireContext(), v);
                NavHostFragment.findNavController(this).popBackStack();
            });
        }

        setupFilters();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PromocodesViewModel.class);

        setupGamePills();

        adapter = new PromocodesAdapter(
                viewModel.getSelectedGameType().getValue(),
                (promoCode, isRedeemed) -> viewModel.setRedeemed(promoCode, isRedeemed),
                this::showModerationDialog
        );
        rvPromocodes.setAdapter(adapter);

        setupSwipeToRedeem();

        btnRefresh.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(requireContext(), v);
            btnRefresh.animate().rotationBy(360f).setDuration(400).start();
            viewModel.loadPromocodes(true);
        });

        observeViewModel();
    }

    private void showModerationDialog(PromoCode code) {
        if (code == null || getContext() == null) return;
        boolean currentlyExpired = code.isExpiredByUser();

        int titleRes = currentlyExpired ? R.string.promocode_restore_title : R.string.promocode_report_title;
        String message = currentlyExpired
                ? getString(R.string.promocode_restore_message, code.getCode())
                : getString(R.string.promocode_report_message, code.getCode());
        int positiveRes = currentlyExpired ? R.string.promocode_restore_confirm : R.string.promocode_report_confirm;

        new MaterialAlertDialogBuilder(requireContext(), R.style.Dialog_GachaPoint_AlertDialog)
                .setTitle(titleRes)
                .setMessage(message)
                .setPositiveButton(positiveRes, (dialog, which) -> {
                    boolean newState = !currentlyExpired;
                    viewModel.setExpiredByUser(code, newState);
                    HapticHelper.performClickHaptic(requireContext(), null);
                    int toastRes = newState ? R.string.promocode_marked_expired : R.string.promocode_marked_restored;
                    Toast.makeText(requireContext(), toastRes, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void setupSwipeToRedeem() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                PromoCode code = adapter.getItemAt(position);
                if (code != null) {
                    boolean newState = !code.isRedeemed();
                    viewModel.setRedeemed(code, newState);
                    HapticHelper.performClickHaptic(requireContext(), null);

                    String message = newState
                            ? getString(R.string.promocode_redeemed_success)
                            : getString(R.string.promocode_unredeemed_success);
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyItemChanged(position);
            }
        };

        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvPromocodes);
    }

    private void setupGamePills() {
        GameType current = viewModel.getSelectedGameType().getValue();
        if (current == null) current = GameType.GENSHIN;

        PillsAdapter pillsAdapter = new PillsAdapter(
                Arrays.asList(SUPPORTED_GAMES),
                current,
                (gameType, position) -> {
                    HapticHelper.performClickHaptic(requireContext(), null);
                    adapter = new PromocodesAdapter(
                            gameType,
                            (promoCode, isRedeemed) -> viewModel.setRedeemed(promoCode, isRedeemed),
                            this::showModerationDialog
                    );
                    rvPromocodes.setAdapter(adapter);
                    viewModel.selectGameType(gameType);
                },
                null
        );
        rvGameTypes.setAdapter(pillsAdapter);
    }

    private void setupFilters() {
        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            HapticHelper.performClickHaptic(requireContext(), null);
            int id = checkedIds.get(0);
            if (id == R.id.chip_all) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.ALL);
            } else if (id == R.id.chip_active) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.ACTIVE);
            } else if (id == R.id.chip_stream) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.STREAM);
            } else if (id == R.id.chip_permanent) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.PERMANENT);
            } else if (id == R.id.chip_expired) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.EXPIRED);
            } else if (id == R.id.chip_redeemed) {
                viewModel.setFilterMode(PromocodesViewModel.FilterMode.REDEEMED);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getDisplayedCodes().observe(getViewLifecycleOwner(), codes -> {
            adapter.submitList(codes);
            boolean isEmpty = (codes == null || codes.isEmpty());
            emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            rvPromocodes.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });

        viewModel.getFilterCounts().observe(getViewLifecycleOwner(), counts -> {
            if (counts == null) return;
            if (chipActive != null) {
                chipActive.setText(getString(R.string.promocode_filter_active_count, counts.activeCount));
            }
            if (chipStream != null) {
                chipStream.setText(getString(R.string.promocode_filter_stream_count, counts.streamCount));
            }
            if (chipPermanent != null) {
                chipPermanent.setText(getString(R.string.promocode_filter_permanent_count, counts.permanentCount));
            }
            if (chipExpired != null) {
                chipExpired.setText(getString(R.string.promocode_filter_expired_count, counts.expiredCount));
            }
            if (chipAll != null) {
                chipAll.setText(getString(R.string.promocode_filter_all_count, counts.allCount));
            }
            if (chipRedeemed != null) {
                chipRedeemed.setText(getString(R.string.promocode_filter_redeemed_count, counts.redeemedCount));
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                btnRefresh.setEnabled(false);
                btnRefresh.setAlpha(0.6f);
            } else {
                btnRefresh.setEnabled(true);
                btnRefresh.setAlpha(1.0f);
            }
        });
    }
}
