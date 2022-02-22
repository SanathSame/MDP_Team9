package com.example.mdpandroid.start;
import com.example.mdpandroid.R;
import com.example.mdpandroid.MainActivity;
import com.example.mdpandroid.map.BoardMap;
import com.example.mdpandroid.map.MapCanvas;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class Bluetoothconnection extends AppCompatActivity{

    private static final String TAG = "Bluetoothconnection";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;
    private static final int REQUEST_ENABLE_BT = 0;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static BluetoothDevice mBTDevice;
    BluetoothAdapter mbluetoothadapter;
    Bluetoothservice mBluetoothconnection;
    MapCanvas mapCanvas;
    Context mContext;
    public ArrayList<BluetoothDevice> mNewBTDevices;
    public ArrayList<BluetoothDevice> mPairedBTDevices;
    public DeviceListAdapter mNewDevlceListAdapter;
    public DeviceListAdapter mPairedDevlceListAdapter;

    static TextView mreceivedtext;

    TextView msenttext, mbluestatus;
    Button mturnonbtn, mturnoffbtn, mdiscoverbtn, mconnectbtn, mbackbtn, msentbtn;
    ListView lvnewdevice, lvpairedevice;

    boolean retryConnection = false;
    Handler reconnectionHandler = new Handler();
    BoardMap _map;
    //MapCanvas mapCanvas;

    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            // bluetooth connection established
            try {
                if (Bluetoothservice.BluetoothConnectionStatus == false) {
                    startBTConnection(mBTDevice, uuid);
                    Toast.makeText(Bluetoothconnection.this, "Reconnection Success", Toast.LENGTH_SHORT).show();

                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Toast.makeText(Bluetoothconnection.this, "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void startConnection(){
        startBTConnection(mBTDevice,uuid);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection");

        mBluetoothconnection.startClientThread(device, uuid);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            _map = (BoardMap) getIntent().getSerializableExtra("boardmap"); //Obtaining data
        }
        setContentView(R.layout.activity_bluetoothconnection);

        msenttext = findViewById(R.id.senttext);
        mreceivedtext = findViewById(R.id.receivedtext);
        mturnonbtn = findViewById(R.id.turnonbtn);
        mturnoffbtn = findViewById(R.id.turnoffbtn);
        mdiscoverbtn = findViewById(R.id.discoverbtn);
        mconnectbtn = findViewById(R.id.connectbtn);
        msentbtn = findViewById(R.id.sentbtn);
        mbackbtn = findViewById(R.id.backbtn);
        mbluestatus = findViewById(R.id.bluestatus);
        lvnewdevice = (ListView) findViewById(R.id.lvnewdevice);
        lvnewdevice.setBackgroundColor(Color.parseColor("#E91E63"));
        lvpairedevice = (ListView) findViewById(R.id.lvpairdevice);
        lvpairedevice.setBackgroundColor(Color.parseColor("#8BC34A"));

        mNewBTDevices = new ArrayList<>();
        mPairedBTDevices = new ArrayList<>();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        //Initialize SharedPreferences
        Bluetoothconnection.context = getApplicationContext();
        SharedPreferences pref = getApplicationContext().getSharedPreferences("Value",0);
        SharedPreferences.Editor editor = pref.edit();
        this.sharedPreferences();

        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver5, filter2);

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        //Bluetooth
        mbluetoothadapter = BluetoothAdapter.getDefaultAdapter();

        lvnewdevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                checkBTPermission();
                mbluetoothadapter.cancelDiscovery();
                lvpairedevice.setAdapter(mPairedDevlceListAdapter);

                String deviceName = mNewBTDevices.get(i).getName();
                String deviceAddress = mNewBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    mNewBTDevices.get(i).createBond();

                    mBluetoothconnection = new Bluetoothservice(Bluetoothconnection.this);
                    mBTDevice = mNewBTDevices.get(i);
                }
            }
        });

        lvpairedevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                checkBTPermission();
                mbluetoothadapter.cancelDiscovery();
                lvnewdevice.setAdapter(mNewDevlceListAdapter);

                String deviceName = mPairedBTDevices.get(i).getName();
                String deviceAddress = mPairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                mBluetoothconnection = new Bluetoothservice(Bluetoothconnection.this);
                mBTDevice = mPairedBTDevices.get(i);
            }
        });


        //check whether both device has bluetooth
        if (mbluetoothadapter == null) {
            mbluestatus.setText("Bluetooth is not available");
        }
        else {
            mbluestatus.setText("Bluetooth is available");
        }

        //On btn
        mturnonbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBTPermission();
                if (!mbluetoothadapter.isEnabled()){
                    showToast("Turning On Bluetooth");
                    //intent to turn on bluetooth
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, REQUEST_ENABLE_BT);
                }
                else {
                    showToast("Bluetooth is already on");
                }
            }
        });

        //Off btn
        mturnoffbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBTPermission();
                if (mbluetoothadapter.isEnabled()){
                    mbluetoothadapter.disable();
                    showToast("Turning Bluetooth Off");
                }
                else {
                    showToast("Bluetooth is already Off");
                }

            }
        });

//      sent btn
        msentbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked sendTextBtn");
                String sentText = "" + msenttext.getText().toString();
                sharedPreferences();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentText);
                editor.commit();
//                mreceivedtext.setText(sharedPreferences.getString("message", ""));
                msenttext.setText("");

                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
                showLog("Exiting sendTextBtn");
            }
        });

        //connect btn
        mconnectbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mBTDevice ==null)
                {
                    Toast.makeText(Bluetoothconnection.this, "Please Select a Device before connecting.", Toast.LENGTH_LONG).show();
                }
                else {
                    startConnection();
                }
            }
        });

        //Discover btn
        mdiscoverbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkBTPermission();

                Log.d(TAG, "toggleButton: Scanning for unpaired devices.");
                mNewBTDevices.clear();
                if(mbluetoothadapter != null) {
                    if (!mbluetoothadapter.isEnabled()) {
                        Toast.makeText(Bluetoothconnection.this, "Please turn on Bluetooth first!", Toast.LENGTH_SHORT).show();
                    }
                    if (mbluetoothadapter.isDiscovering()) {
                        mbluetoothadapter.cancelDiscovery();
                        Log.d(TAG, "toggleButton: Cancelling Discovery.");

                        mbluetoothadapter.startDiscovery();
                        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                    } else if (!mbluetoothadapter.isDiscovering()) {

                        mbluetoothadapter.startDiscovery();
                        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                        registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
                    }
                    mPairedBTDevices.clear();
                    Set<BluetoothDevice> pairedDevices = mbluetoothadapter.getBondedDevices();
                    Log.d(TAG, "toggleButton: Number of paired devices found: "+ pairedDevices.size());
                    for(BluetoothDevice d : pairedDevices){
                        Log.d(TAG, "Paired Devices: "+ d.getName() +" : " + d.getAddress());
                        mPairedBTDevices.add(d);
                        mPairedDevlceListAdapter = new DeviceListAdapter(Bluetoothconnection.this, R.layout.device_adapter_view, mPairedBTDevices);
                        lvpairedevice.setAdapter(mPairedDevlceListAdapter);
                    }
                }

//                if (!mbluetoothadapter.isDiscovering()){
//                    showToast("Making your device discoverable");
//                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
//                }
            }

        });

        mbackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle bundle = new Bundle();
                bundle.putSerializable("boardmap", _map);
                Intent i = new Intent(getApplicationContext(), MainActivity.class);
                i.putExtras(bundle);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            }
        });

    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    public static void sharedPreferences() {
        sharedPreferences = Bluetoothconnection.getSharedPreferences(Bluetoothconnection.context);
        editor = sharedPreferences.edit();
    }

    public static TextView getMessageReceivedtext() {
        return mreceivedtext;
    }

    public static void refreshMessageReceived() {
        mreceivedtext.setText(sharedPreferences.getString("message", ""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    //bluetooth is on
                    showToast("Bluetooth is on");
                }
                else {
                    //user denied to turn turntooth on
                    showToast("Bluetooth cannot be turn on");
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkBTPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            if (permissionCheck != 0){
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        }
    }


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mbluetoothadapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mbluetoothadapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");
            checkBTPermission();

            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mNewBTDevices.add(device);
                Log.d(TAG, "onReceive: "+ device.getName() +" : " + device.getAddress());
                mNewDevlceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mNewBTDevices);
                lvnewdevice.setAdapter(mNewDevlceListAdapter);

            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            checkBTPermission();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BOND_BONDED.");
                    Toast.makeText(Bluetoothconnection.this, "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    mBTDevice = mDevice;
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BOND_BONDING.");
                }
                if(mDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    private BroadcastReceiver mBroadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkBTPermission();
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if(status.equals("connected")){
                try {
//                    myDialog.dismiss();
                } catch(NullPointerException e){
                    e.printStackTrace();
                }

                Log.d(TAG, "mBroadcastReceiver5: Device now connected to "+mDevice.getName());
                Toast.makeText(Bluetoothconnection.this, "Device now connected to "+mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                mbluestatus.setText("Connected to " + mDevice.getName());
            }
            else if(status.equals("disconnected") && retryConnection == false){
                Log.d(TAG, "mBroadcastReceiver5: Disconnected from "+mDevice.getName());
                Toast.makeText(Bluetoothconnection.this, "Disconnected from "+mDevice.getName(), Toast.LENGTH_LONG).show();
                mBluetoothconnection = new Bluetoothservice(Bluetoothconnection.this);
                mBluetoothconnection.startAcceptThread();


                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.bluestatus);
                connStatusTextView.setText("Disconnected");
                editor.commit();

                try {
//                    myDialog.show();
                }catch (Exception e){
                    Log.d(TAG, "BluetoothPopUp: mBroadcastReceiver5 Dialog show failure");
                }
                retryConnection = true;
                reconnectionHandler.postDelayed(reconnectionRunnable, 5000);

            }
            editor.commit();
        }
    };

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
//            Intent incomingintent = new Intent("incomingmessage");
//            incomingintent.putExtra("receivedmessage", incomingintent);
//            LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingintent);
            showLog("receivedMessage: message --- " + message);
            SharedPreferences pref = getSharedPreferences("Value",0);
            String receivedText = message;
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("message", receivedText);
            editor.commit();
            editor.apply();
            System.out.println("bluetooth shit");
            refreshMessageReceived();
        }
    };
//    public static void useReceivedMessage(BoardMap _map, MapCanvas mapCanvas, String msg) {
//        int j;
//        String[] cases = {"ROBOT", "TARGET"};
//        String[] parts;
//        for(j = 0; j < cases.length; j++)
//            if(msg.contains(cases[j]))
//                break;
//        switch(j) {
//            case 0:
//                parts = msg.replace(" ","").replace("ROBOT", "").split(",");
//                _map.getRobo().setX(Integer.parseInt(parts[0]));
//                _map.getRobo().setY(20-Integer.parseInt(parts[1]));
//                _map.getRobo().setFacing(Integer.parseInt(parts[2])); //0123 NSEW
//                break;
//            case 1:
//                parts = msg.replace(" ","").replace("TARGET", "").split(",");
//                int targetid = Integer.parseInt(parts[0]);
//                int imageid = Integer.parseInt(parts[1]);
//                Target t = _map.getTargets().get(targetid-1);
//                t.setImg(imageid);
//                break;
//            default:
//                System.out.println("invalid");
//        }
//    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        super.onPause();
        try {
            unregisterReceiver(mBroadcastReceiver1);
            unregisterReceiver(mBroadcastReceiver2);
            unregisterReceiver(mBroadcastReceiver3);
            unregisterReceiver(mBroadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver5);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("mBTDevice", mBTDevice);
        data.putExtra("myUUID",uuid);
        setResult(RESULT_OK, data);
        super.finish();
    }

    //Toast message function
        private void showToast(String msg){
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }
}