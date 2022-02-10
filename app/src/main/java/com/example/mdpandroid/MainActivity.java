package com.example.mdpandroid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button bluetoothbtn = (Button)findViewById(R.id.Bluetoothbtn);
        bluetoothbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent blueintent = new Intent(getApplicationContext(), Bluetoothconnection.class);
                startActivity(blueintent);
            }
        });
        Button maze = (Button) findViewById(R.id.startbtn);
        maze.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view){
                Intent i = new Intent( MainActivity.this, arena_map.class);
                startActivity(i);
            }
        });
    }
}