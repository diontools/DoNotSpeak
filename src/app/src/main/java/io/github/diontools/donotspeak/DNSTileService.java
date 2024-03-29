package io.github.diontools.donotspeak;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.N)
public final class DNSTileService extends TileService {
    private static final String TAG = "DNSTileService";

    public static boolean enabled = false;
    public static boolean stopUntilScreenOff = false;
    public static String disableTimeString = "";

    private static DNSTileService listenerInstance;

    public static void requestUpdate() {
        DNSTileService service = listenerInstance;
        if (service != null) {
            service.updateIcon();
        }
    }

    @Override
    public void onTileAdded() {
        Log.d(TAG, "onTileAdded");
        this.updateIcon();
    }

    @Override
    public void onTileRemoved() {
        Log.d(TAG, "onTileRemoved");
    }

    @Override
    public void onStartListening() {
        Log.d(TAG, "onStartListening");
        this.updateIcon();
        listenerInstance = this;
    }

    @Override
    public void onStopListening() {
        Log.d(TAG, "onStopListening");
        listenerInstance = null;
    }

    @Override
    public void onClick() {
        Log.d(TAG, "onClick");
        this.toggle();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);
    }

    private boolean isWorking() {
        return enabled && DNSService.IsLive;
    }

    private void toggle() {
        if (this.isWorking()) {
            this.showDisableDialogAndCollapse();
        } else {
            if (this.isLocked()) {
                IntentUtility.start(this);
            } else {
                this.startAndCollapse();
            }
        }
    }

    private void showDisableDialogAndCollapse() {
        Compat.startActivityAndCollapse(
                this,
                new Intent(this, MainActivity.class)
                        .setAction(MainActivity.ACTION_DISABLE_DIALOG)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                NotificationInfo.REQUEST_CODE_TILE
        );
    }

    private void startAndCollapse() {
        Compat.startActivityAndCollapse(
                this,
                new Intent(this, MainActivity.class)
                        .setAction("")
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                NotificationInfo.REQUEST_CODE_TILE
        );
    }

    private void updateIcon() {
        Tile tile = this.getQsTile();
        if (tile == null) {
            Log.d(TAG, "updateIcon: tile is null");
            return;
        }

        Log.d(TAG, "updateIcon:" + enabled + " untilSF:" + stopUntilScreenOff + " time:" + disableTimeString + " isLive: " + DNSService.IsLive);

        boolean isWorking = this.isWorking();
        tile.setIcon(Icon.createWithResource(this, isWorking ? R.drawable.ic_volume_off_black_24dp : R.drawable.ic_volume_up_black_24dp));
        tile.setLabel(isWorking ? this.getString(R.string.app_name) : this.getStoppedMessage());
        tile.setState(isWorking ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.updateTile();
    }

    private String getStoppedMessage() {
        Resources res = this.getResources();
        if (!DNSService.IsLive) return res.getString(R.string.tile_is_not_live);
        return stopUntilScreenOff ? res.getString(R.string.tile_stop_until_screen_off) : String.format(res.getString(R.string.tile_stop_text), disableTimeString);
    }
}
