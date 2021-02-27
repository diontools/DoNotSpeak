package io.github.diontools.donotspeak;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class DiagnosticsContentProvider extends ContentProvider {
    private static final String AUTHORITY = BuildConfig.APPLICATION_ID;
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    private static final String TAG = "DiagnosticsCP";
    public static final String LOGS_FILE_NAME = "logs.txt";

    public static Uri getLogFileUri() {
        return Uri.withAppendedPath(CONTENT_URI, LOGS_FILE_NAME);
    }

    public static void writeLogFile(Context context, String logText) throws IOException {
        Log.d(TAG, "writeLogFile");
        File logFile = new File(context.getCacheDir(), LOGS_FILE_NAME);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(logFile, false), StandardCharsets.UTF_8)) {
            writer.write(logText);
        }
    }

    public static void writeLogFileInExternal(Context context, String logText) throws IOException {
        Log.d(TAG, "writeLogFileInExternal");
        File logFile = new File(context.getExternalCacheDir(), LOGS_FILE_NAME);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(logFile, false), StandardCharsets.UTF_8)) {
            writer.write(logText);
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        Log.d(TAG, "openFile " + uri);

        String name = uri.getPath().substring(1);
        if (!name.equals(LOGS_FILE_NAME)) {
            Log.d(TAG, "not logs.txt! " + name);
            throw new FileNotFoundException("not logs.txt");
        }

        Log.d(TAG, "open " + name);
        return ParcelFileDescriptor.open(new File(this.getContext().getCacheDir(), name), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
