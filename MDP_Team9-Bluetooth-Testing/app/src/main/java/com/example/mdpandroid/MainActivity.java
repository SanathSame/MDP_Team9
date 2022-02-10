package com.example.mdpandroid;

import static com.example.mdpandroid.map.Robot.*;
import com.example.mdpandroid.start.StartedActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import java.util.ArrayList;

import biz.laenger.android.vpbs.BottomSheetUtils;
import com.example.mdpandroid.bluetooth.BluetoothFragment;
import com.example.mdpandroid.bluetooth.MessageFragment;
import com.example.mdpandroid.bluetooth.PagerAdapter;
import com.example.mdpandroid.bluetooth.PagerAdapter.TabItem;

import com.example.mdpandroid.leaderboard.MapConfigDialog;
import com.example.mdpandroid.leaderboard.TimerDialogFragment;
import com.example.mdpandroid.map.MapCanvas;
import com.example.mdpandroid.map.BoardMap;

public class MainActivity extends AppCompatActivity {

    MapCanvas mapCanvas;
    private BoardMap _map = new BoardMap();
    Button btnReset;
    Button btnTarget;
    Button btnImg;
    Button btnFastest;
    ImageButton btnForward;
    ImageButton btnReverse;
    ImageButton btnLeft;
    ImageButton btnRight;
    TextView topTitle;

    Toolbar topToolbar;
    Toolbar bottomSheetToolbar;
    TabLayout bottomSheetTabLayout; //bottom_sheet_tabs
    ViewPager bottomSheetViewPager; //bottom_sheet_viewpager

    ArrayList longpress = new ArrayList();


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activty_main);
        mapCanvas = findViewById(R.id.pathGrid);
        btnReset = (Button) this.findViewById(R.id.btn_reset);
        btnTarget = (Button) this.findViewById(R.id.btn_target);
        btnForward = (ImageButton) this.findViewById(R.id.btn_accelerate);
        btnReverse = (ImageButton) this.findViewById(R.id.btn_reverse);
        btnLeft = (ImageButton) this.findViewById(R.id.btn_left);
        btnRight = (ImageButton) this.findViewById(R.id.btn_right);
        btnImg = (Button) this.findViewById(R.id.btn_img);
        btnFastest = (Button) this.findViewById(R.id.btn_fastest);
        topToolbar = (Toolbar) this.findViewById(R.id.toolbar_top);
        bottomSheetToolbar = (Toolbar) this.findViewById(R.id.bottom_sheet_toolbar);
        bottomSheetTabLayout = (TabLayout) this.findViewById(R.id.topTabs);
        bottomSheetViewPager = (ViewPager) this.findViewById(R.id.viewpager);
        topTitle = (TextView) this.findViewById(R.id.top_title);

        setupBottomSheet();

        _map = mapCanvas.getFinder();

        btnTarget.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageFragment.sendMessage("MAP -> RPI:\t\t ", "RESET" + '\n');
                int n = 0;
                while (n < _map.getTargets().size()) {
                    String message;
                    message = "OBS|[" + (n+1) + "," + _map.getTargets().get(n).getX() + "," + (21-_map.getTargets().get(n).getY()) + "," + _map.getTargets().get(n).getF() + "]";
                    MessageFragment.sendMessage("MAP -> RPI:\t\t ", message + '\n');
                    n++;
                }}
        });

        Button backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener(v->{
            Intent intent = new Intent(this, StartedActivity.class);
            startActivity(intent);
            finish();
        });

        btnFastest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageFragment.sendMessage("LDRB -> RPI:\t\t", "START_FASTEST");
                showBottomSheetDialog("fastest");
            }
        });

        btnReset.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mapCanvas.setSolving(false);
                _map.resetGrid();
                mapCanvas.invalidate();
            }
        });

        mapCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                updateRoboStatus();
                return false;
            }
        });

        setupImgLongClick();
        setupControlsLongClicks(btnForward, ROBOT_MOTOR_FORWARD);
        setupControlsLongClicks(btnReverse, ROBOT_MOTOR_REVERSE);
        setupControlsLongClicks(btnLeft, ROBOT_SERVO_LEFT);
        setupControlsLongClicks(btnRight, ROBOT_SERVO_RIGHT);
    }

    @SuppressLint("SetTextI18n")
    public void updateRoboStatus() {
        topTitle.setText("X: " + _map.getRobo().getX() + " Y: " + (20-_map.getRobo().getY()) + "\t\t" + _map.getRobo().getFacingText());
    }

    private void setupControlsLongClicks(View btn, int direction) {
        btn.setOnClickListener(v -> {
            if (longpress.size() < 1) {
                switch (v.getId()) {
                    case R.id.btn_accelerate:
                    case R.id.btn_reverse:
                        _map.getRobo().motorRotate(direction);
                        break;
                     //case R.id.btn_left:
                     //case R.id.btn_right:
                         //_map.getRobo().servoTurn(direction);
                         //Log.d("ROBOT", "CLICK " + _map.getRobo().toString());
                         //break;
                }
            }
            updateRoboStatus();
            longpress.clear();
        });
        btn.setOnTouchListener(new View.OnTouchListener() {

            private int DELAYms = 250;
            private Handler mHandler;

            @Override public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mHandler != null) return true;
                        mHandler = new Handler();
                        switch (v.getId()) {
                            case R.id.btn_accelerate:
                            case R.id.btn_reverse:
                                MessageFragment.sendMessage("BHLD -> RPI:\t\t", (direction == ROBOT_MOTOR_FORWARD ? STM_COMMAND_FORWARD : STM_COMMAND_REVERSE));
                                Log.d("ROBOT TOUCH DOWN", _map.getRobo().toString());
                                break;
                            case R.id.btn_left:
                            case R.id.btn_right:
                                MessageFragment.sendMessage("BHLD -> RPI:\t\t", (direction == ROBOT_SERVO_LEFT ? STM_COMMAND_LEFT : STM_COMMAND_RIGHT));
                                Log.d("ROBOT TOUCH DOWN", _map.getRobo().toString());
                                break;
                        }
                        mHandler.postDelayed(mAction, DELAYms);
                        break;
                    case MotionEvent.ACTION_UP:
                        if (mHandler == null) return true;
                        switch (v.getId()) {
                            case R.id.btn_accelerate:
                            case R.id.btn_reverse:
                                MessageFragment.sendMessage("BRLS -> RPI:\t\t", STM_COMMAND_STOP);
                                _map.getRobo().setMotor(ROBOT_MOTOR_STOP);
                                Log.d("ROBOT TOUCH UP", _map.getRobo().toString());
                                break;
                            case R.id.btn_left:
                            case R.id.btn_right:
                                MessageFragment.sendMessage("BRLS -> RPI:\t\t", STM_COMMAND_CENTRE);
                                _map.getRobo().setServo(ROBOT_SERVO_CENTRE);
                                Log.d("ROBOT TOUCH UP", _map.getRobo().toString());
                                break;
                        }
                        MessageFragment.addSeparator();
                        mHandler.removeCallbacks(mAction);
                        mHandler = null;
                        break;


                }
                updateRoboStatus();
                return false;
            }

            Runnable mAction = new Runnable() {
                @Override public void run() {
                    switch (btn.getId()) {
                        case R.id.btn_accelerate:
                        case R.id.btn_reverse:
                            _map.getRobo().motorRotate(direction);
                            Log.d("ROBOT RUNNABLE", _map.getRobo().toString());
                            break;
                        case R.id.btn_left:
                        case R.id.btn_right:
                            _map.getRobo().servoTurn(direction);
                            Log.d("ROBOT RUNNABLE", "TOUCH " + _map.getRobo().toString());
                            break;
                    }
                    longpress.add(1);
                    mHandler.postDelayed(this, DELAYms);
                }
            };

        });
    }

    private void setupImgLongClick() {

        btnImg.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBottomSheetDialog("config");
                return true;
            }
        });

        btnImg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageFragment.sendMessage("LDRB -> RPI:\t\t", "DRAW_PATH");
                showBottomSheetDialog("img");
            }
        });

    }

    private void setupBottomSheet() {
//        bottomSheetToolbar.setTitle(R.string.bottom_sheet_title);
        final PagerAdapter sectionsPagerAdapter = new PagerAdapter(getSupportFragmentManager(), this, TabItem.CONNECTION, TabItem.MESSAGE);
        bottomSheetViewPager.setOffscreenPageLimit(1);
        bottomSheetViewPager.setAdapter(sectionsPagerAdapter);
        bottomSheetTabLayout.setupWithViewPager(bottomSheetViewPager);
        BottomSheetUtils.setupViewPager(bottomSheetViewPager);
    }

    public void bluetoothOnClickMethods(View v) {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment instanceof BluetoothFragment) {
                BluetoothFragment bluetooth_fragment = (BluetoothFragment) fragment;
                bluetooth_fragment.myClickMethod(v, this);
            }
        }
    }

    private void showBottomSheetDialog(String dialog) {
        topToolbar.setVisibility(View.GONE);
        switch (dialog) {
            case "img":
                TimerDialogFragment imgDialog = MessageFragment.getTimerDialog("Image Recognition Run");
                imgDialog.show(getSupportFragmentManager(), imgDialog.getTag());
                break;
            case "fastest":
                TimerDialogFragment fastestDialog = MessageFragment.getTimerDialog("Fastest Path Run");
                fastestDialog.show(getSupportFragmentManager(), fastestDialog.getTag());
                break;
            case "config":
                final MapConfigDialog mapConfigDialog = new MapConfigDialog();
                mapConfigDialog.show(getSupportFragmentManager(), mapConfigDialog.getTag());
                break;
        }
    }
}
