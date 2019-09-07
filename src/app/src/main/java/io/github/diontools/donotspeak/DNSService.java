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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
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
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";

    public static final String DISABLE_TIME_NAME = "DISABLE_TIME";

    private boolean enabled = false;
    private boolean stopUntilScreenOff = false;

    private final Handler mainHandler = new Handler();
    private final DNSContentObserver contentObserver = new DNSContentObserver(this.mainHandler, new Runnable() {
        @Override
        public void run() {
            DNSService.this.update();
            DNSService.this.updationDebouncer.update();
        }
    });
    private final Debouncer updationDebouncer = new Debouncer(this.mainHandler, 1000, new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "debounce");
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

        this.getContentResolver().registerContentObserver(android.provider.Settings.System.getUriFor("volume_music_speaker"), true, this.contentObserver);

        this.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (DNSService.this.stopUntilScreenOff) {
                            DNSService.this.start();
                        } else {
                            DNSService.this.update();
                        }
                    }
                },
                new IntentFilter(Intent.ACTION_SCREEN_OFF));

        this.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Log.d(TAG, "ACTION_AUDIO_BECOMING_NOISY");
                        DNSService.this.update(true);
                    }
                },
                new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        this.registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.getIntExtra("state", -1) == 0) {
                            Log.d(TAG, "unplugged");
                            DNSService.this.update();
                        }
                    }
                },
                new IntentFilter(Intent.ACTION_HEADSET_PLUG)
        );
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
            Log.d(TAG, "command is null, force start");
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
            case ACTION_STOP: {
                this.stopUntilScreenOff = false;
                int disableTime = intent.getIntExtra(DISABLE_TIME_NAME, 0);
                if (disableTime > 0) {
                    this.stop(disableTime);
                }
                break;
            }
            case ACTION_STOP_UNTIL_SCREEN_OFF: {
                this.stopUntilScreenOff = true;
                this.stop(-1);
                break;
            }
            default: {
                Log.d(TAG, "unknown command");
            }
        }

        return START_STICKY;
    }

    private void start() {
        this.enabled = true;
        this.stopUntilScreenOff = false;
        this.cancelTimer();
        this.update();
        Toast.makeText(this, this.getStartedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void stop(int disableTime) {
        Log.d(TAG, "stop disableTime:" + disableTime);
        this.enabled = false;

        if (disableTime >= 0) {
            long startTime = System.currentTimeMillis() + disableTime;
            this.disableTimeString = DateFormat.format(new Date(startTime));

            this.cancelTimer();

            Log.d(TAG, "set timer: " + this.disableTimeString);
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
        Log.d(TAG, "cancel timer");
        this.alarmManager.cancel(this.startIntent);
    }

    private void update() {
        this.update(false);
    }

    private void update(boolean forceMute) {
        if (this.enabled) {
            this.mute(forceMute);
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
        if (!force && this.isHeadsetConnected()) {
            Log.d(TAG, "Headset connected!");
            return;
        }

        Log.d(TAG, "set volume 0");
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

    private void responseStateToTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IntentUtility.setTileState(this.enabled, this.stopUntilScreenOff, this.disableTimeString);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy!");
    }
}