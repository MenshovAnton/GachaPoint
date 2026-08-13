package ru.menshovanton.gachapoint.ui.journal.piggybank;

import ru.menshovanton.gachapoint.ui.main.MainActivity;
import ru.menshovanton.gachapoint.data.local.Preferences;

public class PiggyBankHelper {
    private final MainActivity mainActivity;

    private final Preferences preferences;

    private int manualProgress;
    private int subsProgress;
    private int target;

    public PiggyBankHelper(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        preferences = new Preferences(mainActivity);
        manualProgress = preferences.getIntPreference(getManualProgressPrefTag());
        subsProgress = preferences.getIntPreference(getSubsProgressPrefTag());
        target = getTarget();
    }

    private String getManualProgressPrefTag() {
        String tag = Preferences.PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
        switch (mainActivity.getSubType()) {
            case GENSHIN:
                tag = Preferences.PIGGY_BANK_MANUAL_PROGRESS_GENSHIN;
                break;
            case HSR:
                tag = Preferences.PIGGY_BANK_MANUAL_PROGRESS_HSR;
                break;
            case ZZZ:
                tag = Preferences.PIGGY_BANK_MANUAL_PROGRESS_ZZZ;
                break;
        }
        return tag;
    }

    private String getSubsProgressPrefTag() {
        String tag = Preferences.PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
        switch (mainActivity.getSubType()) {
            case GENSHIN:
                tag = Preferences.PIGGY_BANK_SUBS_PROGRESS_GENSHIN;
                break;
            case HSR:
                tag = Preferences.PIGGY_BANK_SUBS_PROGRESS_HSR;
                break;
            case ZZZ:
                tag = Preferences.PIGGY_BANK_SUBS_PROGRESS_ZZZ;
                break;
        }
        return tag;
    }

    private String getTargetPrefTag() {
        String tag = Preferences.PIGGY_BANK_TARGET_GENSHIN;
        switch (mainActivity.getSubType()) {
            case GENSHIN:
                tag = Preferences.PIGGY_BANK_TARGET_GENSHIN;
                break;
            case HSR:
                tag = Preferences.PIGGY_BANK_TARGET_HSR;
                break;
            case ZZZ:
                tag = Preferences.PIGGY_BANK_TARGET_ZZZ;
                break;
        }
        return tag;
    }

    public boolean pushManualProgress(int wishes) {
        if (manualProgress + subsProgress + wishes <= target) {
            manualProgress += wishes;
            preferences.saveIntPreference(getManualProgressPrefTag(), manualProgress);
            return true;
        } else {
            return false;
        }
    }

    public void updateSubsProgress(int wishes) {
        if (manualProgress + subsProgress + wishes <= target) {
            subsProgress = wishes;
            preferences.saveIntPreference(getSubsProgressPrefTag(), subsProgress);
        }
    }

    public int getProgress() {
        return preferences.getIntPreference(getManualProgressPrefTag()) + preferences.getIntPreference(getSubsProgressPrefTag());
    }

    public int getTarget() {
        return preferences.getIntPreference(getTargetPrefTag());
    }

    public void pushTarget(int target) {
        this.target += target;
        preferences.saveIntPreference(getTargetPrefTag(), this.target);
    }

    public void reset() {
        target = 0;
        manualProgress = 0;
        preferences.saveIntPreference(getTargetPrefTag(), 0);
        preferences.saveIntPreference(getManualProgressPrefTag(), 0);
    }
}
