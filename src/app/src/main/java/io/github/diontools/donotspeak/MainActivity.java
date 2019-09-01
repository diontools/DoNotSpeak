package io.github.diontools.donotspeak;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;

import java.util.Objects;

public final class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_DISABLE_DIALOG = "DISABLE_DIALOG";
    public static final String ACTION_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setContentView(R.layout.activity_main)

        // Allow show dialog on locked screen
        KeyguardManager keyguardManager = Compat.getSystemService(this, KeyguardManager.class);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            Log.d(TAG, "show when locked");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                keyguardManager.requestDismissKeyguard(this, null);
            } else {
                this.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }

        String action = this.getIntent().getAction();
        Log.d(TAG, action);

        if (Objects.equals(action, ACTION_DISABLE_DIALOG)) {
            View view = this.getLayoutInflater().inflate(R.layout.disable_dialog_layout, null);
            final NumberPicker numPicker = view.findViewById(R.id.numberPicker);
            String[] values = new String[24];
            for (int i = 0; i < values.length; i++) values[i] = String.valueOf((i + 1) * 5);
            numPicker.setMinValue(0);
            numPicker.setMaxValue(values.length - 1);
            numPicker.setDisplayedValues(values);

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.disable_alert_title)
                            .setView(view)
                            .setPositiveButton(R.string.disable_alert_okButton, new DialogInterface.OnClickListener() {
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
                            .setNeutralButton(R.string.disable_alert_untilScreenOffButton, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.stopUntilScreenOff();
                                }
                            })
                            .setNegativeButton(R.string.disable_alert_cancelButton, new DialogInterface.OnClickListener() {
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
        } else if (Objects.equals(action, ACTION_STOP_UNTIL_SCREEN_OFF)) {
            this.stopUntilScreenOff();
        } else if (Objects.equals(action, TileService.ACTION_QS_TILE_PREFERENCES)) {
            this.stopUntilScreenOff();
        } else {
            Log.d(TAG, "start service!");
            Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_START);
            Compat.startForegroundService(this, serviceIntent);

            this.exit();
        }
    }

    private void stopUntilScreenOff() {
        Intent intent =
                new Intent(MainActivity.this, DNSService.class)
                        .setAction(DNSService.ACTION_STOP_UNTIL_SCREEN_OFF);
        MainActivity.this.startService(intent);
        MainActivity.this.exit();
    }

    private void exit() {
        this.finishAndRemoveTask();
        this.overridePendingTransition(0, 0);
    }
}
