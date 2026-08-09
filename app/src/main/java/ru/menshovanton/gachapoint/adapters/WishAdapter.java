package ru.menshovanton.gachapoint.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ru.menshovanton.gachapoint.R;

public class WishAdapter extends RecyclerView.Adapter<WishAdapter.WishViewHolder> {

    private final String[] wishes;

    public WishAdapter(String[] wishes) {
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
        holder.date.setText(wishes[position]);
    }

    @Override
    public int getItemCount() {
        return wishes.length;
    }

    static class WishViewHolder extends RecyclerView.ViewHolder {
        TextView date;

        public WishViewHolder(@NonNull View itemView) {
            super(itemView);
            date = itemView.findViewById(R.id.text);
        }
    }
}
