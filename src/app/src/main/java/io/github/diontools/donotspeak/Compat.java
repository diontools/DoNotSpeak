package io.github.diontools.donotspeak;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;

final class Compat {
    static void startForegroundService(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    static <T> T getSystemService(Context context, Class<T> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemService(serviceClass);
        }
        String serviceName = getSystemServiceName(context, serviceClass);
        return serviceName != null ? (T) context.getSystemService(serviceName) : null;
    }

    static String getSystemServiceName(Context context, Class<?> serviceClass) {
        if (Build.VERSION.SDK_INT >= 23) {
            return context.getSystemServiceName(serviceClass);
        }

        if (serviceClass == AudioManager.class) return Context.AUDIO_SERVICE;
        if (serviceClass == NotificationManager.class) return Context.NOTIFICATION_SERVICE;
        if (serviceClass == AlarmManager.class) return Context.ALARM_SERVICE;
        throw new Resources.NotFoundException();
    }

    static Notification.Builder createNotificationBuilder(Context context, String id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Notification.Builder(context, id);
        }
        return new Notification.Builder(context);
    }
}
