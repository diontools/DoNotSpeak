package io.github.diontools.donotspeak;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

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
            View view = this.getLayoutInflater().inflate(R.layout.disable_dialog_layout, null);
            final NumberPicker numPicker = view.findViewById(R.id.numberPicker);
            String[] values = new String[24];
            for (int i = 0; i < values.length; i++) values[i] = String.valueOf((i + 1) * 5);
            numPicker.setMinValue(0);
            numPicker.setMaxValue(values.length - 1);
            numPicker.setDisplayedValues(values);

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle("Speak?")
                            .setView(view)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    int disableTime = (numPicker.getValue() + 1) * 5 * 60 * 1000;
                                    Intent intent =
                                            new Intent(MainActivity.this, DNSService.class)
                                                    .setAction(DNSService.ACTION_STOP)
                                                    .putExtra(DNSService.DISABLE_TIME_NAME, disableTime);
                                    MainActivity.this.startService(intent);
                                    MainActivity.this.exit();
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.exit();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    MainActivity.this.exit();
                                }
                            })
                            .create();
            dialog.show();
        } else {
            Log.d(TAG, "start service!");
            Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_START);
            Compat.startForegroundService(this, serviceIntent);

            this.exit();
        }
    }

    private void exit() {
        this.finishAndRemoveTask();
        this.overridePendingTransition(0, 0);
    }
}
