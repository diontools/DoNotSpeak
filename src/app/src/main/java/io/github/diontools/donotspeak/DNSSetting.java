package io.github.diontools.donotspeak;

import android.app.backup.BackupManager;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class DNSSetting {
    private static final String PREF_NAME = "donotspeak_pref.xml";

    private enum Keys {
        RESTORE_VOLUME,
        REQUEST_TO_STOP_PLAYBACK,
        KEEP_SCREEN_ON,
        BLUETOOTH_HEADSET_ADDRESSES,
        DIAGNOSTICS_FILE_LOG,
        USE_ADJUST_VOLUME,
        USE_NOTIFICATION,
        USE_BLUETOOTH,
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static void requestBackup(Context context) {
        new BackupManager(context).dataChanged();
    }


    public static boolean getRestoreVolume(Context context) {
        return getPref(context).getBoolean(Keys.RESTORE_VOLUME.name(), false);
    }

    public static void setRestoreVolume(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.RESTORE_VOLUME.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getRequestToStopPlayback(Context context) {
        return getPref(context).getBoolean(Keys.REQUEST_TO_STOP_PLAYBACK.name(), false);
    }

    public static void setRequestToStopPlayback(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.REQUEST_TO_STOP_PLAYBACK.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getKeepScreenOn(Context context) {
        return getPref(context).getBoolean(Keys.KEEP_SCREEN_ON.name(), false);
    }

    public static void setKeepScreenOn(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.KEEP_SCREEN_ON.name(), value)
                .apply();
        requestBackup(context);
    }


    public static Set<String> getBluetoothHeadsetAddresses(Context context) {
        Set<String> value = getPref(context).getStringSet(Keys.BLUETOOTH_HEADSET_ADDRESSES.name(), null);
        return value != null ? value : new HashSet<>();
    }

    public static void setBlueToothHeadsetAddresses(Context context, Set<String> value) {
        getPref(context)
                .edit()
                .putStringSet(Keys.BLUETOOTH_HEADSET_ADDRESSES.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getDiagnosticsFileLog(Context context) {
        return getPref(context).getBoolean(Keys.DIAGNOSTICS_FILE_LOG.name(), false);
    }

    public static void setDiagnosticsFileLog(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.DIAGNOSTICS_FILE_LOG.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getUseAdjustVolume(Context context) {
        return getPref(context).getBoolean(Keys.USE_ADJUST_VOLUME.name(), false);
    }

    public static void setUseAdjustVolume(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.USE_ADJUST_VOLUME.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getUseBluetooth(Context context) {
        return getPref(context).getBoolean(Keys.USE_BLUETOOTH.name(), true);
    }

    public static void setUseBluetooth(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.USE_BLUETOOTH.name(), value)
                .apply();
        requestBackup(context);
    }


    public static boolean getUseNotification(Context context) {
        return getPref(context).getBoolean(Keys.USE_NOTIFICATION.name(), true);
    }

    public static void setUseNotification(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.USE_NOTIFICATION.name(), value)
                .apply();
        requestBackup(context);
    }
}
