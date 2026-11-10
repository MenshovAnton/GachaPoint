package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public class SupporterItem {

    private String name;
    private String comment;

    public SupporterItem() {}

    public SupporterItem(@NonNull String name, @Nullable String comment) {
        this.name = name;
        this.comment = comment;
    }

    @NonNull
    public String getName() {
        return name != null ? name : "";
    }

    @Nullable
    public String getComment() {
        return comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SupporterItem that = (SupporterItem) o;
        return Objects.equals(name, that.name) && Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, comment);
    }
}
