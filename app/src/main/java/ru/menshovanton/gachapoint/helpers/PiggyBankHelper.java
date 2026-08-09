package ru.menshovanton.gachapoint.helpers;

import ru.menshovanton.gachapoint.activities.MainActivity;

public class PiggyBankHelper {
    private final MainActivity mainActivity;

    private final PreferencesHelper preferencesHelper;

    private int manualProgress;
    private int subsProgress;
    private int target;

    public PiggyBankHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        preferencesHelper = new PreferencesHelper(mainActivity);
        manualProgress = preferencesHelper.getIntPreference(getManualProgressPrefTag());
        subsProgress = preferencesHelper.getIntPreference(getSubsProgressPrefTag());
        target = getTarget();
    }

    private String getManualProgressPrefTag() {
        String tag = PreferencesHelper.PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
        switch (mainActivity.getSubType()) {
            case 0:
                tag = PreferencesHelper.PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
                break;
            case 1:
                tag = PreferencesHelper.PIGGY_BANK_MANUAL_PROGRESS_HSR;
                break;
            case 2:
                tag = PreferencesHelper.PIGGY_BANK_MANUAL_PROGRESS_ZZZ;
                break;
        }
        return tag;
    }

    private String getSubsProgressPrefTag() {
        String tag = PreferencesHelper.PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
        switch (mainActivity.getSubType()) {
            case 0:
                tag = PreferencesHelper.PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
                break;
            case 1:
                tag = PreferencesHelper.PIGGY_BANK_SUBS_PROGRESS_HSR;
                break;
            case 2:
                tag = PreferencesHelper.PIGGY_BANK_SUBS_PROGRESS_ZZZ;
                break;
        }
        return tag;
    }

    private String getTargetPrefTag() {
        String tag = PreferencesHelper.PIGGY_BANK_TARGET_GENSHIN;
        switch (mainActivity.getSubType()) {
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

    public boolean pushManualProgress(int wishes) {
        if (manualProgress + subsProgress + wishes <= target) {
            manualProgress += wishes;
            preferencesHelper.saveIntPreference(getManualProgressPrefTag(), manualProgress);
            return true;
        } else {
            return false;
        }
    }

    public void updateSubsProgress(int wishes) {
        if (manualProgress + subsProgress + wishes <= target) {
            subsProgress = wishes;
            preferencesHelper.saveIntPreference(getSubsProgressPrefTag(), subsProgress);
        }
    }

    public int getProgress() {
        return preferencesHelper.getIntPreference(getManualProgressPrefTag()) + preferencesHelper.getIntPreference(getSubsProgressPrefTag());
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
        manualProgress = 0;
        preferencesHelper.saveIntPreference(getTargetPrefTag(), 0);
        preferencesHelper.saveIntPreference(getManualProgressPrefTag(), 0);
    }
}
