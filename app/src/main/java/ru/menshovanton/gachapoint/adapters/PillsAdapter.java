package ru.menshovanton.gachapoint.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import ru.menshovanton.gachapoint.R;

public class PillsAdapter extends RecyclerView.Adapter<PillsAdapter.PillViewHolder> {

    private final List<String> items;
    private int selectedPosition = 0;
    private final OnPillClickListener listener;

    public interface OnPillClickListener {
        void onPillClick(String item, int position);
    }

    public PillsAdapter(List<String> items, OnPillClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PillViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pill, parent, false);
        return new PillViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PillViewHolder holder, int position) {
        holder.textView.setText(items.get(position));
        holder.textView.setSelected(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos == RecyclerView.NO_POSITION) return;

            setSelectedPosition(currentPos);

            if (listener != null) {
                listener.onPillClick(items.get(currentPos), currentPos);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    public static class PillViewHolder extends RecyclerView.ViewHolder {
        public TextView textView;

        public PillViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textViewPill);
        }
    }

    public void setSelectedPosition(int newPosition) {
        if (newPosition < 0 || newPosition >= items.size() || newPosition == selectedPosition) {
            return;
        }

        int previousPosition = selectedPosition;
        selectedPosition = newPosition;

        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }
}