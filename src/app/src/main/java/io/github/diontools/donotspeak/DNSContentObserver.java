package io.github.diontools.donotspeak;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public final class DNSContentObserver extends ContentObserver {
    private static final String TAG = "DNSContentObserver";

    private Runnable callback;

    public DNSContentObserver(Handler handler, Runnable callback) {
        super(handler);
        this.callback = callback;
    }

    @Override
    public boolean deliverSelfNotifications() {
        return false;
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        super.onChange(selfChange, uri);
        Log.d(TAG, "setting changed:" + uri);
        this.callback.run();
    }
}