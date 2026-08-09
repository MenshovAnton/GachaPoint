package ru.menshovanton.gachapoint.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.models.Wish;

public class WishAdapter extends RecyclerView.Adapter<WishAdapter.WishViewHolder> {

    private final List<Wish> wishes;

    public WishAdapter(List<Wish> wishes) {
        this.wishes = wishes;
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

        String displayText = wish.getDateTime() + "\n" + wish.getContent();
        holder.date.setText(displayText);
    }

    @Override
    public int getItemCount() {
        return wishes != null ? wishes.size() : 0;
    }

    public static class WishViewHolder extends RecyclerView.ViewHolder {
        TextView date;

        public WishViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.text);
        }
    }
}