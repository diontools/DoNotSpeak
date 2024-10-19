package io.github.diontools.donotspeak;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

public final class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    public static final String ACTION_DISABLE_DIALOG = "DISABLE_DIALOG";
    public static final String ACTION_STOP_UNTIL_SCREEN_OFF = "STOP_UNTIL_SCREEN_OFF";

    private AlertDialog mainDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        //setContentView(R.layout.activity_main)

        // Allow show dialog on locked screen
        KeyguardManager keyguardManager = Compat.getSystemService(this, KeyguardManager.class);
        if (keyguardManager != null && keyguardManager.isKeyguardLocked()) {
            Log.d(TAG, "show when locked");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                keyguardManager.requestDismissKeyguard(this, null);
            } else {
                this.getWindow().addFlags(
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            }
        }

        String action = this.getIntent().getAction();
        Log.d(TAG, "action: " + action);

        if (Objects.equals(action, ACTION_DISABLE_DIALOG)) {
            View view = this.getLayoutInflater().inflate(R.layout.disable_dialog_layout, null);

            final ImageButton menuButton = view.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(MainActivity.this::showPopupMenu);

            final NumberPicker numPicker = view.findViewById(R.id.numberPicker);
            final String[] values = new String[24];
            for (int i = 0; i < values.length; i++) values[i] = String.valueOf((i + 1) * 5);
            numPicker.setMinValue(0);
            numPicker.setMaxValue(values.length - 1);
            numPicker.setDisplayedValues(values);

            final TimePicker timePicker = view.findViewById(R.id.timePicker);
            timePicker.setIs24HourView(DateFormat.is24HourFormat(this));

            final RadioGroup periodRadioGroup = view.findViewById(R.id.periodRadioGroup);
            final View minutesView = view.findViewById(R.id.minutesView);
            final View timeOfDayView = view.findViewById(R.id.timeOfDayView);
            periodRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                minutesView.setVisibility(checkedId == R.id.minutesRadioButton ? View.VISIBLE : View.GONE);
                timeOfDayView.setVisibility(checkedId == R.id.timeOfDayRadioButton ? View.VISIBLE : View.GONE);

                // init time by selected number picker value
                if (checkedId == R.id.timeOfDayRadioButton) {
                    final Calendar calendar = Calendar.getInstance();
                    int totalMinutes = (numPicker.getValue() + 1) * 5;
                    calendar.add(Calendar.MINUTE, totalMinutes);
                    timePicker.setCurrentHour(calendar.get(Calendar.HOUR_OF_DAY));
                    timePicker.setCurrentMinute(calendar.get(Calendar.MINUTE));
                }
            });
            periodRadioGroup.check(R.id.minutesRadioButton);

            final Switch restoreVolumeSwitch = view.findViewById(R.id.restoreVolumeSwitch);
            restoreVolumeSwitch.setChecked(DNSSetting.getRestoreVolume(this));
            restoreVolumeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> DNSSetting.setRestoreVolume(MainActivity.this, isChecked));

            final Switch requestToStopPlaybackSwitch = view.findViewById(R.id.requestToStopPlaybackSwitch);
            requestToStopPlaybackSwitch.setChecked(DNSSetting.getRequestToStopPlayback(this));
            requestToStopPlaybackSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> DNSSetting.setRequestToStopPlayback(MainActivity.this, isChecked));

            final Switch keepScreenOnSwitch = view.findViewById(R.id.keepScreenOnSwitch);
            keepScreenOnSwitch.setChecked(DNSSetting.getKeepScreenOn(this));
            keepScreenOnSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                DNSSetting.setKeepScreenOn(MainActivity.this, isChecked);
                IntentUtility.applySettings(MainActivity.this);
            });

            this.mainDialog =
                    new AlertDialog.Builder(this)
                            .setView(view)
                            .setPositiveButton(R.string.disable_alert_okButton, (dialog, which) -> {
                                boolean useMinutes = periodRadioGroup.getCheckedRadioButtonId() == R.id.minutesRadioButton;
                                Date disableTime;
                                if (useMinutes) {
                                    int minutes = (numPicker.getValue() + 1) * 5;
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.add(Calendar.MINUTE, minutes);
                                    disableTime = calendar.getTime();
                                } else {
                                    Calendar currentCalendar = Calendar.getInstance();
                                    Calendar disableTimeCalender = Calendar.getInstance();
                                    disableTimeCalender.clear();
                                    disableTimeCalender.set(
                                            currentCalendar.get(Calendar.YEAR),
                                            currentCalendar.get(Calendar.MONTH),
                                            currentCalendar.get(Calendar.DATE),
                                            timePicker.getCurrentHour(),
                                            timePicker.getCurrentMinute());

                                    // add one day when disableTime < now
                                    if (disableTimeCalender.before(currentCalendar)) {
                                        disableTimeCalender.add(Calendar.DATE, 1);
                                    }

                                    disableTime = disableTimeCalender.getTime();
                                }

                                IntentUtility.stop(MainActivity.this, disableTime);
                                MainActivity.this.exit();
                            })
                            .setNeutralButton(R.string.disable_alert_untilScreenOffButton, (dialog, which) -> MainActivity.this.stopUntilScreenOff())
                            .setNegativeButton(R.string.disable_alert_cancelButton, (dialog, which) -> MainActivity.this.exit())
                            .setOnDismissListener(dialog -> MainActivity.this.exit())
                            .create();
            this.mainDialog.show();
        } else if (Objects.equals(action, ACTION_STOP_UNTIL_SCREEN_OFF)) {
            this.stopUntilScreenOff();
        } else if (Objects.equals(action, TileService.ACTION_QS_TILE_PREFERENCES)) {
            this.stopUntilScreenOff();
        } else if (Objects.equals(action, DNSService.ACTION_SWITCH)) {
            IntentUtility.switching(this);
            this.exit();
        } else if (Objects.equals(action, DNSService.ACTION_SHUTDOWN)) {
            this.requestStopApp();
        } else {
            PermissionUtility.requestBluetoothPermissionIfRequired(this, result -> {
                switch (result) {
                    case Granted:
                        DNSSetting.setUseBluetooth(this, true);
                        break;
                    case Denied:
                        DNSSetting.setUseBluetooth(this, false);
                        break;
                }

                PermissionUtility.requestPostNotificationsPermissionIfRequired(this, result2 -> {
                    switch (result2) {
                        case Granted:
                            DNSSetting.setUseNotification(this, true);
                            break;
                        case Denied:
                            DNSSetting.setUseNotification(this, false);
                            break;
                    }

                    Log.d(TAG, "start service!");
                    IntentUtility.start(this);
                    this.exit();
                });
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        this.exit();
    }

    private void stopUntilScreenOff() {
        IntentUtility.stopUntilScreenOff(this);
        this.exit();
    }

    private void exit() {
        Log.d(TAG, "exit");
        if (this.mainDialog != null && this.mainDialog.isShowing()) {
            this.mainDialog.dismiss();
        }
        this.finishAndRemoveTask();
        this.overridePendingTransition(0, 0);
    }

    private void requestStopApp() {
        AlertDialog dialog =
                new AlertDialog.Builder(this, R.style.DisableAnimationDialogTheme)
                .setTitle(R.string.stop_app_alert_title)
                .setMessage(R.string.stop_app_alert_message)
                .setPositiveButton(R.string.stop_app_alert_ok, (dialog1, which) -> MainActivity.this.stopApp())
                .setNegativeButton(R.string.disable_alert_cancelButton, (dialog12, which) -> {})
                .create();
        dialog.show();
    }

    private void stopApp() {
        IntentUtility.shutdown(this);
        this.exit();
        this.finish();
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.disable_alert_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.stop_app) {
                MainActivity.this.requestStopApp();
                return true;
            } else if (itemId == R.id.diagnostics) {
                MainActivity.this.startActivity(new Intent(MainActivity.this, DiagnosticsActivity.class));
                MainActivity.this.exit();
                return true;
            } else if (itemId == R.id.setting) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Toast.makeText(MainActivity.this, R.string.disable_alert_menu_settings_not_supported_message, Toast.LENGTH_SHORT).show();
                } else {
                    MainActivity.this.startActivity(new Intent(MainActivity.this, SettingActivity.class));
                }
                return true;
            }
            return false;
        });

        popup.show();
    }
}
