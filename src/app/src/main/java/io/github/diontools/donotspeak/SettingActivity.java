package io.github.diontools.donotspeak;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class SettingActivity extends Activity implements AdapterView.OnItemClickListener {
    private ArrayList<Item> items;
    private ItemAdapter itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle(R.string.settings_title);
        this.setContentView(R.layout.activity_setting);

        this.items = new ArrayList<>();
        Set<String> addresses = DNSSetting.getBluetoothHeadsetAddresses(this);
        for (String address : addresses) {
            this.items.add(new Item(address, true));
        }

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

        this.itemAdapter = new ItemAdapter(this, this.items);
        ListView listView = this.findViewById(R.id.device_list_view);
        listView.setAdapter(this.itemAdapter);
        listView.setOnItemClickListener(this);
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