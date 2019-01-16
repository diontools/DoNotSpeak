package io.github.diontools.donotspeak

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class DNSService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("service", "started!")
        var notificationService = NotificationManagerCompat.from(this)
        var id = "DoNotSpeak_Notification"

        var notification =
            NotificationCompat.Builder(this, id)
                .setContentTitle("たいとる")
                .setContentText("こんてんと")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build()

        this.startForeground(1, notification)

        this.mute()

        return START_STICKY
    }

    private fun mute() {
        var audioManager = ContextCompat.getSystemService(this, AudioManager::class.java)
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
    }
}