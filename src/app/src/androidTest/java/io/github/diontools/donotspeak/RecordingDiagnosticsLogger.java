package io.github.diontools.donotspeak;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/** Captures service diagnostics without enabling file logging in the target app. */
final class RecordingDiagnosticsLogger extends DiagnosticsLogger {
    private static final String TAG = "TestDiagnostics";

    private final Object monitor = new Object();
    private final List<String> entries = new ArrayList<>();

    @Override
    public void Log(String tag, String message) {
        String entry = tag + ": " + message;
        Log.d(TAG, entry);

        synchronized (this.monitor) {
            this.entries.add(entry);
            this.monitor.notifyAll();
        }
    }

    boolean awaitContains(String text, long timeoutMillis) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;

        synchronized (this.monitor) {
            while (!this.containsLocked(text)) {
                long remaining = deadline - SystemClock.elapsedRealtime();
                if (remaining <= 0) return false;
                this.monitor.wait(remaining);
            }
            return true;
        }
    }

    boolean awaitContainsAny(long timeoutMillis, String... texts) throws InterruptedException {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;

        synchronized (this.monitor) {
            while (!this.containsAnyLocked(texts)) {
                long remaining = deadline - SystemClock.elapsedRealtime();
                if (remaining <= 0) return false;
                this.monitor.wait(remaining);
            }
            return true;
        }
    }

    boolean contains(String text) {
        synchronized (this.monitor) {
            return this.containsLocked(text);
        }
    }

    String dump() {
        synchronized (this.monitor) {
            return String.join("\n", this.entries);
        }
    }

    private boolean containsLocked(String text) {
        for (String entry : this.entries) {
            if (entry.contains(text)) return true;
        }
        return false;
    }

    private boolean containsAnyLocked(String... texts) {
        for (String text : texts) {
            if (this.containsLocked(text)) return true;
        }
        return false;
    }
}
