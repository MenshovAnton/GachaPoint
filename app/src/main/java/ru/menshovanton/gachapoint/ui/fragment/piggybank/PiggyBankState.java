package ru.menshovanton.gachapoint.ui.fragment.piggybank;

public class PiggyBankState {
    private final int progress;
    private final int target;
    private final int remainderGems;
    private final int totalGems;

    public PiggyBankState(int progress, int target) {
        this(progress, target, 0, progress * 160);
    }

    public PiggyBankState(int progress, int target, int remainderGems, int totalGems) {
        this.progress = progress;
        this.target = target;
        this.remainderGems = remainderGems;
        this.totalGems = totalGems;
    }

    public int getProgress() { return progress; }
    public int getTarget() { return target; }
    public int getRemainderGems() { return remainderGems; }
    public int getTotalGems() { return totalGems; }
}
