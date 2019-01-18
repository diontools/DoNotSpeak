package io.github.diontools.donotspeak;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public final class MainActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main)

        Log.d("tag", "start service!");
        Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_START);
        ContextCompat.startForegroundService(this, serviceIntent);

        this.finish();
    }
}
