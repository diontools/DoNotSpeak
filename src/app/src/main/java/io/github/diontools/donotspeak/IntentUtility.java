package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

final class IntentUtility {
    private static final String TAG = "IntentUtility";

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

    @RequiresApi(Build.VERSION_CODES.N)
    static void setTileState(boolean enabled, boolean stopUntilScreenOff, String disableTimeString) {
        Log.d(TAG, "setTileState " + enabled + " " + stopUntilScreenOff + " " + disableTimeString);
        DNSTileService.enabled = enabled;
        DNSTileService.stopUntilScreenOff = stopUntilScreenOff;
        DNSTileService.disableTimeString = disableTimeString;
        DNSTileService.requestUpdate();
    }
}
