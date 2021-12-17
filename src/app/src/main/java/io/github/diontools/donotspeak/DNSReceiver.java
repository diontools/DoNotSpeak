package io.github.diontools.donotspeak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public final class DNSReceiver extends BroadcastReceiver {
    private static final String TAG = "DNSReceiver";

    public static final  String ACTION_REBOOT = BuildConfig.APPLICATION_ID + ".REBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent != null ? intent.toString() : "null");
        String action = intent != null ? intent.getAction() : null;
        if (action == null) return;

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED: {
                Log.d(TAG, "ACTION_BOOT_COMPLETED");
                IntentUtility.start(context);
                break;
            }
            case Intent.ACTION_MY_PACKAGE_REPLACED: {
                Log.d(TAG, "ACTION_MY_PACKAGE_REPLACED");
                IntentUtility.reboot(context);
                break;
            }
            case ACTION_REBOOT: {
                Log.d(TAG, "ACTION_REBOOT");
                IntentUtility.reboot(context);
                break;
            }
        }
    }
}
