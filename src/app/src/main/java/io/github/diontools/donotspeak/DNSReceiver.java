package io.github.diontools.donotspeak;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;

public final class DNSReceiver extends BroadcastReceiver {
    private static final String TAG = "DNSReceiver";

    public static final  String ACTION_REBOOT = BuildConfig.APPLICATION_ID + ".REBOOT";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, intent != null ? intent.toString() : "null");
        String action = intent != null ? intent.getAction() : null;
        if (action == null) return;

        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED: {
                Log.d(TAG, "ACTION_BOOT_COMPLETED");
                IntentUtility.start(context);
                break;
            }
            case Intent.ACTION_MY_PACKAGE_REPLACED: {
                Log.d(TAG, "ACTION_MY_PACKAGE_REPLACED");
                IntentUtility.reboot(context, "ACTION_MY_PACKAGE_REPLACED");
                break;
            }
            case ACTION_REBOOT: {
                Log.d(TAG, "ACTION_REBOOT");
                if (DNSService.IsLive) {
                    NotificationInfo.setRebootTimer(
                            Compat.getSystemService(context, AlarmManager.class),
                            Compat.getSystemService(context, NotificationManager.class),
                            PendingIntent.getBroadcast(
                                    context.getApplicationContext(),
                                    NotificationInfo.REQUEST_CODE_REBOOT,
                                    new Intent(context.getApplicationContext(), DNSReceiver.class).setAction(ACTION_REBOOT),
                                    PendingIntent.FLAG_MUTABLE
                            )
                    );
                } else {
                    Log.d(TAG, "show reboot notification");
                    showRebootNotification(context);
                }
                break;
            }
        }
    }

    private static void showRebootNotification(Context context) {
        final NotificationManager notificationManager = Compat.getSystemService(context, NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        NotificationInfo.createNotificationChannels(context, notificationManager);

        // Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
        final int immutableFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;

        final PendingIntent startIntent =
                PendingIntent.getActivity(
                        context.getApplicationContext(),
                        NotificationInfo.REQUEST_CODE_REBOOT,
                        new Intent(context.getApplicationContext(), MainActivity.class),
                        PendingIntent.FLAG_CANCEL_CURRENT | immutableFlag);

        final Notification.Builder builder =
                Compat.createNotificationBuilder(context, NotificationInfo.ID_REBOOT)
                        .setSmallIcon(R.drawable.ic_volume_up_black_24dp)
                        .setContentTitle(context.getResources().getText(R.string.notification_reboot_content_title))
                        .setContentText(context.getResources().getText(R.string.notification_reboot_content_text))
                        .setColor(0xFF5419)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setContentIntent(startIntent);

        notificationManager.notify(NotificationInfo.REQUEST_CODE_REBOOT, builder.build());
    }
}
