package io.github.diontools.donotspeak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public final class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main)

        Log.d("tag", "start service!");
        Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_START);
        Compat.startForegroundService(this, serviceIntent);

        this.finish();
    }
}
