package ru.menshovanton.gachapoint.models;

public class Statistic {
    public int missedGems;
    public int claimGems;
    public int laterGems;
    public int missedWishes;
    public int claimWishes;
    public int laterWishes;

    public Statistic(int missedGems, int claimGems, int laterGems, int missedWishes, int claimWishes, int laterWishes) {
        this.missedGems = missedGems;
        this.claimGems = claimGems;
        this.laterGems = laterGems;
        this.missedWishes = missedWishes;
        this.claimWishes = claimWishes;
        this.laterWishes = laterWishes;
    }
}
