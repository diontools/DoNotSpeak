package io.github.diontools.donotspeak;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

final class PermissionUtility {
    private static final String TAG = "PermissionUtility";

    public static boolean isBluetoothGranted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isPostNotificationsGranted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestBluetoothPermissionIfRequired(Activity context, Consumer<RequestResult> onCompleted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            final boolean useBluetooth = DNSSetting.getUseBluetooth(context);
            Log.d(TAG, "useBluetooth: " + useBluetooth);

            if (useBluetooth) {
                requestPermissionIfRequired(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        onCompleted,
                        (builder, requestPermission) -> builder
                                .setTitle(R.string.permission_dialog_bluetooth_title)
                                .setMessage(R.string.permission_dialog_bluetooth_message)
                                .setPositiveButton(R.string.permission_dialog_bluetooth_positive, (dialog, which) -> requestPermission.run())
                                .setNegativeButton(R.string.permission_dialog_bluetooth_negative, (dialog, which) -> onCompleted.accept(RequestResult.Denied)),
                        builder -> builder
                                .setTitle(R.string.permission_dialog_bluetooth_denied_title)
                                .setMessage(R.string.permission_dialog_bluetooth_denied_message)
                                .setPositiveButton(R.string.permission_dialog_bluetooth_denied_positive, (dialog, which) -> {
                                    openApplicationDetailsSettings(context);
                                    onCompleted.accept(RequestResult.None);
                                })
                                .setNegativeButton(R.string.permission_dialog_bluetooth_denied_negative, (dialog, which) -> onCompleted.accept(RequestResult.Denied))
                                .setOnCancelListener(dialog -> onCompleted.accept(RequestResult.None))
                );
                return;
            }
        }

        onCompleted.accept(RequestResult.None);
    }

    public static void requestPostNotificationsPermissionIfRequired(Activity context, Consumer<RequestResult> onCompleted) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            final boolean useNotification = DNSSetting.getUseNotification(context);
            Log.d(TAG, "useNotification: " + useNotification);

            if (useNotification) {
                requestPermissionIfRequired(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                        onCompleted,
                        (builder, requestPermission) -> builder
                                .setTitle(R.string.permission_dialog_notification_title)
                                .setMessage(R.string.permission_dialog_notification_message)
                                .setPositiveButton(R.string.permission_dialog_notification_positive, (dialog, which) -> requestPermission.run())
                                .setNegativeButton(R.string.permission_dialog_notification_negative, (dialog, which) -> onCompleted.accept(RequestResult.Denied)),
                        builder -> builder
                                .setTitle(R.string.permission_dialog_notification_denied_title)
                                .setMessage(R.string.permission_dialog_notification_denied_message)
                                .setPositiveButton(R.string.permission_dialog_notification_denied_positive, (dialog, which) -> {
                                    openApplicationDetailsSettings(context);
                                    onCompleted.accept(RequestResult.None);
                                })
                                .setNegativeButton(R.string.permission_dialog_notification_denied_negative, (dialog, which) -> onCompleted.accept(RequestResult.Denied))
                                .setOnCancelListener(dialog -> onCompleted.accept(RequestResult.None))
                );
                return;
            }
        }

        onCompleted.accept(RequestResult.None);
    }

    private static void openApplicationDetailsSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private static void requestPermissionIfRequired(
            Activity context,
            String permission,
            Consumer<RequestResult> onCompleted,
            BiFunction<AlertDialog.Builder, Runnable, AlertDialog.Builder> onConfirmDialogBuild,
            Function<AlertDialog.Builder, AlertDialog.Builder> onDeniedDialogBuild
    ) {
        if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "already granted: " + permission);
            onCompleted.accept(RequestResult.Granted);
            return;
        }

        if (context.shouldShowRequestPermissionRationale(permission)) {
            onDeniedDialogBuild.apply(new AlertDialog.Builder(context))
                    .show();
        } else {
            new PermissionDialogFragment()
                    .set(permission, onCompleted, onConfirmDialogBuild)
                    .show(context.getFragmentManager(), TAG);
        }
    }

    public enum RequestResult {
        None,
        Canceled,
        Denied,
        Granted,
    }

    @SuppressWarnings("deprecation")
    public static class PermissionDialogFragment extends DialogFragment
    {
        private String permission;
        private Consumer<RequestResult> onCompleted;
        private BiFunction<AlertDialog.Builder, Runnable, AlertDialog.Builder> onConfirmDialogBuild;

        public PermissionDialogFragment set(String permission, Consumer<RequestResult> onCompleted, BiFunction<AlertDialog.Builder, Runnable, AlertDialog.Builder> onConfirmDialogBuild) {
            this.permission = permission;
            this.onCompleted = onCompleted;
            this.onConfirmDialogBuild = onConfirmDialogBuild;
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return
                    this.onConfirmDialogBuild.apply(
                        new AlertDialog.Builder(this.getActivity()),
                        () -> this.requestPermissions(new String[]{this.permission}, 1)
                    )
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            Log.d(TAG, "onCancel");
            this.onCompleted.accept(RequestResult.Canceled);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            Log.d(TAG, "onDismiss");
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            Log.d(TAG, "onRequestPermissionsResult " + requestCode + permissions.length + " " + grantResults.length);
            if (requestCode == 1) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                    this.onCompleted.accept(RequestResult.Granted);
                } else {
                    Log.d(TAG, "permission denied");
                    this.onCompleted.accept(RequestResult.Denied);
                }
            }
        }
    }
}
