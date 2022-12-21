package io.github.diontools.donotspeak;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
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
    private OutputStreamWriter outputStreamWriter;
    private Thread.UncaughtExceptionHandler backupDefaultUncaughtExceptionHandler;

    public void Log(String tag, String message) {
        Log.d(tag, message);

        OutputStreamWriter writer = this.outputStreamWriter;
        Callback callback = this.callback;
        if (writer != null || callback != null) {
            String str = DateFormat.format(new Date()) + " " + message;
            this.writeFile(str, writer);

            if (callback != null) {
                try {
                    Log.d(TAG, str);
                    callback.call(str);
                } catch (Exception ex) {
                    Log.e(TAG, "callback error", ex);
                }
            }
        }
    }

    private void writeFile(String str) {
        this.writeFile(str, this.outputStreamWriter);
    }

    private void writeFile(String str, OutputStreamWriter writer) {
        if (writer != null) {
            try {
                writer.write(str);
                writer.write("\n");
                writer.flush();
            } catch (Exception ex) {
                Log.e(TAG, "write error", ex);
            }
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public String setFileDir(Context context, boolean enable) {
        OutputStreamWriter writer = this.outputStreamWriter;
        if (writer != null) {
            this.outputStreamWriter = null;

            try {
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
                this.Log(TAG, e.toString());
            }
        }

        if (enable) {
            String filename = "diagnostics_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".txt";
            File file = new File(context.getExternalCacheDir(), filename);
            Log.d(TAG, file.getPath());

            try {
                FileOutputStream outputStream = new FileOutputStream(file.getPath(), true);
                this.outputStreamWriter = new OutputStreamWriter(outputStream);
            } catch (FileNotFoundException e) {
                this.Log(TAG, e.toString());
            }

            Toast.makeText(context, file.getPath(), Toast.LENGTH_LONG).show();

            this.catchUnhandledException(context);
        }

        return null;
    }

    private void catchUnhandledException(Context context) {
        if (this.backupDefaultUncaughtExceptionHandler != null) {
            return;
        }

        this.backupDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                Log.e(TAG, e.toString());
                String message = DiagnosticsLogger.DateFormat.format(new Date()) + " " + e + "\r\n" + TextUtils.join("\r\n", e.getStackTrace());
                DiagnosticsLogger.this.writeFile(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            DiagnosticsLogger.this.backupDefaultUncaughtExceptionHandler.uncaughtException(t, e);
        });
    }
}
