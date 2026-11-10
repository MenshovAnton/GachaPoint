package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public class SupportersData {

    private List<SupporterItem> legendary;
    private List<SupporterItem> epic;
    private List<SupporterItem> supporter;

    public SupportersData() {}

    public SupportersData(
            List<SupporterItem> legendary,
            List<SupporterItem> epic,
            List<SupporterItem> supporter
    ) {
        this.legendary = legendary;
        this.epic = epic;
        this.supporter = supporter;
    }

    @NonNull
    public List<SupporterItem> getLegendary() {
        return legendary != null ? legendary : Collections.emptyList();
    }

    @NonNull
    public List<SupporterItem> getEpic() {
        return epic != null ? epic : Collections.emptyList();
    }

    @NonNull
    public List<SupporterItem> getSupporter() {
        return supporter != null ? supporter : Collections.emptyList();
    }

    public boolean isEmpty() {
        return getLegendary().isEmpty() && getEpic().isEmpty() && getSupporter().isEmpty();
    }
}
