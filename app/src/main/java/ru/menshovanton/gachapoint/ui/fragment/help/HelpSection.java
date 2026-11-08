package ru.menshovanton.gachapoint.ui.fragment.help;

public class HelpSection {

    private final int titleResId;
    private final int contentResId;
    private final int iconResId;
    private boolean isExpanded;

    public HelpSection(int titleResId, int contentResId, int iconResId) {
        this.titleResId = titleResId;
        this.contentResId = contentResId;
        this.iconResId = iconResId;
        this.isExpanded = false;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getContentResId() {
        return contentResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
