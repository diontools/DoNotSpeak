package io.github.diontools.donotspeak;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public final class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_DISABLE_DIALOG = "DISABLE_DIALOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main)

        String action = this.getIntent().getAction();
        Log.d(TAG, action);

        if (action == ACTION_DISABLE_DIALOG) {
            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle("Speak?")
                            .setMessage("WARNING: Enable Speakers?")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(MainActivity.this, DNSService.class).setAction(DNSService.ACTION_STOP);
                                    MainActivity.this.startService(intent);
                                    MainActivity.this.finishAndRemoveTask();
                                    MainActivity.this.overridePendingTransition(0, 0);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.finishAndRemoveTask();
                                    MainActivity.this.overridePendingTransition(0, 0);
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    MainActivity.this.finishAndRemoveTask();
                                    MainActivity.this.overridePendingTransition(0, 0);
                                }
                            })
                            .create();
            dialog.show();
        } else {
            Log.d(TAG, "start service!");
            Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_START);
            Compat.startForegroundService(this, serviceIntent);

            this.finishAndRemoveTask();
        }
    }
}
