package io.github.diontools.donotspeak;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
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
    private CheckBox useNotificationCheckBox;
    private CheckBox useBluetoothCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.settings_title);
        this.setContentView(R.layout.activity_setting);

        this.useAdjustVolumeCheckBox = this.findViewById(R.id.use_adjust_volume_check_box);
        this.useAdjustVolumeCheckBox.setOnCheckedChangeListener(this);

        this.useNotificationCheckBox = this.findViewById(R.id.use_notification_check_box);
        this.useNotificationCheckBox.setOnCheckedChangeListener(this);
        this.useNotificationCheckBox.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? View.VISIBLE : View.GONE);

        this.useBluetoothCheckBox = this.findViewById(R.id.use_bluetooth_check_box);
        this.useBluetoothCheckBox.setOnCheckedChangeListener(this);
        this.useBluetoothCheckBox.setVisibility(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? View.VISIBLE : View.GONE);

        this.items = new ArrayList<>();
        this.itemAdapter = new ItemAdapter(this, this.items);
        ListView listView = this.findViewById(R.id.device_list_view);
        listView.setAdapter(this.itemAdapter);
        listView.setOnItemClickListener(this);

        this.loadSettings();
    }

    private void loadSettings()
    {
        this.useAdjustVolumeCheckBox.setChecked(DNSSetting.getUseAdjustVolume(this));
        this.useNotificationCheckBox.setChecked(DNSSetting.getUseNotification(this));
        this.useBluetoothCheckBox.setChecked(DNSSetting.getUseBluetooth(this));
        this.refreshListItems();
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
        } else if (id == R.id.use_notification_check_box) {
            if (DNSSetting.getUseNotification(this) != isChecked) {
                DNSSetting.setUseNotification(this, isChecked);
                if (isChecked) {
                    PermissionUtility.requestPostNotificationsPermissionIfRequired(
                            this,
                            result -> {
                                DNSSetting.setUseNotification(this, PermissionUtility.isPostNotificationsGranted(this));
                                IntentUtility.applySettings(this);
                                this.loadSettings();
                            }
                    );
                }
            }
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