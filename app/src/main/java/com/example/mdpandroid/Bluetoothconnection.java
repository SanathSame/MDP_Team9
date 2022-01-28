package com.example.mdpandroid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

public class Bluetoothconnection extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 0;
    private static final int REQUEST_DISCOVER_BT = 1;

    TextView msenttext, mreceivedtext, mbluestatus, mpairdisplay;
    Button mturnonbtn, mturnoffbtn, mdiscoverbtn, mpairbtn;
    BluetoothAdapter mbluetoothadapter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetoothconnection);

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        msenttext = findViewById(R.id.senttext);
        mreceivedtext = findViewById(R.id.receivedtext);
        mturnonbtn = findViewById(R.id.turnonbtn);
        mturnoffbtn = findViewById(R.id.turnoffbtn);
        mdiscoverbtn = findViewById(R.id.discoverbtn);
        mpairbtn = findViewById(R.id.pairbtn);
        mbluestatus = findViewById(R.id.bluestatus);
        mpairdisplay = findViewById(R.id.pairdisplay);


        //Bluetooth
        mbluetoothadapter = BluetoothAdapter.getDefaultAdapter();

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
                if (mbluetoothadapter.isEnabled()){
                    mbluetoothadapter.disable();
                    showToast("Turning Bluetooth Off");
                }
                else {
                    showToast("Bluetooth is already Off");
                }

            }
        });

        //Discover btn
        mdiscoverbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mbluetoothadapter.isDiscovering()){
                    showToast("Making your device discoverable");
                    Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, REQUEST_DISCOVER_BT);
               }
            }
        });

        //Paired Device
        mpairbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mbluetoothadapter.isEnabled()){
                    mpairdisplay.setText("Paired Devices");
                    Set<BluetoothDevice> devices = mbluetoothadapter.getBondedDevices();
                    for (BluetoothDevice device: devices){
                        mpairdisplay.append("\nDevice: " + device.getName()+ "," + device);
                    }
                }
                else {
                    //Bluetooth is off, unable to get paired devices
                    showToast("Please turn on Bluetooth");
                }
            }
        });
        }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

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

    //Toast message function
        private void showToast(String msg){
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}