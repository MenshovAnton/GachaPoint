package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.PityStatus;
import ru.menshovanton.gachapoint.domain.enums.RareType;
import ru.menshovanton.gachapoint.domain.models.Pull;

public class PullsAdapter extends ListAdapter<Pull, PullsAdapter.PullsViewHolder> {

    private static final String PAYLOAD_SELECTION = "payload_selection";
    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private OnItemClickListener listener;
    private OnItemLongClickListener longClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    private boolean isSelectionMode = false;
    private final Set<Integer> selectedPullIds = new HashSet<>();

    public interface OnItemClickListener {
        void onItemClick(Pull pull);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(Pull pull);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    public PullsAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Pull> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Pull oldItem, @NonNull Pull newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Pull oldItem, @NonNull Pull newItem) {
            return oldItem.getPityNumber() == newItem.getPityNumber()
                    && oldItem.getPity4StarNumber() == newItem.getPity4StarNumber()
                    && Objects.equals(oldItem.getPityStatus(), newItem.getPityStatus())
                    && Objects.equals(oldItem.getDropRare(), newItem.getDropRare())
                    && Objects.equals(oldItem.getDropType(), newItem.getDropType())
                    && Objects.equals(oldItem.getDateTime(), newItem.getDateTime());
        }
    };

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener selectionChangedListener) {
        this.selectionChangedListener = selectionChangedListener;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public Set<Integer> getSelectedPullIds() {
        return new HashSet<>(selectedPullIds);
    }

    public void startSelection(Pull initialPull) {
        isSelectionMode = true;
        selectedPullIds.clear();
        if (initialPull != null) {
            selectedPullIds.add(initialPull.getId());
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        notifySelectionChanged();
    }

    public void toggleSelection(int pullId) {
        if (selectedPullIds.contains(pullId)) {
            selectedPullIds.remove(pullId);
        } else {
            selectedPullIds.add(pullId);
        }

        if (selectedPullIds.isEmpty()) {
            clearSelection();
            return;
        }

        int position = findPositionByPullId(pullId);
        if (position != -1) {
            notifyItemChanged(position, PAYLOAD_SELECTION);
        } else {
            notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        }
        notifySelectionChanged();
    }

    public void selectAll() {
        selectedPullIds.clear();
        List<Pull> currentList = getCurrentList();
        for (Pull pull : currentList) {
            selectedPullIds.add(pull.getId());
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        notifySelectionChanged();
    }

    public void clearSelection() {
        isSelectionMode = false;
        selectedPullIds.clear();
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        notifySelectionChanged();
    }

    private int findPositionByPullId(int pullId) {
        List<Pull> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            if (currentList.get(i).getId() == pullId) {
                return i;
            }
        }
        return -1;
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedPullIds.size());
        }
    }

    @NonNull
    @Override
    public PullsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pull, parent, false);
        return new PullsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PullsViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            Pull pull = getItem(position);
            applySelectionAndStyling(holder, pull, holder.itemView.getContext());
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PullsViewHolder holder, int position) {
        Pull pull = getItem(position);
        Context context = holder.itemView.getContext();

        holder.dropRare.setText(pull.getDropRare());
        holder.dropType.setText(pull.getDropType());

        String rawDate = pull.getDateTime();
        if (rawDate != null) {
            try {
                LocalDate parsedDate = LocalDate.parse(rawDate, DB_FORMATTER);
                holder.date.setText(parsedDate.format(DISPLAY_FORMATTER));
            } catch (Exception e) {
                holder.date.setText(rawDate);
            }
        } else {
            holder.date.setText("");
        }

        applySelectionAndStyling(holder, pull, context);

        holder.cardView.setOnClickListener(v -> {
            if (isSelectionMode) {
                toggleSelection(pull.getId());
            } else if (listener != null) {
                listener.onItemClick(pull);
            }
        });

        holder.cardView.setOnLongClickListener(v -> {
            if (!isSelectionMode && longClickListener != null) {
                longClickListener.onItemLongClick(pull);
                return true;
            } else if (isSelectionMode) {
                toggleSelection(pull.getId());
                return true;
            }
            return false;
        });
    }

    private void applySelectionAndStyling(@NonNull PullsViewHolder holder, Pull pull, Context context) {
        RareType rareType = RareType.from(pull.getDropRare(), context);
        boolean isSelected = isSelectionMode && selectedPullIds.contains(pull.getId());

        if (isSelectionMode) {
            holder.cbSelect.setVisibility(View.VISIBLE);
            holder.cbSelect.setChecked(isSelected);
        } else {
            holder.cbSelect.setVisibility(View.GONE);
        }

        if (isSelected) {
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_row_selected_bg));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_selected_stroke));
            holder.cardView.setStrokeWidth((int) (1.5f * context.getResources().getDisplayMetrics().density));
        } else {
            holder.cardView.setStrokeWidth((int) (context.getResources().getDisplayMetrics().density));
        }

        if (rareType == RareType.LEGENDARY) {
            if (!isSelected) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_gold_bg));
                holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.rare_legendary));
            }
            holder.badgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rare_legendary));
            holder.tvPityBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvPityBadge.setText(String.format(context.getString(R.string.pity_pull_at), pull.getPityNumber()));

            holder.dropRare.setTextColor(ContextCompat.getColor(context, R.color.rare_legendary));
            holder.dropType.setTextColor(ContextCompat.getColor(context, R.color.text));

            PityStatus pityStatus = pull.getPityStatus();
            if (pityStatus != null && pityStatus != PityStatus.NONE) {
                holder.tvOutcome.setVisibility(View.VISIBLE);
                holder.tvOutcome.setText(context.getString(pityStatus.getLabelResId()));
                if (pityStatus == PityStatus.LOSS_50_50) {
                    holder.tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.missed));
                } else if (pityStatus == PityStatus.WIN_50_50) {
                    holder.tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.checked));
                } else {
                    holder.tvOutcome.setTextColor(ContextCompat.getColor(context, R.color.rare_legendary));
                }
            } else {
                holder.tvOutcome.setVisibility(View.GONE);
            }
        } else if (rareType == RareType.EPIC) {
            if (!isSelected) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_purple_bg));
                holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.rare_epic));
            }
            holder.badgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.rare_epic));
            holder.tvPityBadge.setTextColor(ContextCompat.getColor(context, R.color.white));
            holder.tvPityBadge.setText(String.format(context.getString(R.string.pity_pull_at), pull.getPity4StarNumber()));

            holder.dropRare.setTextColor(ContextCompat.getColor(context, R.color.rare_epic));
            holder.dropType.setTextColor(ContextCompat.getColor(context, R.color.text));
            holder.tvOutcome.setVisibility(View.GONE);
        } else {
            if (!isSelected) {
                holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_row_bg));
                holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
            }
            holder.badgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_pity_badge_bg));
            holder.tvPityBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.tvPityBadge.setText(String.valueOf(pull.getPityNumber()));

            holder.dropRare.setTextColor(ContextCompat.getColor(context, R.color.check));
            holder.dropType.setTextColor(ContextCompat.getColor(context, R.color.text));
            holder.tvOutcome.setVisibility(View.GONE);
        }
    }

    public static class PullsViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        MaterialCardView badgeCard;
        TextView tvPityBadge;
        TextView dropRare;
        TextView dropType;
        TextView tvOutcome;
        TextView date;
        MaterialCheckBox cbSelect;

        public PullsViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_pull);
            badgeCard = itemView.findViewById(R.id.badge_pity_count);
            tvPityBadge = itemView.findViewById(R.id.tv_pity_badge);
            dropRare = itemView.findViewById(R.id.dropRare);
            dropType = itemView.findViewById(R.id.dropType);
            tvOutcome = itemView.findViewById(R.id.tv_pull_outcome);
            date = itemView.findViewById(R.id.date);
            cbSelect = itemView.findViewById(R.id.cb_select);
        }
    }
}