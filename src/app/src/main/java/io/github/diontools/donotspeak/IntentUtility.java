package io.github.diontools.donotspeak;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

final class IntentUtility {
    static void start(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_START)
        );
    }

    static void stop(Context context, int disableTime) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_STOP)
                        .putExtra(DNSService.DISABLE_TIME_NAME, disableTime)
        );
    }

    static void stopUntilScreenOff(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_STOP_UNTIL_SCREEN_OFF)
        );
    }

    static void requestStateFromTile(Context context) {
        Compat.startForegroundService(
                context,
                new Intent(context, DNSService.class)
                        .setAction(DNSService.ACTION_REQUEST_STATE_FROM_TILE)
        );
    }

    @RequiresApi(Build.VERSION_CODES.N)
    static void responseStateToTile(Context context, boolean enabled, boolean stopUntilScreenOff, String disableTimeString) {
        context.startService(
                new Intent(context, DNSTileService.class)
                        .setAction(DNSTileService.ACTION_RESPONSE_STATE)
                        .putExtra(DNSTileService.RESPONSE_STATE_EXTRA_ENABLED, enabled)
                        .putExtra(DNSTileService.RESPONSE_STATE_EXTRA_STOP_UNTIL_SCREEN_OFF, stopUntilScreenOff)
                        .putExtra(DNSTileService.RESPONSE_STATE_EXTRA_DISABLE_TIME, disableTimeString)
        );
    }
}
