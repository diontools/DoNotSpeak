package io.github.diontools.donotspeak;

import android.os.Handler;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Debouncer {
    private static final String TAG = "Debouncer";

    private final Handler handler;
    private final int dueTime;
    private final Runnable callback;
    private final Runnable checkRunner = new Runnable() {
        @Override
        public void run() {
            Debouncer.this.check();
        }
    };

    private final AtomicBoolean locked = new AtomicBoolean(false);
    private int elapsedTime;
    private long startTime;

    public Debouncer(Handler handler, int dueTime, Runnable callback) {
        this.handler = handler;
        this.dueTime = dueTime;
        this.callback = callback;
        this.elapsedTime = this.dueTime;
    }

    public void update() {
        for (; ; ) {
            // lock
            if (this.locked.compareAndSet(false, true)) {
                int elapsed = this.elapsedTime;

                // reset time
                Log.d(TAG, "reset");
                this.startTime = System.currentTimeMillis();
                this.elapsedTime = 0;

                // not running?
                if (elapsed >= this.dueTime) {
                    this.start(this.dueTime);
                }

                // unlock
                this.locked.set(false);
                break;
            }
        }
    }

    private void start(int delayTime) {
        Log.d(TAG, "start");
        this.handler.postDelayed(this.checkRunner, delayTime);
    }

    private void check() {
        Log.d(TAG, "check");
        // lock
        if (this.locked.compareAndSet(false, true)) {
            long currentTime = System.currentTimeMillis();
            this.elapsedTime += currentTime - this.startTime;
            boolean over = this.elapsedTime >= this.dueTime;

            // retry
            if (!over) {
                int remainTime = this.dueTime - this.elapsedTime;
                this.startTime = currentTime;
                this.start(remainTime);
            }

            // unlock
            this.locked.set(false);

            // callback
            if (over) this.callback.run();
        }
    }
}
