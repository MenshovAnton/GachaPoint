package ru.menshovanton.gachapoint.models;

public class Wish {
    private final int id;
    private final String dropRare;
    private final String dropType;
    private final String dateTime;
    private final String bannerType;
    private int pityNumber;

    public Wish(int id, String dropRare, String dropType, String dateTime, String bannerType) {
        this.id = id;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.dateTime = dateTime;
        this.bannerType = bannerType;
    }

    public int getId() { return id; }
    public String getDateTime() { return dateTime; }
    public String getDropRare() { return dropRare; }
    public String getDropType() { return dropType; }
    public String getBannerType() { return bannerType; }

    public int getPityNumber() { return pityNumber; }
    public void setPityNumber(int pityNumber) { this.pityNumber = pityNumber; }
}