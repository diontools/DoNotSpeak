package io.github.diontools.donotspeak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.After;
import org.junit.Before;

abstract class BluetoothPermissionTestBase {
    protected static final long TIMEOUT_MILLIS = 10_000;

    protected Instrumentation instrumentation;
    protected UiDevice device;
    protected Context targetContext;
    protected RecordingDiagnosticsLogger logger;

    private AudioManager audioManager;
    private boolean previousUseBluetooth;
    private boolean previousUseNotification;
    private int previousVolume;

    @Before
    public void setUpBluetoothPermissionTest() throws Exception {
        this.instrumentation = InstrumentationRegistry.getInstrumentation();
        this.device = UiDevice.getInstance(this.instrumentation);
        this.targetContext = this.instrumentation.getTargetContext();
        this.audioManager = (AudioManager) this.targetContext.getSystemService(Context.AUDIO_SERVICE);
        assertNotNull("AudioManager is unavailable", this.audioManager);

        this.previousUseBluetooth = DNSSetting.getUseBluetooth(this.targetContext);
        this.previousUseNotification = DNSSetting.getUseNotification(this.targetContext);
        this.previousVolume = this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        this.logger = new RecordingDiagnosticsLogger();
        DNSService.Logger = this.logger;
        this.stopService();

        // Discard diagnostics from cleanup so every assertion belongs to the current scenario.
        this.logger = new RecordingDiagnosticsLogger();
        DNSService.Logger = this.logger;
    }

    @After
    public void tearDownBluetoothPermissionTest() throws Exception {
        try {
            this.stopService();
        } finally {
            DNSSetting.setUseBluetooth(this.targetContext, this.previousUseBluetooth);
            DNSSetting.setUseNotification(this.targetContext, this.previousUseNotification);
            this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, this.previousVolume, 0);
            DNSService.Logger = null;
        }
    }

    protected void assertBluetoothPermission(boolean expectedGranted) {
        assertEquals(
                "Unexpected BLUETOOTH_CONNECT permission state",
                expectedGranted,
                PermissionUtility.isBluetoothGranted(this.targetContext));
    }

    protected void configure(boolean useBluetooth) {
        DNSSetting.setUseBluetooth(this.targetContext, useBluetooth);
        DNSSetting.setUseNotification(this.targetContext, false);
        assertEquals(useBluetooth, DNSSetting.getUseBluetooth(this.targetContext));
    }

    protected void prepareNonZeroVolume() {
        if (this.audioManager.isVolumeFixed()) {
            fail("The emulator has fixed volume; safe mute cannot be verified");
        }

        int maxVolume = this.audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int testVolume = Math.max(1, maxVolume / 2);
        this.audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, testVolume, 0);

        assertTrue(
                "Could not prepare a non-zero music volume",
                this.awaitVolume(testVolume, TIMEOUT_MILLIS));
    }

    protected void launchSwitchActivity() throws Exception {
        Intent intent = new Intent(this.targetContext, MainActivity.class)
                .setAction(DNSService.ACTION_SWITCH)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.targetContext.startActivity(intent);

        assertTrue(
                "MainActivity did not deliver ACTION_SWITCH:\n" + this.logger.dump(),
                this.logger.awaitContains("command:" + DNSService.ACTION_SWITCH, TIMEOUT_MILLIS));
    }

    protected void launchDefaultActivity() {
        Intent intent = new Intent(this.targetContext, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.targetContext.startActivity(intent);
    }

    protected void assertSafeMuteAfterPermissionDenial() throws Exception {
        assertTrue(
                "Permission denial was not logged:\n" + this.logger.dump(),
                this.logger.awaitContains("BLUETOOTH_CONNECT permission: denied", TIMEOUT_MILLIS));
        assertTrue(
                "Safe mute did not reach volume zero:\n" + this.logger.dump(),
                this.awaitVolume(0, TIMEOUT_MILLIS));
        assertTrue(
                "The service did not log the resulting volume:\n" + this.logger.dump(),
                this.logger.awaitContains("volume: 0", TIMEOUT_MILLIS));
        assertFalse(
                "Permission denial incorrectly entered profile initialization wait:\n" + this.logger.dump(),
                this.logger.contains("Bluetooth initialized: false"));
        assertFalse(
                "Permission denial stopped mute while waiting for Bluetooth:\n" + this.logger.dump(),
                this.logger.contains("Bluetooth not initialized"));
        assertTrue("DNSService did not remain live", DNSService.IsLive);
    }

    protected void assertVolumeZero() {
        assertTrue(
                "Music volume did not become zero:\n" + this.logger.dump(),
                this.awaitVolume(0, TIMEOUT_MILLIS));
    }

    private boolean awaitVolume(int expectedVolume, long timeoutMillis) {
        long deadline = SystemClock.elapsedRealtime() + timeoutMillis;
        do {
            if (this.audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == expectedVolume) {
                return true;
            }
            SystemClock.sleep(50);
        } while (SystemClock.elapsedRealtime() < deadline);

        return false;
    }

    private void stopService() throws Exception {
        if (DNSService.IsLive) {
            Intent shutdown = new Intent(this.targetContext, DNSService.class)
                    .setAction(DNSService.ACTION_SHUTDOWN);
            this.targetContext.startService(shutdown);
            this.logger.awaitContains("onDestroy", TIMEOUT_MILLIS);
        }

        this.targetContext.stopService(new Intent(this.targetContext, DNSService.class));
    }
}
