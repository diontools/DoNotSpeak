package io.github.diontools.donotspeak;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Insets;
import android.graphics.Rect;
import android.media.AudioManager;
import android.os.Build;
import android.os.PowerManager;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;

final class Compat {
    static void startForegroundService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static <T> T getSystemService(Context context, Class<T> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemService(serviceClass);
        }
        String serviceName = getSystemServiceName(context, serviceClass);
        //noinspection unchecked
        return serviceName != null ? (T) context.getSystemService(serviceName) : null;
    }

    static String getSystemServiceName(Context context, Class<?> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemServiceName(serviceClass);
        }

        if (serviceClass == AudioManager.class) return Context.AUDIO_SERVICE;
        if (serviceClass == NotificationManager.class) return Context.NOTIFICATION_SERVICE;
        if (serviceClass == AlarmManager.class) return Context.ALARM_SERVICE;
        if (serviceClass == KeyguardManager.class) return Context.KEYGUARD_SERVICE;
        if (serviceClass == PowerManager.class) return Context.POWER_SERVICE;
        throw new Resources.NotFoundException();
    }

    static Notification.Builder createNotificationBuilder(Context context, String id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(context, id);
        }
        return new Notification.Builder(context);
    }

    /**
     * Keeps content clear of the system bars. targetSdk 36 forces edge-to-edge on Android 15+
     * (windowOptOutEdgeToEdgeEnforcement no longer works on Android 16), and androidx.core is
     * deliberately not a dependency, so the insets are applied by hand.
     *
     * The bottom inset goes to {@code scrollingView} with clipToPadding disabled, so its content
     * scrolls under the navigation bar instead of stopping above it. What arrives at the top is
     * the action bar height added to the status bar height, because the decor passes its own
     * content inset down rather than laying the content out below the action bar itself.
     */
    static void applySystemBarInsets(final View root, final ViewGroup scrollingView) {
        scrollingView.setClipToPadding(false);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            Rect bars = new Rect();
            getSystemBarInsets(insets, bars);
            v.setPadding(bars.left, bars.top, bars.right, 0);
            scrollingView.setPadding(
                    scrollingView.getPaddingLeft(),
                    scrollingView.getPaddingTop(),
                    scrollingView.getPaddingRight(),
                    bars.bottom);
            return insets;
        });
        root.requestApplyInsets();
    }

    @SuppressWarnings("deprecation")
    private static void getSystemBarInsets(WindowInsets insets, Rect out) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getSystemBarInsetsApi30(insets, out);
            return;
        }
        out.set(
                insets.getSystemWindowInsetLeft(),
                insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),
                insets.getSystemWindowInsetBottom());
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private static void getSystemBarInsetsApi30(WindowInsets insets, Rect out) {
        Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
        out.set(bars.left, bars.top, bars.right, bars.bottom);
    }

    static void startActivityAndCollapse(TileService tileService, Intent intent, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            tileService.startActivityAndCollapse(
                    PendingIntent.getActivity(
                            tileService.getApplicationContext(),
                            requestCode,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE
                    )
            );
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            startActivityAndCollapseApi24(tileService, intent);
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @SuppressWarnings("deprecation")
    @RequiresApi(Build.VERSION_CODES.N)
    private static void startActivityAndCollapseApi24(TileService tileService, Intent intent) {
        tileService.startActivityAndCollapse(intent);
    }
}
