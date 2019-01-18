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
        switch (action) {
            case AudioManager.ACTION_AUDIO_BECOMING_NOISY: {
                Log.d(TAG, "ACTION_AUDIO_BECOMING_NOISY");
                Intent startIntent = new Intent(context, DNSService.class).setAction(DNSService.ACTION_FORCE_MUTE);
                context.startService(startIntent);
                break;
            }
            case Intent.ACTION_BOOT_COMPLETED: {
                Log.d(TAG, "ACTION_BOOT_COMPLETED");
                Intent startIntent = new Intent(context, DNSService.class).setAction(DNSService.ACTION_START);
                Compat.startForegroundService(context, startIntent);
                break;
            }
        }
    }
}
