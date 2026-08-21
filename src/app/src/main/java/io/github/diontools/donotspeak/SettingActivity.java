package io.github.diontools.donotspeak;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SettingActivity extends Activity implements AdapterView.OnItemClickListener, CompoundButton.OnCheckedChangeListener {
    private ArrayList<Item> items;
    private ItemAdapter itemAdapter;
    private CheckBox useAdjustVolumeCheckBox;
    private CheckBox restoreVolumeOnHeadphoneConnectCheckBox;
    private CheckBox useBluetoothCheckBox;
    private View notificationSettingsSection;
    private TextView notificationStatusTextView;
    private Button notificationSettingsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.settings_title);
        this.setContentView(R.layout.activity_setting);

        this.useAdjustVolumeCheckBox = this.findViewById(R.id.use_adjust_volume_check_box);
        this.useAdjustVolumeCheckBox.setOnCheckedChangeListener(this);

        this.restoreVolumeOnHeadphoneConnectCheckBox = this.findViewById(R.id.restore_volume_on_headphone_connect_check_box);
        this.restoreVolumeOnHeadphoneConnectCheckBox.setOnCheckedChangeListener(this);

        this.notificationSettingsSection = this.findViewById(R.id.notification_settings_section);
        this.notificationStatusTextView = this.findViewById(R.id.notification_status_text_view);
        this.notificationSettingsButton = this.findViewById(R.id.notification_settings_button);
        this.notificationSettingsSection.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? View.VISIBLE : View.GONE);
        this.notificationSettingsButton.setOnClickListener(view -> this.onNotificationSettingsClicked());

        this.useBluetoothCheckBox = this.findViewById(R.id.use_bluetooth_check_box);
        this.useBluetoothCheckBox.setOnCheckedChangeListener(this);
        this.useBluetoothCheckBox.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? View.VISIBLE : View.GONE);

        this.items = new ArrayList<>();
        this.itemAdapter = new ItemAdapter(this, this.items);
        ListView listView = this.findViewById(R.id.device_list_view);
        listView.setAdapter(this.itemAdapter);
        listView.setOnItemClickListener(this);

        Compat.applySystemBarInsets(this.findViewById(R.id.root_layout), listView);

        this.loadSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.refreshNotificationSettings();
    }

    private void loadSettings()
    {
        this.useAdjustVolumeCheckBox.setChecked(DNSSetting.getUseAdjustVolume(this));
        this.restoreVolumeOnHeadphoneConnectCheckBox.setChecked(DNSSetting.getRestoreVolumeOnHeadphoneConnect(this));
        this.useBluetoothCheckBox.setChecked(DNSSetting.getUseBluetooth(this));
        this.refreshListItems();
    }

    private void onNotificationSettingsClicked() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (PermissionUtility.isPostNotificationsGranted(this)) {
            PermissionUtility.openNotificationSettings(this);
            return;
        }

        DNSSetting.setUseNotification(this, true);
        PermissionUtility.requestPostNotificationsPermissionIfRequired(this, result -> {
            switch (result) {
                case Granted:
                    DNSSetting.setUseNotification(this, true);
                    break;
                case Denied:
                    DNSSetting.setUseNotification(this, false);
                    break;
            }
            this.refreshNotificationSettings();
        });
    }

    private void refreshNotificationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        NotificationManager notificationManager = Compat.getSystemService(this, NotificationManager.class);
        String unavailable = this.getString(R.string.settings_notification_state_unavailable);
        String appState = unavailable;
        String statusChannelState = unavailable;
        String rebootChannelState = unavailable;
        if (notificationManager != null) {
            appState = this.getString(notificationManager.areNotificationsEnabled() ? R.string.settings_notification_state_enabled : R.string.settings_notification_state_disabled);
            statusChannelState = this.getNotificationChannelState(notificationManager, NotificationInfo.ID_STATUS);
            rebootChannelState = this.getNotificationChannelState(notificationManager, NotificationInfo.ID_REBOOT);
        }
        this.notificationStatusTextView.setText(this.getString(R.string.settings_notification_status, appState, statusChannelState, rebootChannelState));
        this.notificationSettingsButton.setText(PermissionUtility.isPostNotificationsGranted(this) ? R.string.settings_notification_open_settings : R.string.settings_notification_allow);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String getNotificationChannelState(NotificationManager notificationManager, String channelId) {
        NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
        if (channel == null) return this.getString(R.string.settings_notification_state_not_created);
        return this.getString(channel.getImportance() == NotificationManager.IMPORTANCE_NONE ? R.string.settings_notification_state_disabled : R.string.settings_notification_state_enabled);
    }

    private void refreshListItems()
    {
        this.items.clear();

        Set<String> addresses = DNSSetting.getBluetoothHeadsetAddresses(this);
        for (String address : addresses) {
            this.items.add(new Item(address, true));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        ) {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
                for (BluetoothDevice device : devices) {
                    String name = device.getName();
                    String address = device.getAddress();

                    Item foundItem = null;
                    for (Item item : this.items) {
                        if (item.address.equals(address)) {
                            foundItem = item;
                            break;
                        }
                    }

                    if (foundItem != null) {
                        foundItem.name = name;
                    } else {
                        this.items.add(new Item(address, name, false));
                    }
                }
            }
        }

        this.itemAdapter.notifyDataSetChanged();
    }

    // CompoundButton.OnCheckedChangeListener
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final int id = buttonView.getId();
        if (id == R.id.use_adjust_volume_check_box) {
            DNSSetting.setUseAdjustVolume(this, isChecked);
            IntentUtility.applySettings(this);
        } else if (id == R.id.restore_volume_on_headphone_connect_check_box) {
            DNSSetting.setRestoreVolumeOnHeadphoneConnect(this, isChecked);
            IntentUtility.applySettings(this);
        } else if (id == R.id.use_bluetooth_check_box) {
            if (DNSSetting.getUseBluetooth(this) != isChecked) {
                DNSSetting.setUseBluetooth(this, isChecked);
                if (isChecked) {
                    PermissionUtility.requestBluetoothPermissionIfRequired(
                            this,
                            result -> {
                                DNSSetting.setUseBluetooth(this, PermissionUtility.isBluetoothGranted(this));
                                IntentUtility.applySettings(this);
                                this.loadSettings();
                            }
                    );
                } else {
                    IntentUtility.applySettings(this);
                }
            }
        }
    }

    // AdapterView.OnItemClickListener
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        {
            Item item = this.items.get(position);
            item.isChecked = !item.isChecked;
            this.itemAdapter.notifyDataSetChanged();
        }

        HashSet<String> addresses = new HashSet<>();
        for (Item item : this.items) {
            if (item.isChecked) {
                addresses.add(item.address);
            }
        }

        DNSSetting.setBlueToothHeadsetAddresses(this, addresses);
        IntentUtility.applySettings(this);
    }

    private static class Item {
        public Item(String address, boolean isChecked) {
            this(address, null, isChecked);
        }

        public Item(String address, String name, boolean isChecked) {
            this.address = address;
            this.name = name;
            this.isChecked = isChecked;
        }

        public final String address;
        public String name;
        public boolean isChecked;
    }

    private static class ItemAdapter extends BaseAdapter {
        private final Activity context;
        private final ArrayList<Item> items;

        public ItemAdapter(Activity context, ArrayList<Item> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return this.items.size();
        }

        @Override
        public Item getItem(int position) {
            return this.items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.context.getLayoutInflater().inflate(R.layout.setting_device_item, parent, false);
            }

            Item item = this.getItem(position);

            CheckBox checkBox = convertView.findViewById(R.id.checkBox);
            checkBox.setChecked(item.isChecked);
            checkBox.setText(item.address);

            TextView nameTextView = convertView.findViewById(R.id.name_textView);
            nameTextView.setText(item.name);

            return convertView;
        }
    }
}
