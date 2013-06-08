package com.github.vicos.nopin;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends ListActivity {

    // Unpublished constants
    public static final String BTDEVICE_ACTION_PAIRING_REQUEST =
            "android.bluetooth.device.action.PAIRING_REQUEST";

    ArrayAdapter<BluetoothDeviceItem> mListItems = null;

    BluetoothAdapter mAdapter = null;
    Object mService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListItems = new ArrayAdapter<BluetoothDeviceItem>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<BluetoothDeviceItem>());
        setListAdapter(mListItems);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        assert(mAdapter != null);

        try {
            mService = getService(mAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!mAdapter.isEnabled())
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_LONG).show();
        else
            loadBTDeviceList(mAdapter.getBondedDevices());

        registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mReceiver, new IntentFilter(BTDEVICE_ACTION_PAIRING_REQUEST));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    protected void startBTSearching() {
        if(!mAdapter.isDiscovering()) {
            mAdapter.startDiscovery();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_discover:
                startBTSearching();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void loadBTDeviceList(Set<BluetoothDevice> devices) {
        mListItems.clear();
        for(BluetoothDevice device: devices)
            addBTDeviceItem(new BluetoothDeviceItem(device));
    }

    protected void addBTDeviceItem(BluetoothDeviceItem deviceItem) {
        mListItems.add(deviceItem);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        // Do something when a list item is clicked
        BluetoothDeviceItem item = (BluetoothDeviceItem)l.getItemAtPosition(position);
        assert(item.device != null);

        try {
            createBond(mService, item.device);
            //byte pinCode[] = {};
            //setPin(mService, item.device, true, 0, pinCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, device.getName() + " founded", Toast.LENGTH_SHORT).show();
                addBTDeviceItem(new BluetoothDeviceItem(device));
            }
            else if (BTDEVICE_ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "received request from " + device.getName(), Toast.LENGTH_SHORT).show();
                try {
                    byte pinCode[] = { 0x00 };
                    setPin(mService, device, true, 1, pinCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private class BluetoothDeviceItem {
        public BluetoothDevice device = null;

        public BluetoothDeviceItem(BluetoothDevice _device) {
            device = _device;
        }

        public String toString() {
            return(device.getName() + "\n" + device.getAddress());
        }
    }

    // Unpublished methods

    public Object getService(BluetoothAdapter adapter)
            throws ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Class cAdapter = Class.forName("android.bluetooth.BluetoothAdapter");
        //Class cIBluetooth = Class.forName("android.bluetooth.IBluetooth");
        Class cIBluetoothCallback = Class.forName("android.bluetooth.IBluetoothManagerCallback");

        Class cArgs[] = { cIBluetoothCallback };
        Method getBluetoothService = cAdapter.getDeclaredMethod("getBluetoothService", cArgs);
        getBluetoothService.setAccessible(true); // have fun!

        Object[] args = { null };
        return getBluetoothService.invoke(adapter, args);
    }

    public Object createBond(Object service ,BluetoothDevice device)
            throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {
        Class cIBluetooth = Class.forName("android.bluetooth.IBluetooth");
        Class cArgs[] = { BluetoothDevice.class };
        Method createBond = cIBluetooth.getMethod("createBond", cArgs);

        Object[] args = { device };
        return createBond.invoke(service, args);
    }

    Object setPin(Object service, BluetoothDevice device, boolean accept, int len, byte[] pinCode)
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class cIBluetooth = Class.forName("android.bluetooth.IBluetooth");
        Class cArgs[] = { BluetoothDevice.class, boolean.class, int.class, byte[].class };
        Method setPin = cIBluetooth.getMethod("setPin", cArgs);

        Object[] args = { device, accept, len, pinCode };
        return setPin.invoke(service, args);
    }
}