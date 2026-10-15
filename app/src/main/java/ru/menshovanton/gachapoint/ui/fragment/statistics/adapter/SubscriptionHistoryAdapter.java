package ru.menshovanton.gachapoint.ui.fragment.statistics.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.db.entities.SubscriptionEntity;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class SubscriptionHistoryAdapter extends RecyclerView.Adapter<SubscriptionHistoryAdapter.ViewHolder> {

    private final List<SubscriptionEntity> items = new ArrayList<>();
    private GameType gameType = GameType.GENSHIN;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @SuppressLint("NotifyDataSetChanged")
    public void setData(List<SubscriptionEntity> newItems, GameType gameType) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        this.gameType = gameType != null ? gameType : GameType.GENSHIN;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subscription_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SubscriptionEntity item = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvSubName.setText(context.getString(gameType.getSubscriptionNameResId()));

        LocalDate start = LocalDate.ofEpochDay(item.startDate);
        LocalDate end = LocalDate.ofEpochDay(item.endDate);
        long days = item.endDate - item.startDate + 1;

        String dateRange = start.format(dateFormatter) + " – " + end.format(dateFormatter) + " (" + days + " " + context.getString(R.string.stat_days_unit) + ")";
        holder.tvSubDates.setText(dateRange);

        long today = LocalDate.now().toEpochDay();
        if (today >= item.startDate && today <= item.endDate) {
            holder.tvSubStatus.setText(R.string.stat_sub_status_active);
            holder.tvSubStatus.setTextColor(ContextCompat.getColor(context, R.color.checked));
            holder.cardSubStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_green_bg));
        } else if (today < item.startDate) {
            holder.tvSubStatus.setText(R.string.stat_sub_status_upcoming);
            holder.tvSubStatus.setTextColor(ContextCompat.getColor(context, R.color.check));
            holder.cardSubStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.badge_blue_bg));
        } else {
            holder.tvSubStatus.setText(R.string.stat_sub_status_completed);
            holder.tvSubStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.cardSubStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_pity_badge_bg));
        }

        holder.tvSubClaimed.setText(context.getString(R.string.stat_sub_claimed_fmt, item.claimed));
        holder.tvSubMissed.setText(context.getString(R.string.stat_sub_missed_fmt, item.missed));

        int total = item.claimed + item.missed;
        int progress = (total > 0) ? (int) ((item.claimed * 100f) / total) : 100;
        holder.pbSubProgress.setProgress(progress);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvSubName;
        public final TextView tvSubDates;
        public final TextView tvSubStatus;
        public final TextView tvSubClaimed;
        public final TextView tvSubMissed;
        public final MaterialCardView cardSubStatus;
        public final LinearProgressIndicator pbSubProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubName = itemView.findViewById(R.id.tv_sub_name);
            tvSubDates = itemView.findViewById(R.id.tv_sub_dates);
            tvSubStatus = itemView.findViewById(R.id.tv_sub_status);
            tvSubClaimed = itemView.findViewById(R.id.tv_sub_claimed);
            tvSubMissed = itemView.findViewById(R.id.tv_sub_missed);
            cardSubStatus = itemView.findViewById(R.id.card_sub_status);
            pbSubProgress = itemView.findViewById(R.id.pb_sub_progress);
        }
    }
}
