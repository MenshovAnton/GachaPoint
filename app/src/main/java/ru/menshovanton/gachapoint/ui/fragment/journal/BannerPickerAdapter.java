package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.enums.Rarity;
import ru.menshovanton.gachapoint.domain.models.BannerScheduleItem;

public class BannerPickerAdapter extends RecyclerView.Adapter<BannerPickerAdapter.ViewHolder> {

    public interface OnBannerClickListener {
        void onBannerSelected(@NonNull BannerScheduleItem banner);
    }

    private final List<BannerScheduleItem> items = new ArrayList<>();
    private final GameType gameType;
    private final OnBannerClickListener listener;
    private final DateTimeFormatter inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault());

    public BannerPickerAdapter(@NonNull GameType gameType, @NonNull OnBannerClickListener listener) {
        this.gameType = gameType;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(@NonNull List<BannerScheduleItem> newItems) {
        this.items.clear();
        this.items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner_picker, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BannerScheduleItem item = items.get(position);
        Context context = holder.itemView.getContext();

        String patchTitle = context.getString(R.string.patch_format, item.getVersion())
                + " • " + context.getString(R.string.banner_phase_format, item.getPhase());
        holder.tvPatchPhase.setText(patchTitle);

        String dateStr = formatDates(item.getStartDate(), item.getEndDate());
        holder.tvDates.setText(dateStr);

        holder.tvFiveStarBadge.setText(Rarity.LEGENDARY.getDisplayRank(gameType, context));
        holder.tvFourStarBadge.setText(Rarity.EPIC.getDisplayRank(gameType, context));

        List<String> fiveStars = item.getFeaturedFiveStars(context);
        if (!fiveStars.isEmpty()) {
            holder.llFiveStarsContainer.setVisibility(View.VISIBLE);
            holder.tvFiveStars.setText(TextUtils.join(", ", fiveStars));
        } else {
            holder.llFiveStarsContainer.setVisibility(View.GONE);
        }

        List<String> fourStars = item.getFeaturedFourStars(context);
        if (!fourStars.isEmpty()) {
            holder.llFourStarsContainer.setVisibility(View.VISIBLE);
            holder.tvFourStars.setText(TextUtils.join(", ", fourStars));
        } else {
            holder.llFourStarsContainer.setVisibility(View.GONE);
        }

        holder.cardItem.setOnClickListener(v -> listener.onBannerSelected(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatDates(String startIso, String endIso) {
        try {
            LocalDate start = LocalDate.parse(startIso, inputFormatter);
            LocalDate end = LocalDate.parse(endIso, inputFormatter);
            return start.format(outputFormatter) + " — " + end.format(outputFormatter);
        } catch (Exception e) {
            return startIso + " — " + endIso;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardItem;
        final TextView tvPatchPhase;
        final TextView tvDates;
        final View llFiveStarsContainer;
        final TextView tvFiveStarBadge;
        final TextView tvFiveStars;
        final View llFourStarsContainer;
        final TextView tvFourStarBadge;
        final TextView tvFourStars;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardItem = itemView.findViewById(R.id.card_banner_picker_item);
            tvPatchPhase = itemView.findViewById(R.id.tv_picker_patch_phase);
            tvDates = itemView.findViewById(R.id.tv_picker_dates);
            llFiveStarsContainer = itemView.findViewById(R.id.ll_picker_five_stars_container);
            tvFiveStarBadge = itemView.findViewById(R.id.tv_picker_five_star_badge);
            tvFiveStars = itemView.findViewById(R.id.tv_picker_five_stars);
            llFourStarsContainer = itemView.findViewById(R.id.ll_picker_four_stars_container);
            tvFourStarBadge = itemView.findViewById(R.id.tv_picker_four_star_badge);
            tvFourStars = itemView.findViewById(R.id.tv_picker_four_stars);
        }
    }
}
