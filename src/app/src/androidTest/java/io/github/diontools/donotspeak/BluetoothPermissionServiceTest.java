package io.github.diontools.donotspeak;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 31)
public class BluetoothPermissionServiceTest extends BluetoothPermissionTestBase {
    @Test
    public void permissionDenied_doesNotWaitForProfiles() throws Exception {
        this.assertBluetoothPermission(false);
        this.configure(true);
        this.prepareNonZeroVolume();

        this.launchSwitchActivity();

        this.assertSafeMuteAfterPermissionDenial();
        assertTrue(
                "The saved Bluetooth setting was changed by the service",
                DNSSetting.getUseBluetooth(this.targetContext));
    }

    @Test
    public void settingDisabled_logsSettingState() throws Exception {
        this.assertBluetoothPermission(false);
        this.configure(false);
        this.prepareNonZeroVolume();

        this.launchSwitchActivity();

        assertTrue(
                "Disabled Bluetooth setting was not logged:\n" + this.logger.dump(),
                this.logger.awaitContains("useBluetooth: false", TIMEOUT_MILLIS));
        this.assertVolumeZero();
        assertFalse(
                "A disabled setting was reported as a permission denial:\n" + this.logger.dump(),
                this.logger.contains("BLUETOOTH_CONNECT permission: denied"));
    }

    @Test
    public void permissionGranted_doesNotLogDenial() throws Exception {
        this.assertBluetoothPermission(true);
        this.configure(true);

        this.launchSwitchActivity();

        assertTrue(
                "Bluetooth initialization was not evaluated:\n" + this.logger.dump(),
                this.logger.awaitContainsAny(
                        TIMEOUT_MILLIS,
                        "Bluetooth not supported",
                        "Bluetooth disabled",
                        "Bluetooth initialized: false",
                        "Bluetooth initialized: true"));
        assertFalse(
                "A granted permission was reported as denied:\n" + this.logger.dump(),
                this.logger.contains("BLUETOOTH_CONNECT permission: denied"));
    }
}
