package ru.menshovanton.gachapoint.ui.fragment.banners;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.CharacterRerunInfo;

public class CharacterRerunAdapter extends RecyclerView.Adapter<CharacterRerunAdapter.RerunViewHolder> {

    private final List<CharacterRerunInfo> items = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submitList(List<CharacterRerunInfo> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RerunViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_character_rerun, parent, false);
        return new RerunViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RerunViewHolder holder, int position) {
        CharacterRerunInfo rerun = items.get(position);
        Context context = holder.itemView.getContext();

        String name = rerun.getCharacterName(context);
        holder.tvName.setText(name);
        ru.menshovanton.gachapoint.domain.enums.GameType gt =
                ru.menshovanton.gachapoint.domain.enums.GameType.fromId(rerun.getGameType());
        ru.menshovanton.gachapoint.domain.models.CatalogItem item =
                ru.menshovanton.gachapoint.data.repository.CatalogRepository.getInstance(context)
                        .findItemByName(gt, name);
        String iconUrl = (item != null) ? item.getIconUrl() : null;

        ru.menshovanton.gachapoint.ui.common.AvatarImageLoader.loadAvatar(
                context,
                iconUrl,
                name,
                holder.ivAvatar,
                holder.tvAvatarLetter
        );

        int days = rerun.getDaysSinceLastRerun();
        if (days == 999 || rerun.getLastVersion() == null) {
            holder.tvLastSeen.setText(context.getString(R.string.banners_rerun_never));
            holder.tvDaysBadge.setText("—");
            holder.cardDaysBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.pull_pity_badge_bg)));
            holder.tvDaysBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
        } else if (days <= 0) {
            holder.tvLastSeen.setText(context.getString(R.string.banners_last_seen, "v" + rerun.getLastVersion()));
            holder.tvDaysBadge.setText(context.getString(R.string.banners_rerun_active));
            holder.cardDaysBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_green_bg)));
            holder.tvDaysBadge.setTextColor(ContextCompat.getColor(context, R.color.checked));
        } else {
            holder.tvLastSeen.setText(context.getString(R.string.banners_last_seen, "v" + rerun.getLastVersion()));
            holder.tvDaysBadge.setText(context.getString(R.string.banners_rerun_days_ago, days));

            if (days > 180) {
                holder.cardDaysBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_red_bg)));
                holder.tvDaysBadge.setTextColor(ContextCompat.getColor(context, R.color.missed));
            } else if (days < 60) {
                holder.cardDaysBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_green_bg)));
                holder.tvDaysBadge.setTextColor(ContextCompat.getColor(context, R.color.checked));
            } else {
                holder.cardDaysBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.pull_pity_badge_bg)));
                holder.tvDaysBadge.setTextColor(ContextCompat.getColor(context, R.color.text));
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class RerunViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivAvatar;
        final TextView tvAvatarLetter;
        final TextView tvName;
        final TextView tvLastSeen;
        final MaterialCardView cardDaysBadge;
        final TextView tvDaysBadge;

        RerunViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_rerun_avatar);
            tvAvatarLetter = itemView.findViewById(R.id.tv_rerun_avatar_letter);
            tvName = itemView.findViewById(R.id.tv_rerun_character_name);
            tvLastSeen = itemView.findViewById(R.id.tv_rerun_last_seen);
            cardDaysBadge = itemView.findViewById(R.id.card_rerun_badge);
            tvDaysBadge = itemView.findViewById(R.id.tv_rerun_days_badge);
        }
    }
}
