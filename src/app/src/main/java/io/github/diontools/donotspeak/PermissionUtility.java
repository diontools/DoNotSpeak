package io.github.diontools.donotspeak;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.Toast;

import java.util.function.Consumer;

final class PermissionUtility {
    private static final String TAG = "PermissionUtility";

    public static boolean isBluetoothPermissionRequired(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S;
    }

    public static boolean isBluetoothGranted(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestBluetoothPermissionIfRequired(Activity context, Consumer<RequestResult> onCompleted) {
        if (isBluetoothPermissionRequired(context)) {
            final Boolean useBluetooth = DNSSetting.getUseBluetooth(context);
            Log.d(TAG, "useBluetooth: " + useBluetooth);

            if (useBluetooth == null || (useBluetooth && !isBluetoothGranted(context))) {
                if (context.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                    new AlertDialog.Builder(context)
                            .setTitle("Bluetooth権限が無効")
                            .setMessage("Bluetooth権限が無効になっています。システム設定からDoNotSpeakのBluetooth権限(付近のデバイス)を許可してください。")
                            .setPositiveButton("設定を開く", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);
                                onCompleted.accept(RequestResult.None);
                            })
                            .setNegativeButton("Bluetoothを使用しない", (dialog, which) -> {
                                onCompleted.accept(RequestResult.NotAllowed);
                            })
                            .setOnCancelListener(dialog -> {
                                onCompleted.accept(RequestResult.None);
                            })
                            .show();
                    return;
                } else {
                    showConfirmBluetoothPermissionDialog(context, onCompleted);
                    return;
                }
            }
        }

        onCompleted.accept(RequestResult.None);
    }

    @RequiresApi(Build.VERSION_CODES.S)
    public static void showConfirmBluetoothPermissionDialog(Activity context, Consumer<RequestResult> onCompleted) {
        Log.d(TAG, "showConfirmBluetoothPermission");
        new PermissionDialogFragment(onCompleted).show(context.getFragmentManager(), TAG);
    }

    public enum RequestResult {
        None,
        Canceled,
        NotAllowed,
        Granted,
    }

    @SuppressWarnings("deprecation") @SuppressLint("ValidFragment")
    public static class PermissionDialogFragment extends DialogFragment
    {
        private final Consumer<RequestResult> onCompleted;

        public PermissionDialogFragment(Consumer<RequestResult> onCompleted)
        {
            this.onCompleted = onCompleted;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(this.getActivity())
                    .setTitle("Bluetoothイヤホンを使用しますか？")
                    .setMessage("Bluetoothイヤホンを使用する場合はBluetoothデバイスにアクセスする権限をアプリに許可する必要があります。")
                    .setPositiveButton("使用する", (dialog, which) -> {
                        requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
                    })
                    .setNegativeButton("使用しない", (dialog, which) -> {
                        this.onCompleted.accept(RequestResult.NotAllowed);
                    })
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
                    Log.d(TAG, "Bluetooth permission: granted");
                    this.onCompleted.accept(RequestResult.Granted);
                } else {
                    Log.d(TAG, "Bluetooth permission: denied");
                    this.onCompleted.accept(RequestResult.NotAllowed);
                }
            }
        }
    }
}
