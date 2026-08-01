package ru.menshovanton.gachapoint;

public class Statistic {
    public int missedGemsCount;
    public int claimGemsCount;
    public int laterGemsCount;
    public int missedWishesCount;
    public int claimWishesCount;
    public int laterWishesCount;

    public Statistic(int missedGemsCount, int claimGemsCount, int laterGemsCount, int missedWishesCount, int claimWishesCount, int laterWishesCount) {
        this.missedGemsCount = missedGemsCount;
        this.claimGemsCount = claimGemsCount;
        this.laterGemsCount = laterGemsCount;
        this.missedWishesCount = missedWishesCount;
        this.claimWishesCount = claimWishesCount;
        this.laterWishesCount = laterWishesCount;
    }
}
