package io.github.diontools.donotspeak

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log

class DNSContentObserver(handler: Handler, private val callback: () -> Unit): ContentObserver(handler) {
    companion object {
        val TAG = DNSContentObserver::class.java.simpleName
    }

    override fun deliverSelfNotifications(): Boolean {
        return false
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d(TAG, "setting changed:" + uri)
        this.callback()
    }
}