package ru.menshovanton.gachapoint.domain.models;

import ru.menshovanton.gachapoint.domain.enums.PityStatus;

public class Pull {
    private final int id;
    private final String dropRare;
    private final String dropType;
    private final String dateTime;
    private final String bannerType;
    private int pityNumber;
    private int pity4StarNumber;
    private boolean isResetPity;
    private PityStatus pityStatus;

    public Pull(int id, String dropRare, String dropType, String dateTime, String bannerType, boolean isResetPity, PityStatus pityStatus) {
        this.id = id;
        this.dropRare = dropRare;
        this.dropType = dropType;
        this.dateTime = dateTime;
        this.bannerType = bannerType;
        this.isResetPity = isResetPity;
        this.pityStatus = pityStatus != null ? pityStatus : PityStatus.NONE;
    }

    public Pull(int id, String dropRare, String dropType, String dateTime, String bannerType, boolean isResetPity) {
        this(id, dropRare, dropType, dateTime, bannerType, isResetPity, PityStatus.NONE);
    }

    public int getId() { return id; }
    public String getDropRare() { return dropRare; }
    public String getDropType() { return dropType; }
    public String getDateTime() { return dateTime; }
    public String getBannerType() { return bannerType; }
    public int getPityNumber() { return pityNumber; }
    public void setPityNumber(int pityNumber) { this.pityNumber = pityNumber; }
    public int getPity4StarNumber() { return pity4StarNumber; }
    public void setPity4StarNumber(int pity4StarNumber) { this.pity4StarNumber = pity4StarNumber; }
    public boolean isResetPity() { return isResetPity; }
    public void setResetPity(boolean resetPity) { isResetPity = resetPity; }
    public PityStatus getPityStatus() { return pityStatus; }
    public void setPityStatus(PityStatus pityStatus) { this.pityStatus = pityStatus; }
}