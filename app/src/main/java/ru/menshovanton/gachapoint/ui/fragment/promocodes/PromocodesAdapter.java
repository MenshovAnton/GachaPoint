package ru.menshovanton.gachapoint.ui.fragment.promocodes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.DataSyncConfig;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.domain.models.PromoCode;
import ru.menshovanton.gachapoint.ui.common.ChromeCustomTabsHelper;
import ru.menshovanton.gachapoint.ui.common.HapticHelper;

public class PromocodesAdapter extends RecyclerView.Adapter<PromocodesAdapter.PromocodeViewHolder> {

    public interface OnRedeemedListener {
        void onRedeemed(PromoCode promoCode, boolean isRedeemed);
    }

    public interface OnExpiredListener {
        void onToggleExpired(PromoCode promoCode);
    }

    private final GameType gameType;
    private final OnExpiredListener onExpiredListener;
    private final List<PromoCode> items = new ArrayList<>();

    public PromocodesAdapter(GameType gameType, OnRedeemedListener onRedeemedListener, OnExpiredListener onExpiredListener) {
        this.gameType = gameType;
        this.onExpiredListener = onExpiredListener;
    }

    public PromoCode getItemAt(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    public void submitList(List<PromoCode> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }
            @Override
            public int getNewListSize() { return newItems != null ? newItems.size() : 0; }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return items.get(oldItemPosition).getId().equals(newItems.get(newItemPosition).getId());
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                PromoCode oldItem = items.get(oldItemPosition);
                PromoCode newItem = newItems.get(newItemPosition);
                return oldItem.isRedeemed() == newItem.isRedeemed()
                        && oldItem.isExpired() == newItem.isExpired()
                        && oldItem.isExpiredByUser() == newItem.isExpiredByUser()
                        && Objects.equals(oldItem.getCode(), newItem.getCode())
                        && Objects.equals(oldItem.getStatus(), newItem.getStatus());
            }
        });

        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PromocodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_promocode, parent, false);
        return new PromocodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PromocodeViewHolder holder, int position) {
        PromoCode code = items.get(position);
        Context context = holder.itemView.getContext();

        holder.tvCode.setText(code.getCode());
        holder.tvRewardsDescription.setText(code.getRewardsDescription(context));

        if (gameType != null) {
            holder.ivRewardIcon.setImageResource(gameType.getPrimaryCurrencyDrawableResId());
        }

        setupStatusBadge(holder, code, context);

        boolean isExpired = code.isExpired();
        boolean isRedeemed = code.isRedeemed();
        if (isExpired) {
            holder.cardPromocode.setAlpha(0.40f);
        } else if (isRedeemed) {
            holder.cardPromocode.setAlpha(0.55f);
        } else {
            holder.cardPromocode.setAlpha(1.0f);
        }

        if (holder.cardRedeemedBadge != null) {
            holder.cardRedeemedBadge.setVisibility(isRedeemed ? View.VISIBLE : View.GONE);
        }

        if (holder.btnPromocodeOptions != null) {
            holder.btnPromocodeOptions.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(context, v);
                if (onExpiredListener != null) {
                    onExpiredListener.onToggleExpired(code);
                }
            });
        }

        holder.cardPromocode.setOnLongClickListener(v -> {
            HapticHelper.performClickHaptic(context, v);
            if (onExpiredListener != null) {
                onExpiredListener.onToggleExpired(code);
                return true;
            }
            return false;
        });

        holder.btnCopy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                ClipData clip = ClipData.newPlainText("PromoCode", code.getCode());
                cm.setPrimaryClip(clip);
                HapticHelper.performClickHaptic(context, v);
                Toast.makeText(context, R.string.promocode_copied, Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnActivate.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(context, v);
            String url = DataSyncConfig.getOfficialRedeemUrl(gameType != null ? gameType : GameType.GENSHIN, code.getCode());
            if (url != null) {
                ChromeCustomTabsHelper.openUrl(context, url);
            }
        });
    }

    private void setupStatusBadge(@NonNull PromocodeViewHolder holder, @NonNull PromoCode code, @NonNull Context context) {
        boolean isExpired = code.isExpired();

        if (holder.cardExpiredBadge != null) {
            holder.cardExpiredBadge.setVisibility(isExpired ? View.VISIBLE : View.GONE);
        }

        if (isExpired) {
            holder.cardStatusBadge.setVisibility(View.GONE);
            return;
        }

        String status = code.getStatus();
        if ("STREAM".equalsIgnoreCase(status) || code.isStream()) {
            long remainingHours = code.getRemainingHours();
            if (remainingHours > 0 && remainingHours <= 24) {
                holder.tvStatusBadge.setText(context.getString(R.string.promocode_status_stream_burning, (int) remainingHours));
            } else {
                holder.tvStatusBadge.setText(R.string.promocode_status_stream);
            }
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.rare_legendary));
            holder.cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_gold_bg)));
            holder.cardStatusBadge.setVisibility(View.VISIBLE);
        } else if ("NEW".equalsIgnoreCase(status) || code.isNew()) {
            holder.tvStatusBadge.setText(R.string.promocode_status_new);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.checked));
            holder.cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.badge_green_bg)));
            holder.cardStatusBadge.setVisibility(View.VISIBLE);
        } else if ("PERMANENT".equalsIgnoreCase(status)) {
            holder.tvStatusBadge.setText(R.string.promocode_status_permanent);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            holder.cardStatusBadge.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.pull_pity_badge_bg)));
            holder.cardStatusBadge.setVisibility(View.VISIBLE);
        } else {
            holder.cardStatusBadge.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PromocodeViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView cardPromocode;
        final TextView tvCode;
        final MaterialCardView cardExpiredBadge;
        final MaterialCardView cardStatusBadge;
        final TextView tvStatusBadge;
        final MaterialCardView cardRedeemedBadge;
        final ImageButton btnPromocodeOptions;
        final ImageView ivRewardIcon;
        final TextView tvRewardsDescription;
        final MaterialButton btnCopy;
        final MaterialButton btnActivate;

        PromocodeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardPromocode = itemView.findViewById(R.id.card_promocode);
            tvCode = itemView.findViewById(R.id.tv_code);
            cardExpiredBadge = itemView.findViewById(R.id.card_expired_badge);
            cardStatusBadge = itemView.findViewById(R.id.card_status_badge);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            cardRedeemedBadge = itemView.findViewById(R.id.card_redeemed_badge);
            btnPromocodeOptions = itemView.findViewById(R.id.btn_promocode_options);
            ivRewardIcon = itemView.findViewById(R.id.iv_reward_icon);
            tvRewardsDescription = itemView.findViewById(R.id.tv_rewards_description);
            btnCopy = itemView.findViewById(R.id.btn_copy);
            btnActivate = itemView.findViewById(R.id.btn_activate);
        }
    }
}
