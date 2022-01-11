package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class DNSSetting {
    private static final String PREF_NAME = "donotspeak_pref.xml";

    private enum Keys {
        RESTORE_VOLUME,
        REQUEST_TO_STOP_PLAYBACK,
        BLUETOOTH_HEADSET_ADDRESSES,
        DIAGNOSTICS_FILE_LOG,
        USE_ADJUST_VOLUME,
    }

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }


    public static boolean getRestoreVolume(Context context) {
        return getPref(context).getBoolean(Keys.RESTORE_VOLUME.name(), false);
    }

    public static void setRestoreVolume(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.RESTORE_VOLUME.name(), value)
                .apply();
    }


    public static boolean getRequestToStopPlayback(Context context) {
        return getPref(context).getBoolean(Keys.REQUEST_TO_STOP_PLAYBACK.name(), false);
    }

    public static void setRequestToStopPlayback(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.REQUEST_TO_STOP_PLAYBACK.name(), value)
                .apply();
    }


    public static Set<String> getBluetoothHeadsetAddresses(Context context) {
        Set<String> value = getPref(context).getStringSet(Keys.BLUETOOTH_HEADSET_ADDRESSES.name(), null);
        return value != null ? value : new HashSet<String>();
    }

    public static void setBlueToothHeadsetAddresses(Context context, Set<String> value) {
        getPref(context)
                .edit()
                .putStringSet(Keys.BLUETOOTH_HEADSET_ADDRESSES.name(), value)
                .apply();
    }


    public static boolean getDiagnosticsFileLog(Context context) {
        return getPref(context).getBoolean(Keys.DIAGNOSTICS_FILE_LOG.name(), false);
    }

    public static void setDiagnosticsFileLog(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.DIAGNOSTICS_FILE_LOG.name(), value)
                .apply();
    }


    public static boolean getUseAdjustVolume(Context context) {
        return getPref(context).getBoolean(Keys.USE_ADJUST_VOLUME.name(), false);
    }

    public static void setUseAdjustVolume(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(Keys.USE_ADJUST_VOLUME.name(), value)
                .apply();
    }
}
