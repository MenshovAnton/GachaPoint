package ru.menshovanton.gachapoint.models;

public class Wish {
    private final int id;
    private final String dropRare;
    private final String dropType;
    private final String dateTime;
    private final String bannerType;
    private int pityNumber;
    private boolean isResetPity;

    public Wish(int id, String dropRare, String dropType, String dateTime, String bannerType, boolean isResetPity) {
        this.id = id;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.dateTime = dateTime;
        this.bannerType = bannerType;
        this.isResetPity = isResetPity;
    }

    public int getId() { return id; }
    public String getDropRare() { return dropRare; }
    public String getDropType() { return dropType; }
    public String getDateTime() { return dateTime; }
    public String getBannerType() { return bannerType; }
    public int getPityNumber() { return pityNumber; }
    public void setPityNumber(int pityNumber) { this.pityNumber = pityNumber; }
    public boolean isResetPity() { return isResetPity; }
    public void setResetPity(boolean resetPity) { isResetPity = resetPity; }
}