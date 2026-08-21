package ru.menshovanton.gachapoint.ui.fragment.journal.wishescounter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.Wish;

public class WishAdapter extends ListAdapter<Wish, WishAdapter.WishViewHolder> {

    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Wish wish);
    }

    public WishAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Wish> DIFF_CALLBACK = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Wish oldItem, @NonNull Wish newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Wish oldItem, @NonNull Wish newItem) {
            return oldItem.getPityNumber() == newItem.getPityNumber()
                    && oldItem.isResetPity() == newItem.isResetPity()
                    && Objects.equals(oldItem.getDropRare(), newItem.getDropRare())
                    && Objects.equals(oldItem.getDropType(), newItem.getDropType())
                    && Objects.equals(oldItem.getDateTime(), newItem.getDateTime());
        }
    };

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
        Wish wish = getItem(position);

        holder.numberOfWish.setText(String.valueOf(wish.getPityNumber()));
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