package io.github.diontools.donotspeak;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import java.util.Date;

final class NotificationInfo {
    private static final String TAG = "NotificationInfo";

    public static final String ID_STATUS = "DoNotSpeak_Status_Notification";
    public static final int REQUEST_CODE_STATUS = 1;

    public static final String ID_REBOOT = "DoNotSpeak_Reboot_Notification";
    public static final int REQUEST_CODE_REBOOT = 2;


    public static void createNotificationChannels(Context context, NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        if (notificationManager.getNotificationChannel(NotificationInfo.ID_STATUS) == null) {
            NotificationChannel chan = new NotificationChannel(NotificationInfo.ID_STATUS, context.getResources().getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            chan.setShowBadge(false);
            notificationManager.createNotificationChannel(chan);
        }

        if (notificationManager.getNotificationChannel(NotificationInfo.ID_REBOOT) == null) {
            NotificationChannel chan = new NotificationChannel(NotificationInfo.ID_REBOOT, context.getResources().getString(R.string.notification_reboot_channel_name), NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.RED);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            chan.setShowBadge(true);
            chan.setImportance(NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(chan);
        }
    }

    public static void setRebootTimer(AlarmManager alarmManager, NotificationManager notificationManager, PendingIntent rebootIntent) {
        DiagnosticsLogger logger = DNSService.Logger;
        long nextTime = System.currentTimeMillis() + 60 * 60 * 1000;
        if (logger != null) logger.Log(TAG, "setRebootTimer " + DNSService.DateFormat.format(new Date(nextTime)));
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextTime, rebootIntent);
        notificationManager.cancel(NotificationInfo.REQUEST_CODE_REBOOT);
    }

    public static void cancelRebootTimer(AlarmManager alarmManager, PendingIntent rebootIntent) {
        DiagnosticsLogger logger = DNSService.Logger;
        if (logger != null) logger.Log(TAG, "cancelRebootTimer");
        alarmManager.cancel(rebootIntent);
    }
}
