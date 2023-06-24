package io.github.diontools.donotspeak;

import android.Manifest;
import android.app.*;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;
import android.media.AudioDeviceInfo;

import java.text.SimpleDateFormat;
import java.util.*;


public final class DNSService extends Service {
    private static final String NOTIFICATION_ID = "DoNotSpeak_Status_Notification";
    private static final String TAG = "DNSService";

    public static final String ACTION_START = "START";
    public static final String ACTION_SWITCH = "SWITCH";
    public static final String ACTION_STOP = "STOP";
    public static final String ACTION_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";
    public static final String ACTION_SHUTDOWN = "SHUTDOWN";
    public static final String ACTION_APPLY_SETTINGS = "APPLY_SETTINGS";
    public static final String ACTION_REBOOT = "REBOOT";

    public static final String DISABLE_TIME_NAME = "DISABLE_TIME";

    private static final String ANDROID_MEDIA_VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
    private static final String ANDROID_MEDIA_EXTRA_VOLUME_STREAM_TYPE = "android.media.EXTRA_VOLUME_STREAM_TYPE";

    public static boolean IsLive = false;
    public static DiagnosticsLogger Logger;

    private SharedPreferences statePreferences;

    private boolean enabled = false;
    private boolean stopUntilScreenOff = false;
    private int beforeVolume = -1;
    private boolean useAdjustVolume = false;
    private boolean keepScreenOn = false;
    private boolean useBluetooth = false;

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private long disableTime;
    private String disableTimeString = "";

    private PendingIntent disableIntent;
    private PendingIntent startIntent;
    private PendingIntent stopUntilScreenOffIntent;

    private AlarmManager alarmManager;
    private NotificationManager notificationManager;
    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback;
    private AudioManager.AudioPlaybackCallback audioPlaybackCallback;
    private PowerManager.WakeLock wakeLock;

    private BroadcastReceiver broadcastReceiver;

    private Set<String> bluetoothHeadsetAddresses;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothProfile.ServiceListener bluetoothServiceListener;
    private BluetoothHeadset bluetoothHeadset;
    private BluetoothA2dp bluetoothA2dp;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // set debug logger
        if (BuildConfig.DEBUG || DNSSetting.getDiagnosticsFileLog(this)) {
            DiagnosticsLogger initLogger = Logger;
            if (initLogger == null) {
                Logger = initLogger = DiagnosticsLogger.Instance;
                initLogger.setFileDir(this, true);
            }
        }

        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onCreate");

        this.statePreferences = this.getSharedPreferences("dns_service_state", MODE_PRIVATE);

        this.applySettings();

        // Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
        final int immutableFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;

        this.disableIntent =
                PendingIntent.getActivity(
                        this.getApplicationContext(),
                        0,
                        new Intent(this.getApplicationContext(), MainActivity.class)
                                .setAction(MainActivity.ACTION_DISABLE_DIALOG),
                        immutableFlag);

        this.stopUntilScreenOffIntent =
                PendingIntent.getActivity(
                        this.getApplicationContext(),
                        0,
                        new Intent(this.getApplicationContext(), MainActivity.class)
                                .setAction(MainActivity.ACTION_STOP_UNTIL_SCREEN_OFF),
                        immutableFlag);

        this.startIntent =
                PendingIntent.getService(
                        this.getApplicationContext(),
                        0,
                        new Intent(this.getApplicationContext(), DNSService.class).setAction(ACTION_START),
                        PendingIntent.FLAG_CANCEL_CURRENT | immutableFlag);

        this.notificationManager = Compat.getSystemService(this, NotificationManager.class);
        if (this.notificationManager == null) throw new UnsupportedOperationException("NotificationManager is null");

        this.alarmManager = Compat.getSystemService(this, AlarmManager.class);
        if (this.alarmManager == null) throw new UnsupportedOperationException("AlarmManager is null");

        this.audioManager = Compat.getSystemService(this, AudioManager.class);
        if (this.audioManager == null) throw new UnsupportedOperationException("AudioManager is null");

        final PowerManager powerManager = Compat.getSystemService(this, PowerManager.class);
        if (powerManager == null) throw new UnsupportedOperationException("PowerManager is null");
        this.wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "DNSService::WakeLock");
        this.wakeLock.setReferenceCounted(false);

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
                    case BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED:
                        if (logger != null) logger.Log(TAG, "A2dp.ACTION_PLAYING_STATE_CHANGED " + intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) + " " + intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) + " -> " + intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1));
                        DNSService.this.update();
                        break;
                    case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                        if (logger != null) logger.Log(TAG, "A2dp.ACTION_CONNECTION_STATE_CHANGED " + intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) + " " + intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) + " -> " + intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1));
                        DNSService.this.update();
                        break;
                    case BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED:
                        if (logger != null) logger.Log(TAG, "Headset.ACTION_AUDIO_STATE_CHANGED " + intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) + " " + intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) + " -> " + intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1));
                        DNSService.this.update();
                        break;
                    case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                        if (logger != null) logger.Log(TAG, "Headset.ACTION_CONNECTION_STATE_CHANGED " + intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) + " " + intent.getIntExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, -1) + " -> " + intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1));
                        DNSService.this.update();
                        break;
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ANDROID_MEDIA_VOLUME_CHANGED_ACTION);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        intentFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);

        this.registerReceiver(this.broadcastReceiver, intentFilter);

        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (this.bluetoothAdapter == null) {
            if (logger != null) logger.Log(TAG, "bluetooth not supported.");
        } else {
            this.bluetoothServiceListener = new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    DiagnosticsLogger logger = Logger;
                    switch (profile) {
                        case BluetoothProfile.HEADSET:
                            if (logger != null) logger.Log(TAG, "HEADSET connected.");
                            DNSService.this.bluetoothHeadset = (BluetoothHeadset)proxy;
                            break;
                        case BluetoothProfile.A2DP:
                            if (logger != null) logger.Log(TAG, "A2DP connected.");
                            DNSService.this.bluetoothA2dp = (BluetoothA2dp)proxy;
                            break;
                    }
                    DNSService.this.update();
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    DiagnosticsLogger logger = Logger;
                    switch (profile) {
                        case BluetoothProfile.HEADSET:
                            if (logger != null) logger.Log(TAG, "HEADSET disconnected.");
                            DNSService.this.bluetoothHeadset = null;
                            break;
                        case BluetoothProfile.A2DP:
                            if (logger != null) logger.Log(TAG, "A2DP disconnected.");
                            DNSService.this.bluetoothA2dp = null;
                            break;
                    }
                }
            };

            if (logger != null) logger.Log(TAG, "get bluetooth profile proxy");
            final boolean headset = this.bluetoothAdapter.getProfileProxy(this.getApplicationContext(), this.bluetoothServiceListener, BluetoothProfile.HEADSET);
            final boolean a2dp = this.bluetoothAdapter.getProfileProxy(this.getApplicationContext(), this.bluetoothServiceListener, BluetoothProfile.A2DP);
            if (logger != null) logger.Log(TAG, "headset: " + headset + " a2dp: " + a2dp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.audioDeviceCallback = new AudioDeviceCallback() {
                @Override
                public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                    DiagnosticsLogger logger = Logger;
                    if (logger != null) {
                        logger.Log(TAG, "onAudioDevicesAdded");
                        for (AudioDeviceInfo device : addedDevices) {
                            logger.Log(TAG, "added device: " + device.toString() + " type: " + device.getType());
                        }
                    }
                    DNSService.this.update();
                }

                @Override
                public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                    DiagnosticsLogger logger = Logger;
                    if (logger != null) {
                        logger.Log(TAG, "onAudioDevicesRemoved");
                        for (AudioDeviceInfo device : removedDevices) {
                            logger.Log(TAG, "removed device: " + device.toString() + " type: " + device.getType());
                        }
                    }
                    DNSService.this.update();
                }
            };
            this.audioManager.registerAudioDeviceCallback(this.audioDeviceCallback, null);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    DiagnosticsLogger logger = Logger;
                    if (logger != null) {
                        logger.Log(TAG, "onPlaybackConfigChanged");
                        for (AudioPlaybackConfiguration config : configs) {
                            AudioAttributes audioAttributes = config.getAudioAttributes();
                            logger.Log(TAG, "playback: " + config + " content-type: " + audioAttributes.getContentType() + " usage: " + audioAttributes.getUsage());
                        }
                    }
                    DNSService.this.update();
                }
            };
            this.audioManager.registerAudioPlaybackCallback(this.audioPlaybackCallback, null);
        }
    }

    private void applySettings() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "applySettings");
        this.bluetoothHeadsetAddresses = DNSSetting.getBluetoothHeadsetAddresses(this);
        this.useAdjustVolume = DNSSetting.getUseAdjustVolume(this);
        this.keepScreenOn = DNSSetting.getKeepScreenOn(this);
        this.useBluetooth = DNSSetting.getUseBluetooth(this);
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
            if (logger != null) logger.Log(TAG, "command is null, reboot");
            command = ACTION_REBOOT; // for kill
        }

        switch (command) {
            case ACTION_START: {
                this.start();
                break;
            }
            case ACTION_SWITCH: {
                if (this.enabled) {
                    this.stop(new Date(0), true);
                } else {
                    this.start();
                }
                break;
            }
            case ACTION_STOP: {
                long disableTime = intent.getLongExtra(DISABLE_TIME_NAME, 0);
                this.stop(new Date(disableTime), false);
                break;
            }
            case ACTION_STOP_UNTIL_SCREEN_OFF: {
                this.stop(new Date(0), true);
                break;
            }
            case ACTION_SHUTDOWN: {
                this.stopSelf();
                IsLive = false;
                break;
            }
            case ACTION_APPLY_SETTINGS: {
                this.applySettings();
                this.update();
                break;
            }
            case ACTION_REBOOT: {
                this.restoreState();
                this.update();
                break;
            }
            default: {
                if (logger != null) logger.Log(TAG, "unknown command");
            }
        }

        return START_STICKY;
    }

    private void backupState() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "backup state: " + this.enabled + " " + this.stopUntilScreenOff + " " + this.disableTime);

        this.statePreferences
            .edit()
            .putBoolean("enabled", this.enabled)
            .putBoolean("stopUntilScreenOff", this.stopUntilScreenOff)
            .putLong("disableTime", this.disableTime)
            .apply();
    }

    private void restoreState() {
        this.enabled = this.statePreferences.getBoolean("enabled", true);
        this.stopUntilScreenOff = this.statePreferences.getBoolean("stopUntilScreenOff", false);
        this.setDisableTime(new Date(this.statePreferences.getLong("disableTime", 0)));

        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "restore state: " + this.enabled + " " + this.stopUntilScreenOff + " " + this.disableTime);
    }

    private void clearState() {
        this.statePreferences
            .edit()
            .clear()
            .apply();
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
        this.setDisableTime(new Date(0));
        this.backupState();

        this.update();

        if (DNSSetting.getRequestToStopPlayback(this)) {
            AudioFocusUtility.request(this.audioManager, logger);
        }

        Toast.makeText(this, this.getStartedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void stop(Date disableTime, boolean stopUntilScreenOff) {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "stop disableTime:" + disableTime + " stopUntilScreenOff: " + stopUntilScreenOff);

        this.enabled = false;
        this.stopUntilScreenOff = stopUntilScreenOff;
        this.setDisableTime(disableTime);
        this.backupState();

        this.update();
        Toast.makeText(this, this.getStoppedMessage(), Toast.LENGTH_SHORT).show();
    }

    private void setDisableTime(Date disableTime) {
        this.disableTime = disableTime.getTime();
        this.disableTimeString = this.disableTime > 0 ? DateFormat.format(disableTime) : "";
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
            if (this.wakeLock.isHeld()) {
                if (logger != null) logger.Log(TAG, "release wake lock");
                this.wakeLock.release();
            }
        } else {
            this.unmute();
            if (this.keepScreenOn && !this.wakeLock.isHeld()) {
                if (logger != null) logger.Log(TAG, "acquire wake lock");
                this.wakeLock.acquire();
            }
        }

        this.cancelTimer();

        long startTime = this.disableTime;
        if (startTime > 0) {
            if (logger != null) logger.Log(TAG, "set timer: " + this.disableTimeString);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, startTime, this.startIntent);
            } else {
                this.alarmManager.set(AlarmManager.RTC, startTime, this.startIntent);
            }
        }

        this.createNotification(NOTIFICATION_ID, this.enabled);
        this.responseStateToTile();
    }

    private void createNotification(String id, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.createNotificationChannel(id, this.getResources().getString(R.string.notification_channel_name));
        }

        Notification.Builder builder =
                Compat.createNotificationBuilder(this, id)
                        .setSmallIcon(enabled ? R.drawable.ic_volume_off_black_24dp : R.drawable.ic_volume_up_black_24dp)
                        .setContentTitle(enabled ? this.getStartedMessage() : this.getStoppedMessage())
                        .setSubText(this.getResources().getText(enabled ? R.string.notification_subtext_enabled : R.string.notification_subtext_disabled))
                        .setColor(enabled ? 0x1976D2 : 0xFF5419)
                        .setOngoing(true)
                        .setPriority(Notification.PRIORITY_LOW)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(enabled ? disableIntent : startIntent);

        if (enabled) {
            builder.addAction(new Notification.Action(R.drawable.ic_noisy, this.getResources().getString(R.string.notification_action_untilScreenOff), this.stopUntilScreenOffIntent));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android S+: avoid 10s notification delay
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        }

        Notification notification = builder.build();

        this.startForeground(1, notification);
    }

    private void createNotificationChannel(String channelId, String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (this.notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW);
                chan.setLightColor(Color.BLUE);
                chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                chan.setShowBadge(false);
                this.notificationManager.createNotificationChannel(chan);
            }
        }
    }

    private void mute(boolean force) {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "mute force:" + force);

        if (!force && !this.isBluetoothInitialized()) {
            if (logger != null) logger.Log(TAG, "Bluetooth not initialized");
            return;
        }

        if (!force && this.isHeadsetConnected()) {
            if (logger != null) logger.Log(TAG, "Headset connected");
            return;
        }

        this.setVolumeTo(0, 0);
    }

    private void unmute() {
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "unmute");

        if (DNSSetting.getRestoreVolume(this) && this.beforeVolume >= 0) {
            if (logger != null) logger.Log(TAG, "restore volume: " + this.beforeVolume);
            this.setVolumeTo(this.beforeVolume, AudioManager.FLAG_SHOW_UI);
            this.beforeVolume = -1;
        }
    }

    private void setVolumeTo(int targetVolume, int flags) {
        DiagnosticsLogger logger = Logger;

        if (useAdjustVolume) {
            int failSafeLoopCount = 0;
            while (true) {
                int currentVolume = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (logger != null) logger.Log(TAG, "volume current: " + currentVolume + " target: " + targetVolume);
                if (currentVolume == targetVolume || failSafeLoopCount++ > 50) {
                    break;
                }

                if (currentVolume > targetVolume) {
                    if (logger != null) logger.Log(TAG, "adjust lower volume");
                    this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, flags);
                } else {
                    if (logger != null) logger.Log(TAG, "adjust raise volume");
                    this.audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, flags);
                }
            }
        } else {
            if (logger != null) logger.Log(TAG, "set volume: " + targetVolume);
            this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, flags);
            if (logger != null) logger.Log(TAG, "volume: " + this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        }
    }

    private boolean isHeadsetConnected() {
        final DiagnosticsLogger logger = Logger;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (logger != null) logger.Log(TAG, "isWiredHeadsetOn:" + this.audioManager.isWiredHeadsetOn() + " isBluetoothScoOn:" + this.audioManager.isBluetoothScoOn() + " isBluetoothA2dpOn:" + this.audioManager.isBluetoothA2dpOn());
            return this.audioManager.isWiredHeadsetOn() || this.audioManager.isBluetoothScoOn() || this.audioManager.isBluetoothA2dpOn();
        } else {
            AudioDeviceInfo[] devices = this.audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                int type = device.getType();
                if (logger != null) logger.Log(TAG, "device:" + device + " type:" + type);

                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_USB_HEADSET
                ) {
                    return true;
                }

                if (this.useBluetooth) {
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                            || type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    ) {
                        if (this.isBluetoothHeadsetConnected()) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }
    }

    private boolean isBluetoothHeadsetConnected() {
        DiagnosticsLogger logger = Logger;
        boolean isConnectedWithoutAudio = false;
        boolean isAudioPlaying = false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            if (logger != null) logger.Log(TAG, "BLUETOOTH_CONNECT permission: denied");
        } else {
            if (this.bluetoothHeadset != null) {
                List<BluetoothDevice> devices = this.bluetoothHeadset.getConnectedDevices();
                for (BluetoothDevice device : devices) {
                    boolean isAudioConnected = this.bluetoothHeadset.isAudioConnected(device);
                    if (logger != null) logger.Log(TAG, "headset: " + device.getName() + " " + device.getAddress() + " isAudioConnected: " + isAudioConnected);
                    if (isAudioConnected) isAudioPlaying = true;
                    if (this.bluetoothHeadsetAddresses.contains(device.getAddress())) {
                        if (isAudioConnected) {
                            if (logger != null) logger.Log(TAG, "Bluetooth headset detected");
                            return true;
                        }
                        isConnectedWithoutAudio = true;
                    }
                }
            }

            if (this.bluetoothA2dp != null) {
                List<BluetoothDevice> devices = this.bluetoothA2dp.getConnectedDevices();
                for (BluetoothDevice device : devices) {
                    boolean isA2dpPlaying = this.bluetoothA2dp.isA2dpPlaying(device);
                    if (logger != null) logger.Log(TAG, "a2dp: " + device.getName() + " " + device.getAddress() + " isA2dpPlaying: " + isA2dpPlaying);
                    if (isA2dpPlaying) isAudioPlaying = true;
                    if (this.bluetoothHeadsetAddresses.contains(device.getAddress())) {
                        if (isA2dpPlaying) {
                            if (logger != null) logger.Log(TAG, "Bluetooth headset detected");
                            return true;
                        }
                        isConnectedWithoutAudio = true;
                    }
                }
            }
        }

        if (logger != null) logger.Log(TAG, "isConnectedWithoutAudio: " + isConnectedWithoutAudio + " isAudioPlaying: " + isAudioPlaying);

        return isConnectedWithoutAudio && !isAudioPlaying;
    }

    private boolean isBluetoothInitialized() {
        DiagnosticsLogger logger = Logger;

        if (!this.useBluetooth) {
            if (logger != null) logger.Log(TAG, "useBluetooth: false");
            return true;
        }

        BluetoothAdapter adapter = this.bluetoothAdapter;
        if (adapter == null) {
            if (logger != null) logger.Log(TAG, "Bluetooth not supported");
        } else if (!adapter.isEnabled()) {
            if (logger != null) logger.Log(TAG, "Bluetooth disabled");
        } else if (this.bluetoothHeadset == null || this.bluetoothA2dp == null) {
            if (logger != null) logger.Log(TAG, "Bluetooth initialized: false");
            return false;
        }

        if (logger != null) logger.Log(TAG, "Bluetooth initialized: true");
        return true;
    }

    private void responseStateToTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            IntentUtility.setTileState(this.enabled, this.stopUntilScreenOff, this.disableTimeString);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onTaskRemoved");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DiagnosticsLogger logger = Logger;
        if (logger != null) logger.Log(TAG, "onDestroy");

        this.clearState();

        if (this.broadcastReceiver != null) {
            this.unregisterReceiver(this.broadcastReceiver);
            this.broadcastReceiver = null;
        }
        if (this.bluetoothHeadset != null) {
            this.bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, this.bluetoothHeadset);
            this.bluetoothHeadset = null;
        }
        if (this.bluetoothA2dp != null) {
            this.bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, this.bluetoothA2dp);
            this.bluetoothA2dp = null;
        }
        if (this.bluetoothServiceListener != null) {
            this.bluetoothServiceListener = null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            this.audioManager.unregisterAudioDeviceCallback(this.audioDeviceCallback);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.audioManager.unregisterAudioPlaybackCallback(this.audioPlaybackCallback);
        }
        if (this.wakeLock != null && this.wakeLock.isHeld()) {
            this.wakeLock.release();
        }
    }
}