package io.github.diontools.donotspeak;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class DiagnosticsActivity extends Activity {
    private static final String TAG = "DiagnosticsActivity";

    private final ArrayList<String> logs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        this.setTitle(R.string.diagnostics_tool_title);
        this.setContentView(R.layout.activity_diagnostics);

        this.logs.add("APPLICATION_ID: " + BuildConfig.APPLICATION_ID);
        this.logs.add("BUILD_TYPE: " + BuildConfig.BUILD_TYPE);
        this.logs.add("VERSION_NAME: " + BuildConfig.VERSION_NAME);
        this.logs.add("VERSION_CODE: " + BuildConfig.VERSION_CODE);
        this.logs.add("ID: " + Build.ID);
        this.logs.add("DISPLAY: " + Build.DISPLAY);
        this.logs.add("PRODUCT: " + Build.PRODUCT);
        this.logs.add("DEVICE: " + Build.DEVICE);
        this.logs.add("BOARD: " + Build.BOARD);
        this.logs.add("MANUFACTURER: " + Build.MANUFACTURER);
        this.logs.add("BRAND: " + Build.BRAND);
        this.logs.add("MODEL: " + Build.MODEL);
        this.logs.add("BOOTLOADER: " + Build.BOOTLOADER);
        this.logs.add("HARDWARE: " + Build.HARDWARE);
        this.logs.add("INCREMENTAL: " + Build.VERSION.INCREMENTAL);
        this.logs.add("RELEASE: " + Build.VERSION.RELEASE);
        this.logs.add("SDK_INT: " + Build.VERSION.SDK_INT);
        this.logs.add("CODENAME: " + Build.VERSION.CODENAME);

        final ArrayAdapter<String> logArrayAdapter = new ArrayAdapter<>(this, R.layout.diagnostics_log_item, this.logs);

        final ListView logListView = this.findViewById(R.id.log_list_view);
        logListView.setAdapter(logArrayAdapter);

        DiagnosticsLogger logger = DiagnosticsLogger.Instance;
        logger.setCallback(str -> {
            logs.add(str);
            logArrayAdapter.notifyDataSetChanged();
        });

        DNSService.Logger = logger;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.getMenuInflater().inflate(R.menu.diagnostics_menu, menu);
        MenuItem logFileMenuItem = menu.findItem(R.id.log_file);
        logFileMenuItem.setChecked(DNSSetting.getDiagnosticsFileLog(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.send_mail) {
                String logText = TextUtils.join("\r\n", this.logs);
                DiagnosticsContentProvider.writeLogFile(this, logText);

                Uri logUri = DiagnosticsContentProvider.getLogFileUri();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"diontools.dev@gmail.com"});
                intent.putExtra(Intent.EXTRA_SUBJECT, this.getResources().getString(R.string.diagnostics_tool_mail_subject));
                intent.putExtra(Intent.EXTRA_TEXT, this.getResources().getString(R.string.diagnostics_tool_mail_text) + "\r\n");
                intent.putExtra(Intent.EXTRA_TITLE, this.getResources().getString(R.string.diagnostics_tool_mail_title));
                intent.putExtra(Intent.EXTRA_STREAM, logUri);
                intent.setData(logUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
                Log.d(TAG, "send email");
                this.startActivity(Intent.createChooser(intent, this.getResources().getString(R.string.diagnostics_tool_mail_chooser_title)));
            } else if (itemId == R.id.log_file) {
                boolean useFileLog = !DNSSetting.getDiagnosticsFileLog(this);
                DNSSetting.setDiagnosticsFileLog(this, useFileLog);
                item.setChecked(useFileLog);
                DiagnosticsLogger.Instance.setFileDir(this, useFileLog);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
            ex.printStackTrace();
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        DiagnosticsLogger.Instance.setCallback(null);
        if (!BuildConfig.DEBUG && !DNSSetting.getDiagnosticsFileLog(this)) {
            DNSService.Logger = null;
        }
    }
}