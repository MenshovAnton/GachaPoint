package ru.menshovanton.gachapoint.models;

public class Wish {
    private final int id;
    private final String dropRare;
    private final String dropType;
    private final String dateTime;

    public Wish(int id, String dropRare, String dropType, String dateTime) {
        this.id = id;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.dateTime = dateTime;
    }

    public int getId() { return id; }
    public String getDateTime() { return dateTime; }
    public String getDropRare() { return dropRare; }
    public String getDropType() { return dropType; }
}
