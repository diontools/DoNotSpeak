package io.github.diontools.donotspeak;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DiagnosticsLogger {
    public static final DiagnosticsLogger Instance = new DiagnosticsLogger();
    public static final String TAG = "DiagnosticsLogger";

    public interface Callback {
        void call(String str);
    }

    private static final SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.getDefault());

    private Callback callback;

    public void Log(String tag, String message) {
        Log.d(tag, message);

        try {
            Callback callback = this.callback;
            if (callback != null) {
                String str = DateFormat.format(new Date()) + " " + message;
                Log.d(TAG, str);
                callback.call(str);
            }
        } catch (Exception ex) {
            Log.e(TAG, "callback error", ex);
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
}
