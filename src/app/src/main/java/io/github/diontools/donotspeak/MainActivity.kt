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
        var serviceIntent = Intent(this, DNSService::class.java).putExtra(DNSService.COMMAND_NAME, DNSService.COMMAND_START)
        ContextCompat.startForegroundService(this, serviceIntent)

        this.finish()
        return

        this.button.setOnClickListener {
            var intent = Intent(this, DNSService::class.java).putExtra(DNSService.COMMAND_NAME, DNSService.COMMAND_TOGGLE)
            this.startService(intent)
        }
    }
}
