package ru.menshovanton.gachapoint.ui.fragment.journal;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.domain.models.CatalogItem;

public class CatalogAutoCompleteAdapter extends ArrayAdapter<CatalogItem> {

    private final List<CatalogItem> allItems;
    private List<CatalogItem> filteredItems;

    public CatalogAutoCompleteAdapter(@NonNull Context context, @NonNull List<CatalogItem> items) {
        super(context, 0, items);
        this.allItems = new ArrayList<>(items);
        this.filteredItems = new ArrayList<>(items);
    }

    @Override
    public int getCount() {
        return filteredItems.size();
    }

    @Nullable
    @Override
    public CatalogItem getItem(int position) {
        if (position >= 0 && position < filteredItems.size()) {
            return filteredItems.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_catalog_autocomplete, parent, false);
        }

        CatalogItem item = getItem(position);
        if (item == null) return convertView;

        TextView tvName = convertView.findViewById(R.id.tv_item_name);
        TextView tvType = convertView.findViewById(R.id.tv_item_type);
        TextView tvRarity = convertView.findViewById(R.id.tv_item_rarity);
        MaterialCardView cardAvatar = convertView.findViewById(R.id.card_item_avatar);
        ImageView ivAvatar = convertView.findViewById(R.id.iv_item_avatar);
        TextView tvPool = convertView.findViewById(R.id.tv_item_pool_badge);

        String displayName = item.getName(getContext());
        tvName.setText(displayName);
        tvType.setText("WEAPON".equalsIgnoreCase(item.getType()) ? R.string.drop_type_weapon : R.string.drop_type_character);
        tvRarity.setText(item.getRarity());

        if (cardAvatar != null) {
            if (item.isFiveStar()) {
                cardAvatar.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.badge_gold_bg)));
                cardAvatar.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.rare_legendary)));
                tvRarity.setTextColor(ContextCompat.getColor(getContext(), R.color.rare_legendary));
            } else if (item.isFourStar()) {
                cardAvatar.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.badge_purple_bg)));
                cardAvatar.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.rare_epic)));
                tvRarity.setTextColor(ContextCompat.getColor(getContext(), R.color.rare_epic));
            } else {
                cardAvatar.setCardBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.pull_pity_badge_bg)));
                cardAvatar.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.pull_row_stroke)));
                tvRarity.setTextColor(ContextCompat.getColor(getContext(), R.color.text_secondary));
            }
        }

        if (ivAvatar != null) {
            if (item.getIconUrl() != null && !item.getIconUrl().isEmpty()) {
                ivAvatar.setVisibility(View.VISIBLE);
                tvRarity.setVisibility(View.GONE);
                Glide.with(getContext())
                        .load(item.getIconUrl())
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageDrawable(null);
                ivAvatar.setVisibility(View.GONE);
                tvRarity.setVisibility(View.VISIBLE);
            }
        }

        if (item.isFiveStar()) {
            boolean isRu = getContext().getString(R.string.genshin).contains("Геншин") || "ru".equals(Locale.getDefault().getLanguage());
            tvPool.setText(item.isStandard() ? (isRu ? "Стандарт" : "Standard") : (isRu ? "Ивент" : "Event"));
            tvPool.setVisibility(View.VISIBLE);
        } else {
            tvPool.setVisibility(View.GONE);
        }

        return convertView;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    results.values = allItems;
                    results.count = allItems.size();
                } else {
                    String query = constraint.toString().toLowerCase(Locale.ROOT).trim();
                    List<CatalogItem> match = new ArrayList<>();
                    for (CatalogItem it : allItems) {
                        if (it.getName().toLowerCase(Locale.ROOT).contains(query)
                                || it.getNameRu().toLowerCase(Locale.ROOT).contains(query)
                                || it.getNameEn().toLowerCase(Locale.ROOT).contains(query)) {
                            match.add(it);
                        }
                    }
                    results.values = match;
                    results.count = match.size();
                }
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.values != null) {
                    filteredItems = (List<CatalogItem>) results.values;
                } else {
                    filteredItems = Collections.emptyList();
                }
                notifyDataSetChanged();
            }

            @Override
            public CharSequence convertResultToString(Object resultValue) {
                if (resultValue instanceof CatalogItem) {
                    return ((CatalogItem) resultValue).getName(getContext());
                }
                return super.convertResultToString(resultValue);
            }
        };
    }
}
