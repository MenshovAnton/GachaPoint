package ru.menshovanton.gachapoint.ui.onboarding;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;
import ru.menshovanton.gachapoint.ui.animation.NumberCountAnimator;
import ru.menshovanton.gachapoint.ui.common.ChromeCustomTabsHelper;
import ru.menshovanton.gachapoint.ui.common.HapticHelper;
import ru.menshovanton.gachapoint.ui.main.MainActivityView;
import ru.menshovanton.gachapoint.worker.NotificationScheduler;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private ImageButton btnBack;
    private MaterialButton btnSkip;
    private MaterialButton btnNext;
    private View dot0;
    private View dot1;
    private View dot2;

    private final List<GameType> enabledGames = new ArrayList<>(Arrays.asList(GameType.values()));
    private GameType defaultGame = GameType.GENSHIN;
    private boolean notificationsEnabled = true;
    private int alarmHour = 12;
    private int alarmMinute = 0;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> finishOnboarding());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Preferences preferences = new Preferences(this);
        int themeMode = preferences.getIntPreference(Preferences.APP_THEME);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        String langCode = preferences.getStringPreference(Preferences.APP_LANGUAGE, "sys");
        if (langCode.equals("sys")) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode));
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_onboarding);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cl_onboarding), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewPager = findViewById(R.id.vp_onboarding);
        btnBack = findViewById(R.id.btn_back);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);
        dot0 = findViewById(R.id.dot_0);
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);

        OnboardingAdapter adapter = new OnboardingAdapter();
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);

        viewPager.setPageTransformer((page, position) -> {
            float absPos = Math.abs(position);
            page.setAlpha(1.0f - 0.35f * absPos);
            page.setScaleY(0.94f + 0.06f * (1.0f - absPos));
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateNavigationState(position);
            }
        });

        btnBack.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(this, v);
            int current = viewPager.getCurrentItem();
            if (current > 0) {
                viewPager.setCurrentItem(current - 1, true);
            }
        });

        btnSkip.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(this, v);
            int current = viewPager.getCurrentItem();
            if (current < 2) {
                viewPager.setCurrentItem(current + 1, true);
            }
        });

        btnNext.setOnClickListener(v -> {
            HapticHelper.performClickHaptic(this, v);
            int current = viewPager.getCurrentItem();
            if (current < 2) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                handleComplete();
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int current = viewPager.getCurrentItem();
                if (current > 0) {
                    viewPager.setCurrentItem(current - 1, true);
                } else {
                    finish();
                }
            }
        });

        updateNavigationState(0);
    }

    private void updateNavigationState(int position) {
        animateDot(dot0, position == 0);
        animateDot(dot1, position == 1);
        animateDot(dot2, position == 2);

        if (position == 0) {
            btnBack.setVisibility(View.INVISIBLE);
            btnSkip.setVisibility(View.GONE);
            btnNext.setText(R.string.onboarding_btn_start);
        } else if (position == 1) {
            btnBack.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.VISIBLE);
            btnNext.setText(R.string.onboarding_btn_next);
        } else {
            btnBack.setVisibility(View.VISIBLE);
            btnSkip.setVisibility(View.GONE);
            btnNext.setText(R.string.onboarding_btn_finish);
        }
    }

    private void animateDot(View dot, boolean isActive) {
        int targetWidth = (int) (isActive ? 22 * getResources().getDisplayMetrics().density : 8 * getResources().getDisplayMetrics().density);
        float targetAlpha = isActive ? 1.0f : 0.35f;

        ValueAnimator widthAnimator = ValueAnimator.ofInt(dot.getWidth(), targetWidth);
        widthAnimator.setDuration(220);
        widthAnimator.addUpdateListener(animation -> {
            ViewGroup.LayoutParams params = dot.getLayoutParams();
            params.width = (int) animation.getAnimatedValue();
            dot.setLayoutParams(params);
        });
        widthAnimator.start();

        dot.animate()
                .alpha(targetAlpha)
                .setDuration(220)
                .start();
    }

    private void handleComplete() {
        if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                return;
            }
        }
        finishOnboarding();
    }

    private void finishOnboarding() {
        Preferences preferences = new Preferences(this);
        preferences.setEnabledGames(enabledGames);
        preferences.setDefaultGame(defaultGame);
        preferences.saveBooleanPreference(Preferences.ALLOW_NOTIFICATIONS, notificationsEnabled);
        preferences.saveIntPreference(Preferences.ALARM_HOURS, alarmHour);
        preferences.saveIntPreference(Preferences.ALARM_MINUTES, alarmMinute);

        if (notificationsEnabled) {
            NotificationScheduler.scheduleDailyNotification(this);
        } else {
            NotificationScheduler.cancelDailyNotification(this);
        }

        preferences.setOnboardingCompleted(true);

        Intent intent = new Intent(this, MainActivityView.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private class OnboardingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @Override
        public int getItemCount() {
            return 3;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == 0) {
                View view = inflater.inflate(R.layout.item_onboarding_welcome, parent, false);
                return new WelcomeViewHolder(view);
            } else if (viewType == 1) {
                View view = inflater.inflate(R.layout.item_onboarding_games, parent, false);
                return new GamesViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_onboarding_notifications, parent, false);
                return new NotificationsViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof WelcomeViewHolder) {
                ((WelcomeViewHolder) holder).bind();
            } else if (holder instanceof GamesViewHolder) {
                ((GamesViewHolder) holder).bind();
            } else if (holder instanceof NotificationsViewHolder) {
                ((NotificationsViewHolder) holder).bind();
            }
        }
    }

    private class WelcomeViewHolder extends RecyclerView.ViewHolder {
        private final View cardTelegram;
        private final View cardTiktok;
        private final View cardGithub;

        WelcomeViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTelegram = itemView.findViewById(R.id.mcv_community_telegram);
            cardTiktok = itemView.findViewById(R.id.mcv_community_tiktok);
            cardGithub = itemView.findViewById(R.id.mcv_community_github);
        }

        void bind() {
            cardTelegram.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(itemView.getContext(), v);
                ChromeCustomTabsHelper.openUrl(itemView.getContext(), getString(R.string.telegram_url));
            });

            cardTiktok.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(itemView.getContext(), v);
                ChromeCustomTabsHelper.openUrl(itemView.getContext(), getString(R.string.tiktok_url));
            });

            cardGithub.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(itemView.getContext(), v);
                ChromeCustomTabsHelper.openUrl(itemView.getContext(), getString(R.string.github_url));
            });
        }
    }

    private class GamesViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardGenshin;
        private final MaterialCardView cardHsr;
        private final MaterialCardView cardZzz;
        private final MaterialCardView cardHi3;

        private final MaterialCardView mcvCheckGenshin;
        private final MaterialCardView mcvCheckHsr;
        private final MaterialCardView mcvCheckZzz;
        private final MaterialCardView mcvCheckHi3;

        private final ImageView ivCheckGenshin;
        private final ImageView ivCheckHsr;
        private final ImageView ivCheckZzz;
        private final ImageView ivCheckHi3;

        private final ImageButton btnStarGenshin;
        private final ImageButton btnStarHsr;
        private final ImageButton btnStarZzz;
        private final ImageButton btnStarHi3;

        private final TextView tvBadgeGenshin;
        private final TextView tvBadgeHsr;
        private final TextView tvBadgeZzz;
        private final TextView tvBadgeHi3;

        GamesViewHolder(@NonNull View itemView) {
            super(itemView);
            cardGenshin = itemView.findViewById(R.id.mcv_game_genshin);
            cardHsr = itemView.findViewById(R.id.mcv_game_hsr);
            cardZzz = itemView.findViewById(R.id.mcv_game_zzz);
            cardHi3 = itemView.findViewById(R.id.mcv_game_hi3);

            mcvCheckGenshin = itemView.findViewById(R.id.mcv_check_genshin);
            mcvCheckHsr = itemView.findViewById(R.id.mcv_check_hsr);
            mcvCheckZzz = itemView.findViewById(R.id.mcv_check_zzz);
            mcvCheckHi3 = itemView.findViewById(R.id.mcv_check_hi3);

            ivCheckGenshin = itemView.findViewById(R.id.iv_check_genshin);
            ivCheckHsr = itemView.findViewById(R.id.iv_check_hsr);
            ivCheckZzz = itemView.findViewById(R.id.iv_check_zzz);
            ivCheckHi3 = itemView.findViewById(R.id.iv_check_hi3);

            btnStarGenshin = itemView.findViewById(R.id.btn_star_genshin);
            btnStarHsr = itemView.findViewById(R.id.btn_star_hsr);
            btnStarZzz = itemView.findViewById(R.id.btn_star_zzz);
            btnStarHi3 = itemView.findViewById(R.id.btn_star_hi3);

            tvBadgeGenshin = itemView.findViewById(R.id.tv_default_badge_genshin);
            tvBadgeHsr = itemView.findViewById(R.id.tv_default_badge_hsr);
            tvBadgeZzz = itemView.findViewById(R.id.tv_default_badge_zzz);
            tvBadgeHi3 = itemView.findViewById(R.id.tv_default_badge_hi3);
        }

        void bind() {
            setupGameRow(GameType.GENSHIN, cardGenshin, mcvCheckGenshin, btnStarGenshin);
            setupGameRow(GameType.HSR, cardHsr, mcvCheckHsr, btnStarHsr);
            setupGameRow(GameType.ZZZ, cardZzz, mcvCheckZzz, btnStarZzz);
            setupGameRow(GameType.HI3, cardHi3, mcvCheckHi3, btnStarHi3);
            updateAllGamesUI();
        }

        private void setupGameRow(GameType game, MaterialCardView card, MaterialCardView mcvCheck, ImageButton btnStar) {
            card.setOnClickListener(v -> toggleGame(game));
            mcvCheck.setOnClickListener(v -> toggleGame(game));

            btnStar.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(itemView.getContext(), v);
                defaultGame = game;
                if (!enabledGames.contains(game)) {
                    enabledGames.add(game);
                }
                updateAllGamesUI();
            });
        }

        private void toggleGame(GameType game) {
            boolean isCurrentlyEnabled = enabledGames.contains(game);
            if (isCurrentlyEnabled && enabledGames.size() <= 1) {
                Toast.makeText(itemView.getContext(), R.string.onboarding_games_error_select, Toast.LENGTH_SHORT).show();
                return;
            }

            HapticHelper.performClickHaptic(itemView.getContext(), itemView);

            if (isCurrentlyEnabled) {
                enabledGames.remove(game);
                if (defaultGame == game && !enabledGames.isEmpty()) {
                    defaultGame = enabledGames.get(0);
                }
            } else {
                enabledGames.add(game);
            }
            updateAllGamesUI();
        }

        private void updateAllGamesUI() {
            updateSingleGame(GameType.GENSHIN, cardGenshin, mcvCheckGenshin, ivCheckGenshin, btnStarGenshin, tvBadgeGenshin);
            updateSingleGame(GameType.HSR, cardHsr, mcvCheckHsr, ivCheckHsr, btnStarHsr, tvBadgeHsr);
            updateSingleGame(GameType.ZZZ, cardZzz, mcvCheckZzz, ivCheckZzz, btnStarZzz, tvBadgeZzz);
            updateSingleGame(GameType.HI3, cardHi3, mcvCheckHi3, ivCheckHi3, btnStarHi3, tvBadgeHi3);
        }

        private void updateSingleGame(GameType game, MaterialCardView card, MaterialCardView mcvCheck, ImageView ivCheck, ImageButton btnStar, TextView tvBadge) {
            Context context = itemView.getContext();
            boolean isEnabled = enabledGames.contains(game);
            boolean isDefault = (defaultGame == game);

            card.setAlpha(isEnabled ? 1.0f : 0.55f);
            if (isEnabled) {
                card.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
                mcvCheck.setCardBackgroundColor(ContextCompat.getColor(context, R.color.check));
                mcvCheck.setStrokeWidth(0);
                ivCheck.setVisibility(View.VISIBLE);
            } else {
                card.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
                mcvCheck.setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent));
                mcvCheck.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
                mcvCheck.setStrokeWidth((int) (1.5f * context.getResources().getDisplayMetrics().density));
                ivCheck.setVisibility(View.INVISIBLE);
            }

            tvBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);
            int starTint = ContextCompat.getColor(context, isDefault ? R.color.check : R.color.text_secondary);
            btnStar.setColorFilter(starTint);
        }
    }

    private class NotificationsViewHolder extends RecyclerView.ViewHolder {
        private final MaterialSwitch smNotif;
        private final View llTimeSection;
        private final View mcvTimeDisplay;
        private final TextView tvSelectedHour;
        private final TextView tvSelectedMinute;

        private final MaterialCardView mcvPreset09;
        private final MaterialCardView mcvPreset12;
        private final MaterialCardView mcvPreset19;
        private final MaterialCardView mcvPreset21;

        private final TextView tvTime09;
        private final TextView tvPeriod09;
        private final TextView tvTime12;
        private final TextView tvPeriod12;
        private final TextView tvTime19;
        private final TextView tvPeriod19;
        private final TextView tvTime21;
        private final TextView tvPeriod21;

        NotificationsViewHolder(@NonNull View itemView) {
            super(itemView);
            smNotif = itemView.findViewById(R.id.sm_onboarding_notif);
            llTimeSection = itemView.findViewById(R.id.ll_time_section);
            mcvTimeDisplay = itemView.findViewById(R.id.mcv_time_display);
            tvSelectedHour = itemView.findViewById(R.id.tv_selected_hour);
            tvSelectedMinute = itemView.findViewById(R.id.tv_selected_minute);

            mcvPreset09 = itemView.findViewById(R.id.mcv_preset_09);
            mcvPreset12 = itemView.findViewById(R.id.mcv_preset_12);
            mcvPreset19 = itemView.findViewById(R.id.mcv_preset_19);
            mcvPreset21 = itemView.findViewById(R.id.mcv_preset_21);

            tvTime09 = itemView.findViewById(R.id.tv_preset_time_09);
            tvPeriod09 = itemView.findViewById(R.id.tv_preset_period_09);
            tvTime12 = itemView.findViewById(R.id.tv_preset_time_12);
            tvPeriod12 = itemView.findViewById(R.id.tv_preset_period_12);
            tvTime19 = itemView.findViewById(R.id.tv_preset_time_19);
            tvPeriod19 = itemView.findViewById(R.id.tv_preset_period_19);
            tvTime21 = itemView.findViewById(R.id.tv_preset_time_21);
            tvPeriod21 = itemView.findViewById(R.id.tv_preset_period_21);
        }

        void bind() {
            smNotif.setChecked(notificationsEnabled);
            llTimeSection.setVisibility(notificationsEnabled ? View.VISIBLE : View.GONE);

            smNotif.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    HapticHelper.performClickHaptic(itemView.getContext(), buttonView);
                    notificationsEnabled = isChecked;
                    llTimeSection.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                }
            });

            mcvTimeDisplay.setOnClickListener(v -> {
                HapticHelper.performClickHaptic(itemView.getContext(), v);
                showCustomTimePicker();
            });

            mcvPreset09.setOnClickListener(v -> setPresetTime(9, 0, v));
            mcvPreset12.setOnClickListener(v -> setPresetTime(12, 0, v));
            mcvPreset19.setOnClickListener(v -> setPresetTime(19, 0, v));
            mcvPreset21.setOnClickListener(v -> setPresetTime(21, 0, v));

            updateTimeDisplay();
        }

        private void setPresetTime(int h, int m, View v) {
            HapticHelper.performClickHaptic(itemView.getContext(), v);
            alarmHour = h;
            alarmMinute = m;
            updateTimeDisplay();
        }

        private void updateTimeDisplay() {
            NumberCountAnimator.animate(tvSelectedHour, alarmHour, val -> String.format(Locale.US, "%02d", val));
            NumberCountAnimator.animate(tvSelectedMinute, alarmMinute, val -> String.format(Locale.US, "%02d", val));

            applyPresetStyle(mcvPreset09, tvTime09, tvPeriod09, alarmHour == 9 && alarmMinute == 0);
            applyPresetStyle(mcvPreset12, tvTime12, tvPeriod12, alarmHour == 12 && alarmMinute == 0);
            applyPresetStyle(mcvPreset19, tvTime19, tvPeriod19, alarmHour == 19 && alarmMinute == 0);
            applyPresetStyle(mcvPreset21, tvTime21, tvPeriod21, alarmHour == 21 && alarmMinute == 0);
        }

        private void applyPresetStyle(MaterialCardView card, TextView tvTime, TextView tvPeriod, boolean isSelected) {
            Context context = itemView.getContext();
            if (isSelected) {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.accent));
                card.setStrokeWidth(0);
                tvTime.setTextColor(ContextCompat.getColor(context, R.color.white));
                tvPeriod.setTextColor(ContextCompat.getColor(context, R.color.white));
            } else {
                card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.pull_row_bg));
                card.setStrokeColor(ContextCompat.getColor(context, R.color.pull_row_stroke));
                card.setStrokeWidth(1);
                tvTime.setTextColor(ContextCompat.getColor(context, R.color.text));
                tvPeriod.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
            }
        }

        @SuppressLint({"InflateParams", "DefaultLocale"})
        private void showCustomTimePicker() {
            Context context = itemView.getContext();
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
            View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_time_picker_sheet, null);
            dialog.setContentView(sheetView);

            NumberPicker npHours = sheetView.findViewById(R.id.np_hours);
            NumberPicker npMinutes = sheetView.findViewById(R.id.np_minutes);
            Button btnSave = sheetView.findViewById(R.id.btn_save_time);

            npHours.setMinValue(0);
            npHours.setMaxValue(23);
            npHours.setFormatter(val -> String.format("%02d", val));

            npMinutes.setMinValue(0);
            npMinutes.setMaxValue(59);
            npMinutes.setFormatter(val -> String.format("%02d", val));

            npHours.setValue(alarmHour);
            npMinutes.setValue(alarmMinute);

            setupNumberPicker(npHours, context);
            setupNumberPicker(npMinutes, context);

            btnSave.setOnClickListener(v -> {
                alarmHour = npHours.getValue();
                alarmMinute = npMinutes.getValue();
                updateTimeDisplay();
                dialog.dismiss();
            });

            dialog.show();
        }

        private void setupNumberPicker(NumberPicker np, Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                np.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 24, context.getResources().getDisplayMetrics()));
                np.setTextColor(ContextCompat.getColor(context, R.color.text));
                np.setSelectionDividerHeight(0);
            } else {
                for (int i = 0; i < np.getChildCount(); i++) {
                    View child = np.getChildAt(i);
                    if (child instanceof EditText) {
                        ((EditText) child).setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
                        ((EditText) child).setTextColor(ContextCompat.getColor(context, R.color.text));
                    }
                }
            }
        }
    }
}