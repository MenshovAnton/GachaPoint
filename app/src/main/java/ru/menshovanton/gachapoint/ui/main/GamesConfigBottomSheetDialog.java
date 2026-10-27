package ru.menshovanton.gachapoint.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.Preferences;
import ru.menshovanton.gachapoint.domain.enums.GameType;

public class GamesConfigBottomSheetDialog {

    public static void show(@NonNull Context context, @Nullable Runnable onSaveOrDismiss) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ThemeOverlay_GachaPoint_BottomSheetDialog);
        @SuppressLint("InflateParams") View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_games_config_sheet, null);
        dialog.setContentView(sheetView);

        Preferences preferences = new Preferences(context);
        RecyclerView rv = sheetView.findViewById(R.id.rv_games_config);
        rv.setLayoutManager(new LinearLayoutManager(context));

        List<GameType> allGames = Arrays.asList(GameType.values());
        List<GameType> enabledGames = new ArrayList<>(preferences.getEnabledGames());
        final GameType[] defaultGame = new GameType[]{preferences.getDefaultGame()};

        GamesAdapter adapter = new GamesAdapter(context, allGames, enabledGames, defaultGame[0], new GamesAdapter.Listener() {
            @Override
            public void onGameToggled(GameType game, boolean isChecked, MaterialSwitch switchView) {
                if (!isChecked && enabledGames.size() <= 1) {
                    switchView.setChecked(true);
                    Toast.makeText(context, R.string.games_error_last_one, Toast.LENGTH_SHORT).show();
                    return;
                }

                triggerVibration(context, preferences);
                if (isChecked) {
                    if (!enabledGames.contains(game)) {
                        enabledGames.add(game);
                    }
                } else {
                    enabledGames.remove(game);
                    if (defaultGame[0] == game) {
                        defaultGame[0] = enabledGames.get(0);
                        preferences.setDefaultGame(defaultGame[0]);
                    }
                }

                preferences.setEnabledGames(enabledGames);
                if (onSaveOrDismiss != null) {
                    onSaveOrDismiss.run();
                }
            }

            @Override
            public void onDefaultGameSelected(GameType game) {
                triggerVibration(context, preferences);
                defaultGame[0] = game;
                if (!enabledGames.contains(game)) {
                    enabledGames.add(game);
                    preferences.setEnabledGames(enabledGames);
                }
                preferences.setDefaultGame(game);
                if (onSaveOrDismiss != null) {
                    onSaveOrDismiss.run();
                }
            }
        });

        rv.setAdapter(adapter);

        sheetView.findViewById(R.id.btn_done_games).setOnClickListener(v -> {
            triggerVibration(context, preferences);
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> {
            if (onSaveOrDismiss != null) {
                onSaveOrDismiss.run();
            }
        });

        dialog.show();
    }

    private static void triggerVibration(Context context, Preferences preferences) {
        if (!preferences.getBooleanPreference(Preferences.VIBRATION_MODE)) return;

        Vibrator vibrator;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = vibratorManager != null ? vibratorManager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private static class GamesAdapter extends RecyclerView.Adapter<GamesAdapter.GameViewHolder> {

        interface Listener {
            void onGameToggled(GameType game, boolean isChecked, MaterialSwitch switchView);
            void onDefaultGameSelected(GameType game);
        }

        private final Context context;
        private final List<GameType> games;
        private final List<GameType> enabledGames;
        private GameType defaultGame;
        private final Listener listener;

        GamesAdapter(Context context, List<GameType> games, List<GameType> enabledGames, GameType defaultGame, Listener listener) {
            this.context = context;
            this.games = games;
            this.enabledGames = enabledGames;
            this.defaultGame = defaultGame;
            this.listener = listener;
        }

        @NonNull
        @Override
        public GameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_game_config, parent, false);
            return new GameViewHolder(view);
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onBindViewHolder(@NonNull GameViewHolder holder, int position) {
            GameType game = games.get(position);
            holder.ivGameIcon.setImageResource(game.getCurrencyIconResId());
            holder.tvGameTitle.setText(context.getString(game.getNameResId()));

            boolean isEnabled = enabledGames.contains(game);
            boolean isDefault = (defaultGame == game);

            holder.switchGameEnabled.setOnCheckedChangeListener(null);
            holder.switchGameEnabled.setChecked(isEnabled);
            holder.switchGameEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
                if (btn.isPressed()) {
                    listener.onGameToggled(game, isChecked, holder.switchGameEnabled);
                    notifyDataSetChanged();
                }
            });

            holder.tvDefaultBadge.setVisibility(isDefault ? View.VISIBLE : View.GONE);

            int starTint = ContextCompat.getColor(context, isDefault ? R.color.check : R.color.text_secondary);
            holder.btnDefaultStar.setColorFilter(starTint);

            holder.btnDefaultStar.setOnClickListener(v -> {
                defaultGame = game;
                listener.onDefaultGameSelected(game);
                notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return games.size();
        }

        static class GameViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivGameIcon;
            final TextView tvGameTitle;
            final TextView tvDefaultBadge;
            final ImageButton btnDefaultStar;
            final MaterialSwitch switchGameEnabled;

            GameViewHolder(@NonNull View itemView) {
                super(itemView);
                ivGameIcon = itemView.findViewById(R.id.iv_game_icon);
                tvGameTitle = itemView.findViewById(R.id.tv_game_title);
                tvDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
                btnDefaultStar = itemView.findViewById(R.id.btn_default_star);
                switchGameEnabled = itemView.findViewById(R.id.switch_game_enabled);
            }
        }
    }
}
