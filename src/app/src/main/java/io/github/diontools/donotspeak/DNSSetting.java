package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

final class DNSSetting {
    private static final String PREF_NAME = "donotspeak_pref.xml";

    private static final String KEY_RESTORE_VOLUME = "RESTORE_VOLUME";
    private static final String KEY_BLUETOOTH_HEADSET_ADDRESSES = "BLUETOOTH_HEADSET_ADDRESSES";

    private static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }


    public static boolean getRestoreVolume(Context context) {
        return getPref(context).getBoolean(KEY_RESTORE_VOLUME, false);
    }

    public static void setRestoreVolume(Context context, boolean value) {
        getPref(context)
                .edit()
                .putBoolean(KEY_RESTORE_VOLUME, value)
                .apply();
    }


    public static Set<String> getBluetoothHeadsetAddresses(Context context) {
        Set<String> value = getPref(context).getStringSet(KEY_BLUETOOTH_HEADSET_ADDRESSES, null);
        return value != null ? value : new HashSet<String>();
    }

    public static void setBlueToothHeadsetAddresses(Context context, Set<String> value) {
        getPref(context)
                .edit()
                .putStringSet(KEY_BLUETOOTH_HEADSET_ADDRESSES, value)
                .apply();
    }
}
