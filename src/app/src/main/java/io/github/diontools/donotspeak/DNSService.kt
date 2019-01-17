package io.github.diontools.donotspeak

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val NOTIFICATION_ID = "DoNotSpeak_Notification"

class DNSService : Service() {
    companion object {
        val TAG = DNSService::class.java.simpleName

        const val ACTION_START = "START"
        const val ACTION_TOGGLE = "TOGGLE"
    }

    private var enabled = false
    private var contentObserver = DNSContentObserver(Handler())

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("not implemented")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        this.contentResolver.registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, this.contentObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "started! flags:" + flags + " id:" + startId)

        var command = null as String?
        if (intent != null) {
            command = intent.action
            Log.d(TAG, "command:" + command)
        }

        when (command) {
            ACTION_START -> {
                this.setEnabled(true)
            }
            ACTION_TOGGLE -> {
                setEnabled(!this.enabled)
            }
            else -> {
                Log.d(TAG, "unknown command")
            }
        }

        return START_STICKY
    }

    private fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        this.createNotification(NOTIFICATION_ID, enabled)

        if (enabled) {
            this.mute()
            Toast.makeText(this, "DoNotSpeak!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Speak!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotification(id: String, enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel(id, "DoNotSpeak")
        }

        val toggleIntent = Intent(this, DNSService::class.java).setAction(ACTION_TOGGLE)
        val pendingIntent = PendingIntent.getService(this, 0, toggleIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val remoteViews = RemoteViews(this.packageName, R.layout.notification_layout)
        remoteViews.setImageViewResource(R.id.imageView, if (enabled) R.drawable.ic_launcher else R.drawable.ic_noisy)
        remoteViews.setTextViewText(R.id.textView, if (enabled) "enabled" else "disabled")

        val notification =
            NotificationCompat.Builder(this, id)
                .setSmallIcon(if (enabled) R.drawable.ic_volume_off_black_24dp else R.drawable.ic_volume_up_black_24dp)
                .setContent(remoteViews)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(pendingIntent)
                .build()

        this.startForeground(1, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String) {
        val manager = NotificationManagerCompat.from(this)
        if (manager.getNotificationChannel(channelId) == null) {
            val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            chan.lightColor = Color.BLUE
            chan.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            manager.createNotificationChannel(chan)
        }
    }

    private fun mute() {
        val audioManager = ContextCompat.getSystemService(this, AudioManager::class.java)
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy!")
    }
}