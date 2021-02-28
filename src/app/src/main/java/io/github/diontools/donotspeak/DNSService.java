package io.github.diontools.donotspeak;

import android.app.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;
import android.media.AudioDeviceInfo;

import java.text.SimpleDateFormat;
import java.util.*;


public final class DNSService extends Service {
    private static final String NOTIFICATION_ID = "DoNotSpeak_Status_Notification";
    private static final String TAG = "DNSService";

    public static final String ACTION_START = "START";
    public static final String ACTION_TOGGLE = "TOGGLE";
    public static final String ACTION_SWITCH = "SWITCH";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";
    public static final String ACTION_SHUTDOWN = "SHUTDOWN";

    public static final String DISABLE_TIME_NAME = "DISABLE_TIME";

    private static final String ANDROID_MEDIA_VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String ANDROID_MEDIA_EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    public static boolean IsLive = false;
    public static DiagnosticsLogger Logger;

    private boolean enabled = false;
    private boolean stopUntilScreenOff = false;
    private int beforeVolume = -1;

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private String disableTimeString = "";

    private Intent disableIntent;
    private PendingIntent toggleIntent;
    private PendingIntent startIntent;

    private AlarmManager alarmManager;
    private NotificationManager notificationManager;
    private AudioManager audioManager;

    private BroadcastReceiver broadcastReceiver;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // set debug logger
        if (BuildConfig.DEBUG) {
            Logger = DiagnosticsLogger.Instance;
        }

        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onCreate");

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

        this.broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DiagnosticsLogger logger = Logger;

                String action = intent.getAction();
                if (action == null) return;

                switch (action) {
                    case ANDROID_MEDIA_VOLUME_CHANGED_ACTION: {
                        int streamType = intent.getIntExtra(ANDROID_MEDIA_EXTRA_VOLUME_STREAM_TYPE, -1);
                        if (streamType == AudioManager.STREAM_MUSIC) {
                            int prevVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                            int volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                            if (logger != null) logger.Log(TAG, "VOLUME_CHANGED_ACTION " + prevVolume + " -> " + volume);
                            if (volume != 0 || prevVolume != volume) {
                                DNSService.this.update();
                            }
                        }
                        break;
                    }
                    case Intent.ACTION_SCREEN_OFF:
                        if (logger != null) logger.Log(TAG, "ACTION_SCREEN_OFF");
                        if (DNSService.this.stopUntilScreenOff) {
                            DNSService.this.start();
                        } else {
                            DNSService.this.update();
                        }
                        break;
                    case AudioManager.ACTION_AUDIO_BECOMING_NOISY:
                        if (logger != null) logger.Log(TAG, "ACTION_AUDIO_BECOMING_NOISY");
                        DNSService.this.update(true);
                        break;
                    case Intent.ACTION_HEADSET_PLUG:
                        if (logger != null) logger.Log(TAG, "ACTION_HEADSET_PLUG");
                        if (intent.getIntExtra("state", -1) == 0) {
                            if (logger != null) logger.Log(TAG, "unplugged");
                            DNSService.this.update();
                        }
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ANDROID_MEDIA_VOLUME_CHANGED_ACTION);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);

        this.registerReceiver(this.broadcastReceiver, intentFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onStartCommand flags:" + flags + " id:" + startId);

        IsLive = true;

        String command = null;
        if (intent != null) {
            command = intent.getAction();
            if (logger != null) logger.Log(TAG, "command:" + command);
        }

        if (command == null) {
            if (logger != null) logger.Log(TAG, "command is null, force start");
            command = ACTION_START; // for kill
        }

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
            case ACTION_SWITCH: {
                if (this.enabled) {
                    this.stopUntilScreenOff = true;
                    this.stop(new Date(0));
                } else {
                    this.start();
                }
                break;
            }
            case ACTION_STOP: {
                this.stopUntilScreenOff = false;
                long disableTime = intent.getLongExtra(DISABLE_TIME_NAME, 0);
                if (disableTime > 0) {
                    this.stop(new Date(disableTime));
                }
                break;
            }
            case ACTION_STOP_UNTIL_SCREEN_OFF: {
                this.stopUntilScreenOff = true;
                this.stop(new Date(0));
                break;
            }
            case ACTION_SHUTDOWN: {
                this.stopSelf();
                IsLive = false;
            }
            default: {
                if (logger != null) logger.Log(TAG, "unknown command");
            }
        }

        return START_STICKY;
    }

    private void start() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "start");
        if (!this.enabled) {
            this.beforeVolume = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (logger != null) logger.Log(TAG, "beforeVolume: " + this.beforeVolume);
        }

        this.enabled = true;
        this.stopUntilScreenOff = false;
        this.cancelTimer();
        this.update();
        Toast.makeText(this, this.getStartedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void stop(Date disableTime) {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "stop disableTime:" + disableTime);
        this.enabled = false;

        if (disableTime.getTime() > 0) {
            long startTime = disableTime.getTime();
            this.disableTimeString = DateFormat.format(disableTime);

            this.cancelTimer();

            if (logger != null) logger.Log(TAG, "set timer: " + this.disableTimeString);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, startTime, this.startIntent);
            } else {
                this.alarmManager.setExact(AlarmManager.RTC, startTime, this.startIntent);
            }
        }

        this.update();
        Toast.makeText(this, this.getStoppedMessage(), Toast.LENGTH_SHORT).show();
    }

    private String getStartedMessage() {
        return this.getResources().getString(R.string.start_toast_text);
    }

    private String getStoppedMessage() {
        Resources res = this.getResources();
        return this.stopUntilScreenOff ? res.getString(R.string.stop_until_screen_off) : String.format(res.getString(R.string.stop_toast_text), this.disableTimeString);
    }

    private void cancelTimer() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "cancel timer");
        this.alarmManager.cancel(this.startIntent);
    }

    private void update() {
        this.update(false);
    }

    private void update(boolean forceMute) {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "update forceMute:" + forceMute);

        if (this.enabled) {
            this.mute(forceMute);
        } else {
            this.unmute();
        }

        this.createNotification(NOTIFICATION_ID, this.enabled);
        this.responseStateToTile();
    }

    private void createNotification(String id, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel(id, this.getResources().getString(R.string.notification_channel_name));
        }

        RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.notification_layout);
        remoteViews.setImageViewResource(R.id.imageView, enabled ? R.drawable.ic_launcher_round : R.drawable.ic_noisy);
        remoteViews.setTextViewText(R.id.textView, enabled ? this.getStartedMessage() : this.getStoppedMessage());

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
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "mute force:" + force);

        if (!force && this.isHeadsetConnected()) {
            if (logger != null) logger.Log(TAG, "Headset connected");
            return;
        }

        if (logger != null) logger.Log(TAG, "set volume 0");
        this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);

        if (logger != null) logger.Log(TAG, "volume: " + this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
    }

    private void unmute() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "unmute");

        if (DNSSetting.getRestoreVolume(this) && this.beforeVolume >= 0) {
            if (logger != null) logger.Log(TAG, "restore volume: " + this.beforeVolume);
            this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, this.beforeVolume, AudioManager.FLAG_SHOW_UI);
            this.beforeVolume = -1;
        }
    }

    private boolean isHeadsetConnected() {
        DiagnosticsLogger logger = Logger;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (logger != null) logger.Log(TAG, "isWiredHeadsetOn:" + this.audioManager.isWiredHeadsetOn() + " isBluetoothScoOn:" + this.audioManager.isBluetoothScoOn() + " isBluetoothA2dpOn:" + this.audioManager.isBluetoothA2dpOn());
            return this.audioManager.isWiredHeadsetOn() || this.audioManager.isBluetoothScoOn() || this.audioManager.isBluetoothA2dpOn();
        } else {
            AudioDeviceInfo[] devices = this.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int type = device.getType();
                if (logger != null)
                    logger.Log(TAG, "device:" + device.toString() + " type:" + type);

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

    private void responseStateToTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IntentUtility.setTileState(this.enabled, this.stopUntilScreenOff, this.disableTimeString);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onDestroy!");
        if (this.broadcastReceiver != null) {
            this.unregisterReceiver(this.broadcastReceiver);
            this.broadcastReceiver = null;
        }
    }
}