package io.github.diontools.donotspeak;

import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Captures every screen across the locale / navigation-mode / night-mode matrix.
 *
 * Screenshots land in {@code /sdcard/Download/shots/api<SDK>/} and are pulled by the host with
 * {@code adb pull}. Compare a run taken before a change with one taken after.
 *
 * The activities under test are not exported, but instrumentation runs inside the target app's
 * process, so they can be started directly.
 */
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    private static final String TAG = "ScreenshotTest";

    private static final String SHOT_ROOT = "/sdcard/Download/shots";
    private static final long LAUNCH_TIMEOUT_MS = 5000;
    /** Lets window animations and the system bars settle before the capture. */
    private static final long SETTLE_MS = 1500;

    private static final String NAV_GESTURAL = "com.android.internal.systemui.navbar.gestural";
    private static final String NAV_THREE_BUTTON = "com.android.internal.systemui.navbar.threebutton";

    private Instrumentation instrumentation;
    private UiDevice device;
    private Context targetContext;
    private String shotDir;

    @Before
    public void setUp() throws Exception {
        this.instrumentation = InstrumentationRegistry.getInstrumentation();
        this.device = UiDevice.getInstance(this.instrumentation);
        this.targetContext = this.instrumentation.getTargetContext();
        this.shotDir = SHOT_ROOT + "/api" + Build.VERSION.SDK_INT;

        this.device.executeShellCommand("rm -r " + this.shotDir);
        this.device.executeShellCommand("mkdir -p " + this.shotDir);

        // Keep the runtime permission dialog out of the captures.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.grant("android.permission.POST_NOTIFICATIONS");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            this.grant("android.permission.BLUETOOTH_CONNECT");
        }
    }

    @Test
    public void captureAllScreens() throws Exception {
        List<String> captured = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The full matrix in Japanese, plus an English spot check in the stock configuration
            // (gesture navigation + light theme). Navigation and theme behave the same regardless
            // of locale, so English only needs to show how the longer strings wrap.
            captured.addAll(this.captureMatrix("ja", true));
            captured.addAll(this.captureMatrix("en", false));
            this.setAppLocale("");
        } else {
            // Per-app language needs API 33, so the device locale is whatever the AVD is set to.
            captured.addAll(this.captureMatrix(Locale.getDefault().getLanguage(), true));
        }

        // Leave the device on the defaults so the next run is not affected.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            this.setNavigationMode(NAV_GESTURAL);
            this.setNightMode("no");
        }

        for (String path : captured) {
            String listing = this.device.executeShellCommand("ls -l " + path).trim();
            assertTrue("screenshot missing: " + path, !listing.contains("No such file"));
        }
        Log.d(TAG, "captured " + captured.size() + " screenshots in " + this.shotDir);
    }

    /**
     * @param full when false, only the stock configuration (gestural + light) is captured
     */
    private List<String> captureMatrix(String locale, boolean full) throws Exception {
        this.setAppLocale(locale);
        List<String> paths = new ArrayList<>();

        // Gestural navigation and night mode both arrived in API 29. Below that there is a single
        // configuration to capture.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            paths.addAll(this.captureScreens(locale + "-default"));
            return paths;
        }

        String[] navs = full ? new String[]{NAV_GESTURAL, NAV_THREE_BUTTON} : new String[]{NAV_GESTURAL};
        String[] nights = full ? new String[]{"no", "yes"} : new String[]{"no"};

        for (String nav : navs) {
            for (String night : nights) {
                this.setNavigationMode(nav);
                this.setNightMode(night);
                String suffix = locale
                        + "-" + (nav.equals(NAV_GESTURAL) ? "gestural" : "threebutton")
                        + "-" + (night.equals("yes") ? "dark" : "light");
                paths.addAll(this.captureScreens(suffix));
            }
        }
        return paths;
    }

    private List<String> captureScreens(String suffix) throws Exception {
        List<String> paths = new ArrayList<>();

        // MainActivity with ACTION_MAIN has no UI -- it requests permissions, starts the service
        // and exits -- so there is nothing to capture for it.

        Intent disable = new Intent(this.targetContext, MainActivity.class);
        disable.setAction(MainActivity.ACTION_DISABLE_DIALOG);
        this.launch(disable);
        paths.add(this.shoot("disable-" + suffix));
        // The expanded options make the dialog its tallest, which is where clipping shows up first.
        // Until.hasObject yields Boolean.FALSE rather than null on timeout, so null-check the
        // object itself instead of the wait result.
        this.device.wait(Until.hasObject(this.byId("optionsHeader")), LAUNCH_TIMEOUT_MS);
        UiObject2 optionsHeader = this.device.findObject(this.byId("optionsHeader"));
        if (optionsHeader != null) {
            optionsHeader.click();
            this.device.waitForIdle();
            Thread.sleep(SETTLE_MS);
            paths.add(this.shoot("disable-options-" + suffix));
        } else {
            Log.w(TAG, "optionsHeader not found, skipping disable-options-" + suffix);
        }
        this.close();

        this.launch(new Intent(this.targetContext, SettingActivity.class));
        paths.add(this.shoot("setting-" + suffix));
        this.close();

        this.launch(new Intent(this.targetContext, DiagnosticsActivity.class));
        paths.add(this.shoot("diagnostics-" + suffix));
        this.close();

        return paths;
    }

    private void launch(Intent intent) throws Exception {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.targetContext.startActivity(intent);

        this.device.wait(Until.hasObject(By.pkg(this.targetContext.getPackageName())), LAUNCH_TIMEOUT_MS);
        this.device.waitForIdle();
        Thread.sleep(SETTLE_MS);
    }

    private String shoot(String name) throws Exception {
        String path = this.shotDir + "/" + name + ".png";
        this.device.executeShellCommand("screencap -p " + path);
        Log.d(TAG, "captured " + path);
        return path;
    }

    /** Closes the current activity so the next launch starts from a clean stack. */
    private void close() {
        this.device.pressBack();
        this.device.waitForIdle();
    }

    private void setNavigationMode(String overlay) throws Exception {
        this.device.executeShellCommand("cmd overlay enable " + overlay);
        this.device.waitForIdle();
        Thread.sleep(SETTLE_MS);
    }

    private void setNightMode(String value) throws Exception {
        this.device.executeShellCommand("cmd uimode night " + value);
        this.device.waitForIdle();
        Thread.sleep(SETTLE_MS);
    }

    /**
     * Switches the per-app language, leaving the system locale alone. Needs API 33; below that the
     * locale service does not exist and this is a no-op. An empty tag restores the system locale.
     */
    private void setAppLocale(String languageTag) throws Exception {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        this.device.executeShellCommand("cmd locale set-app-locales "
                + this.targetContext.getPackageName() + " --locales " + languageTag);
        this.device.waitForIdle();
        Thread.sleep(SETTLE_MS);
    }

    private void grant(String permission) throws Exception {
        this.device.executeShellCommand(
                "pm grant " + this.targetContext.getPackageName() + " " + permission);
    }

    private BySelector byId(String id) {
        return By.res(this.targetContext.getPackageName(), id);
    }
}
