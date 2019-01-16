package io.github.diontools.donotspeak

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("tag", "start service!")
        var serviceIntent = Intent(this, DNSService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}
