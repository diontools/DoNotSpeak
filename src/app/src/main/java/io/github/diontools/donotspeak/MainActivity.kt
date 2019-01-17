package io.github.diontools.donotspeak

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_main)

        Log.d("tag", "start service!")
        val serviceIntent = Intent(this, DNSService::class.java).setAction(DNSService.ACTION_START)
        ContextCompat.startForegroundService(this, serviceIntent)

        this.finish()
        return

        this.button.setOnClickListener {
            val intent = Intent(this, DNSService::class.java).setAction(DNSService.ACTION_TOGGLE)
            this.startService(intent)
        }
    }
}
