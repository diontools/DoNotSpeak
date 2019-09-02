package io.github.diontools.donotspeak;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public final class DNSReceiver extends BroadcastReceiver {
    private static final String TAG = "DNSReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        switch (action) {
            case AudioManager.ACTION_AUDIO_BECOMING_NOISY: {
                Log.d(TAG, "ACTION_AUDIO_BECOMING_NOISY");
                IntentUtility.forceMute(context);
                break;
            }
            case Intent.ACTION_BOOT_COMPLETED: {
                Log.d(TAG, "ACTION_BOOT_COMPLETED");
                IntentUtility.start(context);
                break;
            }
            case Intent.ACTION_MY_PACKAGE_REPLACED: {
                Log.d(TAG, "ACTION_MY_PACKAGE_REPLACED");
                IntentUtility.start(context);
                break;
            }
        }
    }
}
