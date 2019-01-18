package io.github.diontools.donotspeak

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        Log.d("tag", "start service!")
        val serviceIntent = Intent(this, DNSService::class.java).setAction(DNSService.ACTION_START)
        ContextCompat.startForegroundService(this, serviceIntent)

        this.finish()
    }
}
