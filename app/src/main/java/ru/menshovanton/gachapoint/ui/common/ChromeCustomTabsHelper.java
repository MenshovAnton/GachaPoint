package ru.menshovanton.gachapoint.ui.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;

public final class ChromeCustomTabsHelper {

    private ChromeCustomTabsHelper() {}

    public static void openUrl(@NonNull Context context, @NonNull String url) {
        Uri uri = Uri.parse(url);
        try {
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setToolbarColor(ContextCompat.getColor(context, R.color.accent))
                    .build();

            customTabsIntent.intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            customTabsIntent.launchUrl(context, uri);
        } catch (Throwable t) {
            AppLogger.w("ChromeCustomTabsHelper", "Custom Tabs failed, falling back to ACTION_VIEW: " + t.getMessage());
            try {
                Intent fallback = new Intent(Intent.ACTION_VIEW, uri);
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallback);
            } catch (Exception e) {
                AppLogger.e("ChromeCustomTabsHelper", "Failed to launch browser", e);
            }
        }
    }
}
