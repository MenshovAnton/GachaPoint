package ru.menshovanton.gachapoint.ui.fragment.journal.pullscounter;

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
import ru.menshovanton.gachapoint.domain.models.Pull;

public class PullsAdapter extends ListAdapter<Pull, PullsAdapter.PullsViewHolder> {

    private static final DateTimeFormatter DB_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Pull pull);
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
    public PullsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pull, parent, false);
        return new PullsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PullsViewHolder holder, int position) {
        Pull pull = getItem(position);

        holder.numberOfWish.setText(String.valueOf(pull.getPityNumber()));
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

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(pull);
            }
        });
    }

    public static class PullsViewHolder extends RecyclerView.ViewHolder {
        TextView numberOfWish;
        TextView dropRare;
        TextView dropType;
        TextView date;

        public PullsViewHolder(@NonNull View itemView) {
            super(itemView);
            numberOfWish = itemView.findViewById(R.id.numberOfWish);
            dropRare = itemView.findViewById(R.id.dropRare);
            dropType = itemView.findViewById(R.id.dropType);
            date = itemView.findViewById(R.id.date);
        }
    }
}