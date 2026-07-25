package io.github.diontools.donotspeak;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Minimal test to verify the androidTest APK installs and runs on every supported API level. */
@RunWith(AndroidJUnit4.class)
public class SmokeTest {
    private static final String TAG = "SmokeTest";

    private static final String SHOT_DIR = "/sdcard/Download/shots";

    @Test
    public void instrumentationIsAlive() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        assertNotNull(instrumentation);

        UiDevice device = UiDevice.getInstance(instrumentation);
        assertNotNull(device);

        Log.d(TAG, "SDK_INT: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "targetPackage: " + instrumentation.getTargetContext().getPackageName());

        // executeShellCommand runs as the shell UID. The screenshot harness relies on this to
        // switch navigation mode / night mode and to write PNGs outside the app sandbox.
        // NOTE: the command is exec'd directly, not through a shell. Builtins (echo), pipes,
        // redirection and quoting are unavailable -- only real binaries under /system/bin.
        String sdk = device.executeShellCommand("getprop ro.build.version.sdk").trim();
        Log.d(TAG, "getprop -> '" + sdk + "'");
        assertTrue("executeShellCommand did not work", sdk.equals(String.valueOf(Build.VERSION.SDK_INT)));

        // screencap is what the harness will use, so verify it end to end.
        device.executeShellCommand("mkdir -p " + SHOT_DIR);
        String name = "smoke-api" + Build.VERSION.SDK_INT + ".png";
        device.executeShellCommand("screencap -p " + SHOT_DIR + "/" + name);
        // ls prints the base name only, and reports missing files on stdout rather than failing.
        String listing = device.executeShellCommand("ls -l " + SHOT_DIR + "/" + name).trim();
        Log.d(TAG, "screencap -> '" + listing + "'");
        assertTrue("screencap did not produce a file: " + listing,
                listing.contains(name) && !listing.contains("No such file"));
    }
}
