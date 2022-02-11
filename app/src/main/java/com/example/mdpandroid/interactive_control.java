package com.example.mdpandroid;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.nio.charset.Charset;

public class interactive_control extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interactive_control_layout);
        Button forward_button = findViewById(R.id.button4);
        Button reverse_button = findViewById(R.id.button5);
        Button left_button = findViewById(R.id.button6);
        Button right_button = findViewById(R.id.button7);
        Button rotate_left_button = findViewById(R.id.button9);
        Button rotate_right_button = findViewById(R.id.button8);
        TextView showReceived = findViewById(R.id.showReceived);
        BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("receivedMessage");
                showReceived.setText(message);
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));
        forward_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "w";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
            }
        });

        reverse_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "s";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
            }
        });

        left_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "a";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                       Bluetoothservice.write(bytes);
                }
            }
        });

        right_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "d";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
            }
        });

        rotate_left_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "tl";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
            }
        });

        rotate_right_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "tr";
                if (Bluetoothservice.BluetoothConnectionStatus == true) {
                    byte[] bytes = message.getBytes(Charset.defaultCharset());
                    Bluetoothservice.write(bytes);
                }
            }
        });
    }}

