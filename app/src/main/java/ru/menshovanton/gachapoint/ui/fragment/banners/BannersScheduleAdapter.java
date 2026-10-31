package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;

public class BannersScheduleAdapter extends RecyclerView.Adapter<BannersScheduleAdapter.BannerViewHolder> {

    private final List<BannerScheduleItem> items = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submitList(List<BannerScheduleItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_banner_schedule, parent, false);
        return new BannerViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        BannerScheduleItem banner = items.get(position);
        Context context = holder.itemView.getContext();

        boolean isRu = "ru".equalsIgnoreCase(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
        holder.tvVersionTitle.setText((isRu ? "Патч " : "Version ") + banner.getVersion() + (isRu ? " — Фаза " : " — Phase ") + banner.getPhase());

        String fiveStars = android.text.TextUtils.join(", ", banner.getFeaturedFiveStars(context));
        holder.tvFiveStars.setText(!fiveStars.isEmpty() ? fiveStars : "—");

        String fourStars = android.text.TextUtils.join(", ", banner.getFeaturedFourStars(context));
        holder.tvFourStars.setText(!fourStars.isEmpty() ? fourStars : "—");

        holder.tvDates.setText(banner.getStartDate() + " — " + banner.getEndDate());

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ISO_LOCAL_DATE;

        try {
            LocalDate start = LocalDate.parse(banner.getStartDate(), fmt);
            LocalDate end = LocalDate.parse(banner.getEndDate(), fmt);
            String bannerStatus = banner.getStatus();

            boolean isCurrent = "CURRENT".equalsIgnoreCase(bannerStatus) || (!today.isBefore(start) && !today.isAfter(end));
            boolean isAnnounced = !isCurrent && ("ANNOUNCED".equalsIgnoreCase(bannerStatus) || today.isBefore(start));

            if (isCurrent) {
                holder.tvStatus.setText(R.string.banners_status_current);
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.checked));
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_green_bg)));

                long daysLeft = ChronoUnit.DAYS.between(today, end);
                if (daysLeft >= 0) {
                    holder.tvTimer.setText(context.getString(R.string.banners_days_left, (int) daysLeft));
                    holder.tvTimer.setTextColor(ContextCompat.getColor(context, R.color.checked));
                    holder.tvTimer.setVisibility(View.VISIBLE);
                } else {
                    holder.tvTimer.setVisibility(View.GONE);
                }
            } else if (isAnnounced) {
                holder.tvStatus.setText(R.string.banners_status_future);
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.rare_epic));
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_purple_bg)));

                long daysUntil = ChronoUnit.DAYS.between(today, start);
                if (daysUntil > 0) {
                    holder.tvTimer.setText(context.getString(R.string.banners_starts_in, (int) daysUntil));
                    holder.tvTimer.setTextColor(ContextCompat.getColor(context, R.color.rare_epic));
                    holder.tvTimer.setVisibility(View.VISIBLE);
                } else {
                    holder.tvTimer.setVisibility(View.GONE);
                }
            } else {
                holder.tvStatus.setText(R.string.banners_status_past);
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                holder.cardStatus.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.pull_pity_badge_bg)));
                holder.tvTimer.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            holder.tvTimer.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        final TextView tvVersionTitle;
        final MaterialCardView cardStatus;
        final TextView tvStatus;
        final TextView tvTimer;
        final TextView tvDates;
        final TextView tvFiveStars;
        final TextView tvFourStars;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVersionTitle = itemView.findViewById(R.id.tv_banner_version_title);
            cardStatus = itemView.findViewById(R.id.card_banner_status);
            tvStatus = itemView.findViewById(R.id.tv_banner_status);
            tvTimer = itemView.findViewById(R.id.tv_banner_timer);
            tvDates = itemView.findViewById(R.id.tv_banner_dates);
            tvFiveStars = itemView.findViewById(R.id.tv_five_stars_list);
            tvFourStars = itemView.findViewById(R.id.tv_four_stars_list);
        }
    }
}
