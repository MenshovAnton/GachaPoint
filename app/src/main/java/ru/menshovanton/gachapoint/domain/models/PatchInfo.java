package ru.menshovanton.gachapoint.domain.models;

import androidx.annotation.NonNull;

public class PatchInfo {
    private final String version;
    private final String title;
    private final String startDate;
    private final String endDate;

    public PatchInfo(@NonNull String version, @NonNull String title, @NonNull String startDate, @NonNull String endDate) {
        this.version = version;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @NonNull
    public String getVersion() { return version; }
    @NonNull
    public String getTitle() { return title; }
    @NonNull
    public String getStartDate() { return startDate; }
    @NonNull
    public String getEndDate() { return endDate; }
}
