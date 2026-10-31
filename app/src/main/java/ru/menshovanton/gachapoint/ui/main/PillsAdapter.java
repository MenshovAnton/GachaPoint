package ru.menshovanton.gachapoint.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class PillsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_PILL = 0;
    public static final int VIEW_TYPE_SETTINGS = 1;
    public static final String PAYLOAD_SELECTION = "payload_selection";

    public static class PillItem {
        public final int viewType;
        public final GameType gameType;

        public PillItem(int viewType, GameType gameType) {
            this.viewType = viewType;
            this.gameType = gameType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PillItem)) return false;
            PillItem that = (PillItem) o;
            return viewType == that.viewType && gameType == that.gameType;
        }

        @Override
        public int hashCode() {
            int result = viewType;
            result = 31 * result + (gameType != null ? gameType.hashCode() : 0);
            return result;
        }
    }

    private final List<PillItem> displayItems = new ArrayList<>();
    private GameType selectedGame;
    private final OnGamePillClickListener listener;
    private final OnSettingsClickListener settingsListener;

    public interface OnGamePillClickListener {
        void onGamePillClick(GameType gameType, int position);
    }

    public interface OnSettingsClickListener {
        void onSettingsClick();
    }

    public PillsAdapter(List<GameType> items, GameType initialSelectedGame,
                        OnGamePillClickListener listener,
                        OnSettingsClickListener settingsListener) {
        this.selectedGame = initialSelectedGame;
        this.listener = listener;
        this.settingsListener = settingsListener;
        this.displayItems.addAll(buildDisplayItems(items));
    }

    private List<PillItem> buildDisplayItems(List<GameType> games) {
        List<PillItem> list = new ArrayList<>();
        if (games != null) {
            for (GameType g : games) {
                list.add(new PillItem(VIEW_TYPE_PILL, g));
            }
            if (games.size() >= 2 && settingsListener != null) {
                list.add(new PillItem(VIEW_TYPE_SETTINGS, null));
            }
        }
        return list;
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SETTINGS) {
            View view = inflater.inflate(R.layout.item_pill_settings, parent, false);
            return new SettingsViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_pill, parent, false);
            return new PillViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        onBindViewHolder(holder, position, new ArrayList<>());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        holder.itemView.setAlpha(1.0f);
        holder.itemView.setScaleX(1.0f);
        holder.itemView.setScaleY(1.0f);
        holder.itemView.setTranslationX(0f);
        holder.itemView.setTranslationY(0f);
        holder.itemView.setVisibility(View.VISIBLE);

        PillItem item = displayItems.get(position);

        if (holder instanceof SettingsViewHolder) {
            holder.itemView.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.88f)
                        .scaleY(0.88f)
                        .setDuration(60)
                        .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                        .start();
                if (settingsListener != null) {
                    settingsListener.onSettingsClick();
                }
            });
            return;
        }

        if (holder instanceof PillViewHolder) {
            PillViewHolder pillHolder = (PillViewHolder) holder;
            GameType game = item.gameType;

            if (!payloads.isEmpty()) {
                pillHolder.textView.setSelected(game == selectedGame);
                return;
            }

            Context context = holder.itemView.getContext();
            if (game != null) {
                pillHolder.textView.setText(context.getString(game.getNameResId()));
                pillHolder.textView.setSelected(game == selectedGame);
            }

            holder.itemView.setOnClickListener(v -> {
                v.animate()
                        .scaleX(0.93f)
                        .scaleY(0.93f)
                        .setDuration(60)
                        .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start())
                        .start();

                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= displayItems.size()) return;

                PillItem clickedItem = displayItems.get(pos);
                if (clickedItem.viewType != VIEW_TYPE_PILL || clickedItem.gameType == null) return;

                setSelectedGame(clickedItem.gameType);

                if (listener != null) {
                    listener.onGamePillClick(clickedItem.gameType, pos);
                }
            });
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setAlpha(1.0f);
        holder.itemView.setScaleX(1.0f);
        holder.itemView.setScaleY(1.0f);
        holder.itemView.setTranslationX(0f);
        holder.itemView.setTranslationY(0f);
        holder.itemView.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public GameType getSelectedGame() {
        return selectedGame;
    }

    public int getPositionForGame(GameType game) {
        for (int i = 0; i < displayItems.size(); i++) {
            PillItem item = displayItems.get(i);
            if (item.viewType == VIEW_TYPE_PILL && item.gameType == game) {
                return i;
            }
        }
        return -1;
    }

    public void setSelectedGame(GameType newGame) {
        if (newGame == null || newGame == selectedGame) return;
        GameType oldGame = selectedGame;
        selectedGame = newGame;

        int oldPos = getPositionForGame(oldGame);
        int newPos = getPositionForGame(newGame);
        if (oldPos != -1) notifyItemChanged(oldPos, PAYLOAD_SELECTION);
        if (newPos != -1) notifyItemChanged(newPos, PAYLOAD_SELECTION);
    }

    public void updateData(List<GameType> newGames, GameType newSelectedGame) {
        List<PillItem> oldList = new ArrayList<>(this.displayItems);
        GameType oldSelectedGame = this.selectedGame;

        List<PillItem> newList = buildDisplayItems(newGames);
        this.selectedGame = newSelectedGame;

        boolean sameItems = (oldList.size() == newList.size());
        if (sameItems) {
            for (int i = 0; i < oldList.size(); i++) {
                if (!oldList.get(i).equals(newList.get(i))) {
                    sameItems = false;
                    break;
                }
            }
        }

        if (sameItems) {
            if (oldSelectedGame != newSelectedGame) {
                int oldPos = getPositionForGame(oldSelectedGame);
                int newPos = getPositionForGame(newSelectedGame);
                if (oldPos != -1) notifyItemChanged(oldPos, PAYLOAD_SELECTION);
                if (newPos != -1) notifyItemChanged(newPos, PAYLOAD_SELECTION);
            }
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                PillItem oldItem = oldList.get(oldItemPosition);
                PillItem newItem = newList.get(newItemPosition);
                if (oldItem.viewType != newItem.viewType) return false;
                return oldItem.gameType == newItem.gameType;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                PillItem oldItem = oldList.get(oldItemPosition);
                PillItem newItem = newList.get(newItemPosition);
                if (oldItem.viewType != newItem.viewType) return false;
                if (oldItem.viewType == VIEW_TYPE_SETTINGS) return true;

                boolean wasSelected = (oldItem.gameType == oldSelectedGame);
                boolean isSelected = (newItem.gameType == newSelectedGame);
                return wasSelected == isSelected;
            }

            @NonNull
            @Override
            public Object getChangePayload(int oldItemPosition, int newItemPosition) {
                return PAYLOAD_SELECTION;
            }
        });

        this.displayItems.clear();
        this.displayItems.addAll(newList);
        diffResult.dispatchUpdatesTo(this);
    }

    public static class PillViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public PillViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tv_pill);
        }
    }

    public static class SettingsViewHolder extends RecyclerView.ViewHolder {
        public SettingsViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
