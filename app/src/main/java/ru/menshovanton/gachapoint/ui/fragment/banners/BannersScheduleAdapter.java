package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import ru.menshovanton.gachapoint.data.repository.CatalogRepository;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;

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
        String cleanVersion = banner.getVersion() != null ? banner.getVersion().replaceAll("^[vV]+", "") : "";
        holder.tvVersionTitle.setText((isRu ? "Патч " : "Version ") + cleanVersion + (isRu ? " — Фаза " : " — Phase ") + banner.getPhase());

        List<String> fiveStars = banner.getFeaturedFiveStars(context);
        String fiveStarsText = android.text.TextUtils.join(", ", fiveStars);
        holder.tvFiveStars.setText(!fiveStarsText.isEmpty() ? fiveStarsText : "—");

        List<String> fourStars = banner.getFeaturedFourStars(context);
        String fourStarsText = android.text.TextUtils.join(", ", fourStars);
        holder.tvFourStars.setText(!fourStarsText.isEmpty() ? fourStarsText : "—");

        holder.tvDates.setText(banner.getStartDate() + " — " + banner.getEndDate());

        // Bind 5★ and 4★ Avatars
        GameType gameType = GameType.fromId(banner.getGameType());
        bindAvatars(holder.llFiveStarsAvatars, fiveStars, gameType, true, context);
        bindAvatars(holder.llFourStarsAvatars, fourStars, gameType, false, context);

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

    private void bindAvatars(
            LinearLayout container,
            List<String> names,
            GameType gameType,
            boolean isFiveStar,
            Context context
    ) {
        if (container == null) return;
        container.removeAllViews();
        if (names == null || names.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        CatalogRepository catalogRepo = CatalogRepository.getInstance(context);
        int size = (int) (38 * context.getResources().getDisplayMetrics().density);
        int margin = (int) (6 * context.getResources().getDisplayMetrics().density);

        for (String name : names) {
            CatalogItem item = catalogRepo.findItemByName(gameType, name);

            MaterialCardView card = new MaterialCardView(context);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(margin);
            card.setLayoutParams(lp);
            card.setRadius(size / 2f);
            card.setCardElevation(0);
            card.setStrokeWidth((int) (1.5f * context.getResources().getDisplayMetrics().density));

            int strokeColor = isFiveStar
                    ? ContextCompat.getColor(context, R.color.rare_legendary)
                    : ContextCompat.getColor(context, R.color.rare_epic);
            int bgColor = isFiveStar
                    ? ContextCompat.getColor(context, R.color.badge_gold_bg)
                    : ContextCompat.getColor(context, R.color.badge_purple_bg);

            card.setStrokeColor(ColorStateList.valueOf(strokeColor));
            card.setCardBackgroundColor(ColorStateList.valueOf(bgColor));

            android.widget.FrameLayout fl = new android.widget.FrameLayout(context);
            fl.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            TextView tv = new TextView(context);
            tv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(strokeColor);
            tv.setTextSize(14);
            try {
                tv.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.genshin_font));
            } catch (Exception ignored) {}

            ImageView iv = new ImageView(context);
            iv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

            fl.addView(tv);
            fl.addView(iv);
            card.addView(fl);

            String iconUrl = (item != null) ? item.getIconUrl() : null;
            ru.menshovanton.gachapoint.ui.common.AvatarImageLoader.loadAvatar(
                    context,
                    iconUrl,
                    name,
                    iv,
                    tv
            );

            container.addView(card);
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
        final LinearLayout llFiveStarsAvatars;
        final LinearLayout llFourStarsAvatars;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvVersionTitle = itemView.findViewById(R.id.tv_banner_version_title);
            cardStatus = itemView.findViewById(R.id.card_banner_status);
            tvStatus = itemView.findViewById(R.id.tv_banner_status);
            tvTimer = itemView.findViewById(R.id.tv_banner_timer);
            tvDates = itemView.findViewById(R.id.tv_banner_dates);
            tvFiveStars = itemView.findViewById(R.id.tv_five_stars_list);
            tvFourStars = itemView.findViewById(R.id.tv_four_stars_list);
            llFiveStarsAvatars = itemView.findViewById(R.id.ll_five_stars_avatars);
            llFourStarsAvatars = itemView.findViewById(R.id.ll_four_stars_avatars);
        }
    }
}
