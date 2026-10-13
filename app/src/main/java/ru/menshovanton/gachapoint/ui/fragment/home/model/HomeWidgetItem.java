package ru.menshovanton.gachapoint.ui.fragment.home.model;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ru.menshovanton.gachapoint.R;

public class HomeWidgetItem {

    public enum Type {
        QUICK_ACTIONS("quick_actions", R.string.widget_quick_actions, R.drawable.icon_daily_task),
        SAVINGS("savings", R.string.widget_savings, R.drawable.icon_piggy_bank),
        STATISTICS("statistics", R.string.widget_statistics, R.drawable.icon_statistics),
        PITY("pity", R.string.widget_pity, R.drawable.icon_journal);

        public final String id;
        @StringRes public final int titleResId;
        @DrawableRes public final int iconResId;

        Type(String id, int titleResId, int iconResId) {
            this.id = id;
            this.titleResId = titleResId;
            this.iconResId = iconResId;
        }

        public static Type fromId(String id) {
            if (id == null) return null;
            for (Type t : values()) {
                if (t.id.equalsIgnoreCase(id)) return t;
            }
            return null;
        }
    }

    public final Type type;
    public boolean isVisible;

    public HomeWidgetItem(Type type, boolean isVisible) {
        this.type = type;
        this.isVisible = isVisible;
    }

    public static List<HomeWidgetItem> parseConfig(String config) {
        List<HomeWidgetItem> list = new ArrayList<>();
        Set<Type> seen = new HashSet<>();
        if (config != null && !config.trim().isEmpty()) {
            String[] parts = config.split(",");
            for (String part : parts) {
                String[] pair = part.trim().split(":");
                if (pair.length == 2) {
                    Type t = Type.fromId(pair[0].trim());
                    if (t != null && !seen.contains(t)) {
                        boolean visible = Boolean.parseBoolean(pair[1].trim());
                        list.add(new HomeWidgetItem(t, visible));
                        seen.add(t);
                    }
                }
            }
        }
        for (Type t : Type.values()) {
            if (!seen.contains(t)) {
                list.add(new HomeWidgetItem(t, true));
            }
        }
        return list;
    }

    public static String toConfigString(List<HomeWidgetItem> items) {
        if (items == null || items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            HomeWidgetItem item = items.get(i);
            sb.append(item.type.id).append(":").append(item.isVisible);
            if (i < items.size() - 1) sb.append(",");
        }
        return sb.toString();
    }
}
