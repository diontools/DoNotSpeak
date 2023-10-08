package io.github.diontools.donotspeak;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

final class NotificationInfo {
    public static final String ID_STATUS = "DoNotSpeak_Status_Notification";
    public static final int REQUEST_CODE_STATUS = 1;

    public static final String ID_REBOOT = "DoNotSpeak_Reboot_Notification";
    public static final int REQUEST_CODE_REBOOT = 2;

    public static final int REQUEST_CODE_TILE = 3;


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
            chan.setImportance(NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(chan);
        }
    }
}
