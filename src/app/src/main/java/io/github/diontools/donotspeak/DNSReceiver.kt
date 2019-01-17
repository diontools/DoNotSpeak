package io.github.diontools.donotspeak

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat

class DNSReceiver : BroadcastReceiver() {
    companion object {
        val TAG = DNSReceiver::class.java.simpleName
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        when (action) {
            AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                Log.d(TAG, "ACTION_AUDIO_BECOMING_NOISY")
                val startIntent = Intent(context, DNSService::class.java).setAction(DNSService.ACTION_FORCE_MUTE)
                context.startService(startIntent)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "ACTION_BOOT_COMPLETED")
                val startIntent = Intent(context, DNSService::class.java).setAction(DNSService.ACTION_START)
                ContextCompat.startForegroundService(context, startIntent)
            }
        }
    }
}
