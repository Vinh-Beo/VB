package com.gmurru.bleframework;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.pm.PackageManager;

import android.location.Address;
import android.os.Build;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;

import android.support.annotation.IntRange;
import android.support.annotation.MainThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import com.unity3d.player.UnityPlayer;

import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import android.app.Activity;
import android.widget.Toast;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * Created by Fenix on 24/09/2017.
 */

public class BleFramework {
    private Activity _unityActivity;
    /*
    Singleton instance.
    */
    private static volatile BleFramework _instance;


    /*
    Static variables
    */
    private static final String TAG = BleFramework.class.getSimpleName();
    private static final long SCAN_PERIOD = 1000;
    private Handler mHandler;

    /*
    List containing all the discovered bluetooth devices
    */
    private List<BluetoothDevice> listBluetoothDevice = new ArrayList<BluetoothDevice>();

    /*
    The latest received data
    */


    private String dataTx="";
    private String dataFullTx="{1,0,0,0}";

    private String value = "Getdata";

    private BroadcastReceiver mReceiver;
    /*
    Bluetooth service
    */
    private RBLService mBluetoothLeService;
    private BluetoothGatt mBluetoothGatt;

    private Map<UUID, BluetoothGattCharacteristic> _map = new HashMap<UUID, BluetoothGattCharacteristic>();

    /*
    Bluetooth adapter
    */
    private BluetoothAdapter _mBluetoothAdapter;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    /*
    Bluetooth device address and name to which the app is currently connected
    */
    private BluetoothDevice _device;
    private String _mDeviceAddress;
    private String _mDeviceName;

    /*
    Boolean variables used to estabilish the status of the connection
    */
    private boolean _connState = false;
    private boolean _flag = true;
    private boolean mScanning = false;

    /*
    The service connection containing the actions definition onServiceConnected and onServiceDisconnected
    */
    private final ServiceConnection _mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((RBLService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "onServiceConnected: Unable to initialize Bluetooth");

            } else {
                Log.d(TAG, "onServiceConnected: Bluetooth initialized correctly");



                // do not connect automatically
                mBluetoothLeService.connect("80:EA:CA:00:00:00");


            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Bluetooth disconnected");
            mBluetoothLeService = null;
        }
    };


    /*
    Callback called when the bluetooth device receive relevant updates about connection, disconnection, service discovery, data available, rssi update
    */
    private final BroadcastReceiver _mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_CONNECTED.equals(action)) {
                _connState = true;
                _flag = true;
                Log.d(TAG, "Connect to Gatt Susscess");


            } else if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
               _connState = false;
                _flag = false;

                Log.d(TAG, "Connection lost");
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                Log.d(TAG, "GATT SERVICE CONNECTED");
                getGattService(mBluetoothLeService.getSupportedGattService());

            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {

                displayData(intent.getStringExtra(RBLService.EXTRA_DATA));

            }
        }
    };




        private void displayData(String dataString) {
            if (dataString != null) {

                char dataTxArr[] = dataString.toCharArray();

                for (int i = 0; i < dataTxArr.length; i++) {

                    if (dataTxArr[i] != '}') {
                        dataTx += dataTxArr[i];


                    } else if (dataTxArr[i] == '}') {
                        dataTx += dataTxArr[i];
                        dataFullTx = dataTx;

                        SendData(value);

                        dataTx = "";
                    } else if (dataString == " ") {

                        Log.e(TAG,"get white space char");

                    }

                }
            }

        }

    /*
    METHODS DEFINITION
    */

        public static BleFramework getInstance(Activity activity) {
            if (_instance == null) {
                synchronized (BleFramework.class) {
                    if (_instance == null) {
                        Log.d(TAG, "BleFramework: Creation of _instance");
                        _instance = new BleFramework(activity);
                    }
                }
            }

            return _instance;
        }

        public BleFramework(Activity activity) {
            Log.d(TAG, "BleFramework: saving unityActivity in private var.");
            this._unityActivity = activity;
        }

        /*
        Method used to create a filter for the bluetooth actions that you like to receive
        */
        private static IntentFilter makeGattUpdateIntentFilter() {
            final IntentFilter intentFilter = new IntentFilter();

            intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
            intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
            intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
            intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

            return intentFilter;
        }

    /*
    Method used to initialize the characteristic for data transmission
    */

        private void getGattService(BluetoothGattService gattService) {

            if (gattService == null)
                return;

            BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
            _map.put(characteristic.getUuid(), characteristic);

            BluetoothGattCharacteristic characteristicTx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);
            mBluetoothLeService.setCharacteristicNotification(characteristicTx,
                    true);


            if(BluetoothGattCharacteristic.PROPERTY_NOTIFY >0) {

                mBluetoothLeService.readCharacteristic(characteristicTx);
            }
            BluetoothGattCharacteristic characteristiclctrl = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_LOW_CTRL);
            _map.put(characteristic.getUuid(), characteristic);



        }




        /*
        Method used to scan for available bluetooth low energy devices
        */
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        private void scanLeDevice() {

            if (!mScanning) {

                // Stops scanning after a pre-defined scan period.

                mHandler.postDelayed(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        _mBluetoothAdapter.stopLeScan(_mLeScanCallback);
                        mScanning = false;

                    }
                }, SCAN_PERIOD);


                Log.d(TAG, "Scan Device");
                _mBluetoothAdapter.startLeScan(_mLeScanCallback);
                mScanning = true;

            } else {

                Log.d(TAG, "Stop Scan");
                _mBluetoothAdapter.stopLeScan(_mLeScanCallback);
                mScanning = false;

            }
        }


        /*
          Callback called when the scan of bluetooth devices is finished
         */
        private BluetoothAdapter.LeScanCallback _mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

                _unityActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d(TAG, "onLeScan: run()");
                        if (device == null && device.getName() != null) {
                            Log.d(TAG, "Device is null? stop?");
                        } else {
                            listBluetoothDevice.add(device);
                            Log.d(TAG, device.getAddress() + " " + device.getName() + "");
                        }
                    }
                });
            }
        };


        private void unregisterBleUpdatesReceiver() {
            Log.d(TAG, "unregisterBleUpdatesReceiver:");
            _unityActivity.unregisterReceiver(_mGattUpdateReceiver);
        }

        private void registerBleUpdatesReceiver() {
            Log.d(TAG, "registerBleUpdatesReceiver:");
            if (!_mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "registerBleUpdatesReceiver: WARNING: _mBluetoothAdapter is not enabled!");

            }
            Log.d(TAG, "registerBleUpdatesReceiver: registerReceiver");
            _unityActivity.registerReceiver(_mGattUpdateReceiver, makeGattUpdateIntentFilter());
        }


        /*
        Public methods that can be directly called by Unity
        */
        @RequiresApi(api = Build.VERSION_CODES.M)
        public void _InitBLEFramework() {
            System.out.println("Android Executing: _InitBLEFramework");

            if (!_unityActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Log.d(TAG, "onCreate: fail: missing FEATURE_BLUETOOTH_LE");

                return;
            }

            final BluetoothManager mBluetoothManager = (BluetoothManager) _unityActivity.getSystemService(Context.BLUETOOTH_SERVICE);
            _mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (_mBluetoothAdapter == null) {
                Log.d(TAG, "onCreate: fail: _mBluetoothAdapter is null");
                return;
            }



            mHandler = new Handler();
            registerBleUpdatesReceiver();

            Log.d(TAG, "onCreate: _mBluetoothAdapter correctly initialized");





            // SCAN DEVICES
            scanLeDevice();


        }


        public boolean IsDeviceConnected() {


                _connState = true;
                Log.d(TAG, "_IsDeviceConnected: " + _connState);


            return _connState;
        }

        public boolean _SearchDeviceDidFinish() {
            Log.d(TAG, "_SearchDeviceDidFinish");
            return !mScanning;
        }

        public String _GetListOfDevices() {
            String jsonListString;

            if (listBluetoothDevice.size() > 0) {
                Log.d(TAG, "_GetListOfDevices");
                String[] uuidsArray = new String[listBluetoothDevice.size()];

                for (int i = 0; i < listBluetoothDevice.size(); i++) {

                    BluetoothDevice bd = listBluetoothDevice.get(i);


                    uuidsArray[i] = bd.getAddress();
                }
                Log.d(TAG, "_GetListOfDevices: Building JSONArray");
                JSONArray uuidsJSON = new JSONArray(Arrays.asList(uuidsArray));
                Log.d(TAG, "_GetListOfDevices: Building JSONObject");
                JSONObject dataUuidsJSON = new JSONObject();

                try {
                    Log.d(TAG, "_GetListOfDevices: Try inserting uuuidsJSON array in the JSONObject");
                    dataUuidsJSON.put("data", uuidsJSON);
                } catch (JSONException e) {
                    Log.e(TAG, "_GetListOfDevices: JSONException");
                    e.printStackTrace();
                }

                jsonListString = dataUuidsJSON.toString();

                Log.d(TAG, "_GetListOfDevices: sending found devices in JSON: " + jsonListString);

            } else {
                jsonListString = "NO DEVICE FOUND";
                Log.d(TAG, "_GetListOfDevices: no device was found");
            }

            return jsonListString;
        }



        public void Connect() {


            Log.d(TAG, "Connecting.....");
            for (BluetoothDevice device : listBluetoothDevice) {

                _mDeviceName = device.getName();
                Log.d(TAG, "Name: " + _mDeviceName);

                Intent gattServiceIntent = new Intent(_unityActivity, RBLService.class);
                _unityActivity.bindService(gattServiceIntent, _mServiceConnection, BIND_AUTO_CREATE);


            }
        }


    public String ReadData()
    {
        Log.d(TAG, "**********READ**********");
        Log.d(TAG,"\n"+dataFullTx);
		
		
        return dataFullTx;
    }




        public void SendData(String data) {



            Log.d(TAG, "**********WRITE**********");


            BluetoothGattCharacteristic characteristic = _map.get(RBLService.UUID_BLE_SHIELD_RX);


            characteristic.setValue(value);

            if (mBluetoothLeService == null) {
                Log.d(TAG, "mBluetoothLeService is null");
            }
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothLeService.writeCharacteristic(characteristic);
            Log.d(TAG, "\n"+value);





        }

    }









