package com.example.mdpandroid.start;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;

import com.example.mdpandroid.MainActivity;
import com.example.mdpandroid.R;

public class StartedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_started);
        CardView startedBtn = findViewById(R.id.get_started);
        CardView blueToothBtn = findViewById(R.id.blueToothBtn);
        startedBtn.setOnClickListener(v->{
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        blueToothBtn.setOnClickListener(v->{
            Intent intent = new Intent(this, Bluetoothconnection.class);
            startActivity(intent);
        });
    }
}