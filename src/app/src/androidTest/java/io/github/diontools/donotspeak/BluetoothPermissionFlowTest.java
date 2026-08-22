package io.github.diontools.donotspeak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 31)
public class BluetoothPermissionFlowTest extends BluetoothPermissionTestBase {
    @Test
    public void canceledDialog_preservesSettingAndStartsService() throws Exception {
        this.assertBluetoothPermission(false);
        this.configure(true);
        this.prepareNonZeroVolume();

        this.launchDefaultActivity();

        UiObject2 title = this.device.wait(
                Until.findObject(By.res("android", "alertTitle")),
                TIMEOUT_MILLIS);
        assertNotNull("Bluetooth permission explanation dialog was not displayed", title);
        assertEquals(
                this.targetContext.getString(R.string.permission_dialog_bluetooth_title),
                title.getText());

        this.device.pressBack();
        this.device.waitForIdle();
        assertTrue(
                "Bluetooth permission explanation dialog did not close",
                Boolean.TRUE.equals(this.device.wait(
                        Until.gone(By.res("android", "alertTitle")),
                        TIMEOUT_MILLIS)));

        this.assertSafeMuteAfterPermissionDenial();
        assertTrue(
                "Canceling the dialog changed the saved Bluetooth setting",
                DNSSetting.getUseBluetooth(this.targetContext));
    }
}
