package ru.menshovanton.gachapoint.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.models.Wish;

public class WishAdapter extends RecyclerView.Adapter<WishAdapter.WishViewHolder> {

    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final List<Wish> wishes;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Wish wish);
    }

    public WishAdapter(List<Wish> wishes) {
        this.wishes = wishes;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public WishViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wishes, parent, false);
        return new WishViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishViewHolder holder, int position) {
        Wish wish = wishes.get(position);

        holder.numberOfWish.setText(String.valueOf(wish.getId()));
        holder.dropRare.setText(wish.getDropRare());
        holder.dropType.setText(wish.getDropType());

        String rawDate = wish.getDateTime();
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(wish);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wishes != null ? wishes.size() : 0;
    }

    public static class WishViewHolder extends RecyclerView.ViewHolder {
        TextView numberOfWish;
        TextView dropRare;
        TextView dropType;
        TextView date;

        public WishViewHolder(@NonNull View itemView) {
            super(itemView);
            numberOfWish = itemView.findViewById(R.id.numberOfWish);
            dropRare = itemView.findViewById(R.id.dropRare);
            dropType = itemView.findViewById(R.id.dropType);
            date = itemView.findViewById(R.id.date);
        }
    }
}