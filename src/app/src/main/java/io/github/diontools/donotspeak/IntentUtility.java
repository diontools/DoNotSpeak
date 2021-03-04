package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.Date;

final class IntentUtility {
    private static final String TAG = "IntentUtility";

    static void start(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_START)
        );
    }

    static void stop(Context context, Date disableTime) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_STOP)
                        .putExtra(DNSService.DISABLE_TIME_NAME, disableTime.getTime())
        );
    }

    static void stopUntilScreenOff(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_STOP_UNTIL_SCREEN_OFF)
        );
    }

    static void shutdown(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_SHUTDOWN)
        );
    }

    static void switching(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_SWITCH)
        );
    }

    static void applySettings(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_APPLY_SETTINGS)
        );
    }

    @RequiresApi(Build.VERSION_CODES.N)
    static void setTileState(boolean enabled, boolean stopUntilScreenOff, String disableTimeString) {
        Log.d(TAG, "setTileState " + enabled + " " + stopUntilScreenOff + " " + disableTimeString);
        DNSTileService.enabled = enabled;
        DNSTileService.stopUntilScreenOff = stopUntilScreenOff;
        DNSTileService.disableTimeString = disableTimeString;
        DNSTileService.requestUpdate();
    }
}
