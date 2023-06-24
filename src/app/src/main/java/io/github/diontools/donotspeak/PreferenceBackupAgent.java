package io.github.diontools.donotspeak;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupDataInput;
import android.app.backup.BackupDataOutput;
import android.app.backup.SharedPreferencesBackupHelper;
import android.os.ParcelFileDescriptor;

import java.io.IOException;

public final class PreferenceBackupAgent extends BackupAgentHelper {
    private static final String TAG = "PreferenceBackupAgent";
    static final String FILE_NAME = "donotspeak_pref.xml";
    static final String BACKUP_KEY = "prefs";

    @Override
    public void onCreate() {
        addHelper(BACKUP_KEY, new SharedPreferencesBackupHelper(this, FILE_NAME));
    }

    @Override
    public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) throws IOException {
        super.onBackup(oldState, data, newState);

        DiagnosticsLogger logger = DNSService.Logger;
        if (logger != null) {
            logger.Log(TAG, "onBackup");
        }
    }

    @Override
    public void onRestore(BackupDataInput data, int appVersionCode, ParcelFileDescriptor newState) throws IOException {
        super.onRestore(data, appVersionCode, newState);

        DiagnosticsLogger logger = DNSService.Logger;
        if (logger != null) {
            logger.Log(TAG, "onRestore");
        }
    }
}
