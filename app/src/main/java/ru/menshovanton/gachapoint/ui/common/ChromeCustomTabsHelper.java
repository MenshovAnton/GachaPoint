package ru.menshovanton.gachapoint.ui.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import ru.menshovanton.gachapoint.R;
import ru.menshovanton.gachapoint.data.local.AppLogger;

public final class ChromeCustomTabsHelper {

    private ChromeCustomTabsHelper() {}

    public static void openUrl(@NonNull Context context, @NonNull String url) {
        if (url.trim().isEmpty()) return;
        Uri uri = Uri.parse(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            AppLogger.w("ChromeCustomTabsHelper", "Blocked navigation to invalid or unsupported scheme: " + url);
            return;
        }
        try {
            CustomTabColorSchemeParams colorSchemeParams = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(context, R.color.accent))
                    .build();

            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setDefaultColorSchemeParams(colorSchemeParams)
                    .build();

            if (!(context instanceof Activity)) {
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
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
