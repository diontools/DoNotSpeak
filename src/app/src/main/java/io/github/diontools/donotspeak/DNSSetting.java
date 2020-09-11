package io.github.diontools.donotspeak;

import android.content.Context;
import android.content.SharedPreferences;

final class DNSSetting {
    private static final String PREF_NAME = "donotspeak_pref.xml";

    private static final String KEY_RESTORE_VOLUME = "RESTORE_VOLUME";

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
}
