package io.github.diontools.donotspeak;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.view.WindowManager;

@TargetApi(Build.VERSION_CODES.N)
public final class DNSTileService extends TileService {
    private static final String TAG = "DNSTileService";
    public static final String ACTION_RESPONSE_STATE = "RESPONSE_STATE";

    public static final  String RESPONSE_STATE_EXTRA_ENABLED = "ENABLED";
    public static final  String RESPONSE_STATE_EXTRA_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";
    public static final  String RESPONSE_STATE_EXTRA_DISABLE_TIME = "DISABLE_TIME";

    private static final int STATE_IDLE = 0;
    private static final int STATE_TOGGLE = 1;

    private boolean enabled = false;
    private boolean stopUntilScreenOff = false;
    private String disableTimeString = "";

    private int state = STATE_IDLE;

    @Override
    public void onTileAdded() {
        Log.d(TAG, "onTileAdded");
        this.requestStateToService();
    }

    @Override
    public void onStartListening() {
        Log.d(TAG, "onStartListening");
        this.requestStateToService();
    }

    @Override
    public void onClick() {
        Log.d(TAG, "onClick");

        this.state = STATE_TOGGLE;
        this.requestStateToService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "started! flags:" + flags + " id:" + startId);

        if (intent != null) {
            String command = intent.getAction();
            Log.d(TAG, "command:" + command);

            if (command == null) {
                Log.d(TAG, "command is null");
                command = ""; // skip
            }

            switch (command) {
                case ACTION_RESPONSE_STATE: {
                    this.enabled = intent.getBooleanExtra(RESPONSE_STATE_EXTRA_ENABLED, false);
                    this.stopUntilScreenOff = intent.getBooleanExtra(RESPONSE_STATE_EXTRA_STOP_UNTIL_SCREEN_OFF, false);
                    this.disableTimeString = intent.getStringExtra(RESPONSE_STATE_EXTRA_DISABLE_TIME);
                    switch (this.state) {
                        case STATE_TOGGLE: {
                            this.state = STATE_IDLE;
                            this.toggle();
                            break;
                        }
                    }
                    this.updateIcon();
                    break;
                }
                default: {
                    Log.d(TAG, "unknown command");
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    private void toggle() {
        if (this.enabled) {
            this.showDisableDialogAndCollapse();
        } else {
            Compat.startForegroundService(this, new Intent(this, DNSService.class).setAction(DNSService.ACTION_TOGGLE));
        }
    }

    private void showDisableDialogAndCollapse() {
        this.startActivityAndCollapse(new Intent(this, MainActivity.class).setAction(MainActivity.ACTION_DISABLE_DIALOG));
    }

    private void requestStateToService() {
        Intent serviceIntent = new Intent(this, DNSService.class).setAction(DNSService.ACTION_REQUEST_STATE_FROM_TILE);
        Compat.startForegroundService(this, serviceIntent);
    }

    private void updateIcon() {
        Tile tile = this.getQsTile();
        if (tile == null) {
            Log.d(TAG, "updateIcon: tile is null");
            return;
        }

        Log.d(TAG, "updateIcon:" + this.enabled + " untilSF:" + this.stopUntilScreenOff + " time:" + this.disableTimeString);

        tile.setIcon(Icon.createWithResource(this, this.enabled ? R.drawable.ic_volume_off_black_24dp : R.drawable.ic_volume_up_black_24dp));
        tile.setLabel(this.enabled ? this.getString(R.string.app_name) : this.getStoppedMessage());
        tile.setState(this.enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private String getStoppedMessage() {
        Resources res = this.getResources();
        return this.stopUntilScreenOff ? res.getString(R.string.tile_stop_until_screen_off) : String.format(res.getString(R.string.tile_stop_text), this.disableTimeString);
    }
}
