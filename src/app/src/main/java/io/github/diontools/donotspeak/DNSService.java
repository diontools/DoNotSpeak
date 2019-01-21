package io.github.diontools.donotspeak;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.media.AudioDeviceInfo;

import java.text.SimpleDateFormat;
import java.util.*;


public final class DNSService extends Service {
    private static final String NOTIFICATION_ID = "DoNotSpeak_Notification";
    private static final String TAG = "DNSService";

    public static final String ACTION_START = "START";
    public static final String ACTION_TOGGLE = "TOGGLE";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_FORCE_MUTE = "FORCE_MUTE";

    public static final String DISABLE_TIME_NAME = "DISABLE_TIME";

    private boolean enabled = false;
    private final Handler mainHandler = new Handler();
    private final DNSContentObserver contentObserver = new DNSContentObserver(this.mainHandler, new Runnable() {
        @Override
        public void run() {
            DNSService.this.update();
        }
    });

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String disableTimeString = "";

    private Intent disableIntent;
    private PendingIntent toggleIntent;
    private PendingIntent startIntent;

    private AlarmManager alarmManager;
    private NotificationManager notificationManager;
    private AudioManager audioManager;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        this.disableIntent =
                new Intent(this, MainActivity.class)
                        .setAction(MainActivity.ACTION_DISABLE_DIALOG)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        this.toggleIntent =
                PendingIntent.getService(
                        this,
                        0,
                        new Intent(this, DNSService.class).setAction(ACTION_TOGGLE),
                        PendingIntent.FLAG_CANCEL_CURRENT);

        this.startIntent =
                PendingIntent.getService(
                        this,
                        0,
                        new Intent(this, DNSService.class).setAction(ACTION_START),
                        PendingIntent.FLAG_CANCEL_CURRENT);

        this.notificationManager = Compat.getSystemService(this, NotificationManager.class);
        if (this.notificationManager == null) throw new UnsupportedOperationException("NotificationManager is null");

        this.alarmManager = Compat.getSystemService(this, AlarmManager.class);
        if (this.alarmManager == null) throw new UnsupportedOperationException("AlarmManager is null");

        this.audioManager = Compat.getSystemService(this, AudioManager.class);
        if (this.audioManager == null) throw new UnsupportedOperationException("AudioManager is null");

        this.getApplicationContext().getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("volume_music_speaker"), true, this.contentObserver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "started! flags:" + flags + " id:" + startId);

        String command = null;
        if (intent != null) {
            command = intent.getAction();
            Log.d(TAG, "command:" + command);
        }

        if (command == null) {
            Log.d(TAG, "command is null");
        } else {
            switch (command) {
                case ACTION_START: {
                    this.start();
                    break;
                }
                case ACTION_TOGGLE: {
                    if (this.enabled) {
                        // to disable
                        this.startActivity(this.disableIntent);
                    } else {
                        this.start();
                    }
                    break;
                }
                case ACTION_STOP: {
                    int disableTime = intent.getIntExtra(DISABLE_TIME_NAME, 0);
                    if (disableTime > 0) {
                        this.stop(disableTime);
                    }
                    break;
                }
                case ACTION_FORCE_MUTE: {
                    this.mute(true);
                    this.start();
                    break;
                }
                default: {
                    Log.d(TAG, "unknown command");
                }
            }
        }

        return START_STICKY;
    }

    private void start() {
        this.enabled = true;
        this.cancelTimer();
        this.update();
        Toast.makeText(this, "DoNotSpeak!", Toast.LENGTH_SHORT).show();
    }

    private void stop(int disableTime) {
        this.enabled = false;

        long startTime = System.currentTimeMillis() + disableTime;
        this.disableTimeString = DateFormat.format(new Date(startTime));

        this.cancelTimer();

        Log.d(TAG, "set timer: " + this.disableTimeString);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, startTime, this.startIntent);
        } else {
            this.alarmManager.setExact(AlarmManager.RTC, startTime, this.startIntent);
        }

        this.update();
        Toast.makeText(this, "Speak until " + this.disableTimeString, Toast.LENGTH_SHORT).show();
    }

    private void cancelTimer() {
        Log.d(TAG, "cancel timer");
        this.alarmManager.cancel(this.startIntent);
    }

    private void update() {
        if (this.enabled) {
            this.mute(false);
        }

        this.createNotification(NOTIFICATION_ID, this.enabled);
    }

    private void createNotification(String id, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel(id, "DoNotSpeak");
        }

        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.notification_layout);
        remoteViews.setImageViewResource(R.id.imageView, enabled ? R.drawable.ic_launcher : R.drawable.ic_noisy);
        remoteViews.setTextViewText(R.id.textView, enabled ? "DoNotSpeak!" : "Speak until " + this.disableTimeString);

        Notification notification =
                Compat.createNotificationBuilder(this, id)
                        .setSmallIcon(enabled ? R.drawable.ic_volume_off_black_24dp : R.drawable.ic_volume_up_black_24dp)
                        .setContent(remoteViews)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(toggleIntent)
                        .build();

        this.startForeground(1, notification);
    }

    private void createNotificationChannel(String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (this.notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
                chan.setLightColor(Color.BLUE);
                chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                this.notificationManager.createNotificationChannel(chan);
            }
        }
    }

    private void mute(boolean force) {
        if (!force && this.isHeadsetConnected()) {
            Log.d(TAG, "Headset connected!");
            return;
        }

        this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
    }

    private boolean isHeadsetConnected() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return this.audioManager.isWiredHeadsetOn() || this.audioManager.isBluetoothScoOn() || this.audioManager.isBluetoothA2dpOn();
        } else {
            AudioDeviceInfo[] devices = this.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (int i = 0; i < devices.length; i++) {
                AudioDeviceInfo device = devices[i];

                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                        || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                ) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy!");
    }
}