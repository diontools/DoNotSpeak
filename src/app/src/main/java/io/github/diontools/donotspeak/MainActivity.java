package io.github.diontools.donotspeak;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.service.quicksettings.TileService;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TimePicker;

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
        Log.d(TAG, action);

        if (Objects.equals(action, ACTION_DISABLE_DIALOG)) {
            View view = this.getLayoutInflater().inflate(R.layout.disable_dialog_layout, null);

            final ImageButton menuButton = view.findViewById(R.id.menu_button);
            menuButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MainActivity.this.showPopupMenu(v);
                }
            });

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
            periodRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
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
                }
            });
            periodRadioGroup.check(R.id.minutesRadioButton);

            final Switch restoreVolumeSwitch = view.findViewById(R.id.restoreVolumeSwitch);
            restoreVolumeSwitch.setChecked(DNSSetting.getRestoreVolume(this));
            restoreVolumeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    DNSSetting.setRestoreVolume(MainActivity.this, isChecked);
                }
            });

            this.mainDialog =
                    new AlertDialog.Builder(this)
                            .setView(view)
                            .setPositiveButton(R.string.disable_alert_okButton, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
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
                                }
                            })
                            .setNeutralButton(R.string.disable_alert_untilScreenOffButton, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.stopUntilScreenOff();
                                }
                            })
                            .setNegativeButton(R.string.disable_alert_cancelButton, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    MainActivity.this.exit();
                                }
                            })
                            .setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    MainActivity.this.exit();
                                }
                            })
                            .create();
            this.mainDialog.show();
        } else if (Objects.equals(action, ACTION_STOP_UNTIL_SCREEN_OFF)) {
            this.stopUntilScreenOff();
        } else if (Objects.equals(action, TileService.ACTION_QS_TILE_PREFERENCES)) {
            this.stopUntilScreenOff();
        } else if (Objects.equals(action, DNSService.ACTION_SWITCH)) {
            IntentUtility.switching(this);
            this.exit();
        } else {
            Log.d(TAG, "start service!");
            IntentUtility.start(this);
            this.exit();
        }
    }

    private void stopUntilScreenOff() {
        IntentUtility.stopUntilScreenOff(this);
        this.exit();
    }

    private void exit() {
        this.finishAndRemoveTask();
        this.overridePendingTransition(0, 0);
    }

    private void requestStopApp() {
        AlertDialog dialog =
                new AlertDialog.Builder(this, R.style.DisableAnimationDialogTheme)
                .setTitle(R.string.stop_app_alert_title)
                .setMessage(R.string.stop_app_alert_message)
                .setPositiveButton(R.string.stop_app_alert_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        MainActivity.this.stopApp();
                    }
                })
                .setNegativeButton(R.string.disable_alert_cancelButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        dialog.show();
    }

    private void stopApp() {
        if (this.mainDialog.isShowing()) {
            this.mainDialog.dismiss();
        }
        IntentUtility.shutdown(this);
        this.exit();
        this.finish();
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.disable_alert_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.stop_app:
                        MainActivity.this.requestStopApp();
                        return true;
                }
                return false;
            }
        });

        popup.show();
    }
}
