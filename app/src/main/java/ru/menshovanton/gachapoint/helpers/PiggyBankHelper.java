package ru.menshovanton.gachapoint.helpers;

import ru.menshovanton.gachapoint.activities.MainActivity;

public class PiggyBankHelper {
    PreferencesHelper preferencesHelper;

    public int progress;
    public int target;

    public PiggyBankHelper(MainActivity mainActivity) {
        preferencesHelper = new PreferencesHelper(mainActivity);
        progress = getProgress();
        target = getTarget();
    }

    private String getProgressPrefTag() {
        String tag = PreferencesHelper.PIGGY_BANK_PROGRESS_GENSHIN;
        switch (MainActivity.subType) {
            case 0:
                tag = PreferencesHelper.PIGGY_BANK_PROGRESS_GENSHIN;
                break;
            case 1:
                tag = PreferencesHelper.PIGGY_BANK_PROGRESS_HSR;
                break;
            case 2:
                tag = PreferencesHelper.PIGGY_BANK_PROGRESS_ZZZ;
                break;
        }
        return tag;
    }

    private String getTargetPrefTag() {
        String tag = PreferencesHelper.PIGGY_BANK_TARGET_GENSHIN;
        switch (MainActivity.subType) {
            case 0:
                tag = PreferencesHelper.PIGGY_BANK_TARGET_GENSHIN;
                break;
            case 1:
                tag = PreferencesHelper.PIGGY_BANK_TARGET_HSR;
                break;
            case 2:
                tag = PreferencesHelper.PIGGY_BANK_TARGET_ZZZ;
                break;
        }
        return tag;
    }

    public boolean pushProgress(int i) {
        if (progress + i <= target) {
            progress += i;
            preferencesHelper.saveIntPreference(getProgressPrefTag(), progress);
            return true;
        } else {
            return false;
        }
    }

    public int getProgress() {
        return preferencesHelper.getIntPreference(getProgressPrefTag());
    }

    public int getTarget() {
        return preferencesHelper.getIntPreference(getTargetPrefTag());
    }

    public void pushTarget(int target) {
        this.target += target;
        preferencesHelper.saveIntPreference(getTargetPrefTag(), this.target);
    }

    public void reset() {
        target = 0;
        progress = 0;
        preferencesHelper.saveIntPreference(getTargetPrefTag(), this.target);
        preferencesHelper.saveIntPreference(getProgressPrefTag(), this.target);
    }
}
