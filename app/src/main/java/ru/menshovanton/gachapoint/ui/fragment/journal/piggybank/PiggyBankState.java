package ru.menshovanton.gachapoint.ui.fragment.journal.piggybank;

public class PiggyBankState {
    private final int progress;
    private final int target;

    public PiggyBankState(int progress, int target) {
        this.progress = progress;
        this.target = target;
    }

    public int getProgress() { return progress; }
    public int getTarget() { return target; }
}