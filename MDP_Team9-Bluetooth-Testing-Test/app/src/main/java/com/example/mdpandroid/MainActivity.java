package com.example.mdpandroid;

import static com.example.mdpandroid.map.BoardMap.TARGET_CELL_CODE;
import static com.example.mdpandroid.map.Robot.*;


import com.example.mdpandroid.leaderboard.TimerDialogFragment;
import com.example.mdpandroid.map.Target;
import com.example.mdpandroid.start.Bluetoothconnection;
import android.content.IntentFilter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;

import android.graphics.Canvas;
import android.os.Bundle;

import android.os.Handler;

import com.example.mdpandroid.start.Bluetoothservice;
import com.example.mdpandroid.start.MessageBox;
import com.example.mdpandroid.start.StartedActivity;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;


import java.nio.charset.Charset;
import java.util.ArrayList;

import biz.laenger.android.vpbs.BottomSheetUtils;

import com.example.mdpandroid.bluetooth.PagerAdapter;
import com.example.mdpandroid.bluetooth.PagerAdapter.TabItem;

import com.example.mdpandroid.leaderboard.MapConfigDialog;
import com.example.mdpandroid.map.MapCanvas;
import com.example.mdpandroid.map.BoardMap;

public class MainActivity extends AppCompatActivity {

    // Declaration Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    MapCanvas mapCanvas;
    private BoardMap _map = new BoardMap();
    private BoardMap mapPass = new BoardMap();
    Button btnReset;
    Button btnTarget;
    Button btnsenttext;
    Button btnImg;
    Button btnFastest;
    ImageButton btnForward;
    ImageButton btnReverse;
    ImageButton btnLeft;
    ImageButton btnRight;
    TextView topTitle, statusBox;

    Toolbar topToolbar;
    Toolbar bottomSheetToolbar;
    TabLayout bottomSheetTabLayout; //bottom_sheet_tabs
    ViewPager bottomSheetViewPager; //bottom_sheet_viewpager

    ArrayList longpress = new ArrayList();
    String message;
    int msgCount;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
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
      LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        // Set up sharedPreferences
        MainActivity.context = getApplicationContext();
        this.sharedPreferences();


        setupBottomSheet();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mapPass = (BoardMap) getIntent().getSerializableExtra("boardmap"); //Obtaining data
            try {
                ArrayList<Target> targets = mapPass.getTargets();
                _map = mapCanvas.getFinder();
                _map.getRobo().setX(mapPass.getRobo().getX());
                _map.getRobo().setY(mapPass.getRobo().getY());
                _map.getRobo().setFacing(mapPass.getRobo().getFacing());

                for(int i=0; i < mapPass.getTargets().size(); i++) {
                    _map.getTargets().add(new Target(mapPass.getTargets().get(i).getX(), mapPass.getTargets().get(i).getY(), i, mapPass.getTargets().get(i).getF()));
                    if(mapPass.getTargets().get(i).getImg() > -1)
                        _map.getTargets().get(i).setImg(mapPass.getTargets().get(i).getImg());
                    _map.getBoard()[mapPass.getTargets().get(i).getX()][mapPass.getTargets().get(i).getY()] = TARGET_CELL_CODE;
                }
                updateRoboStatus();
            }
            catch (NullPointerException e) {
                System.err.println("Null pointer exception");
            }
        }
        else{
            _map = mapCanvas.getFinder();
        }


        btnTarget.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                //MessageBox.sendMessage("MAP -> RPI:\t\t ", "RESET" + '\n');
                int n = 0;
                message = "";
                while (n < _map.getTargets().size()) {
                    message = message + "OBS " + (n+1) + " " + _map.getTargets().get(n).getX() + " " + (21-_map.getTargets().get(n).getY()) + " " + _map.getTargets().get(n).getF();
                    n++;
                    if(n == _map.getTargets().size())
                        break;
                    if(n > 0)
                        message += " || ";
                    //sendMessage(message);
                    //System.out.println(message);

                }
                MessageBox.sendMessage("ALG ", message + '\n');
            }

        });

        Button backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener(v->{
            Bundle bundle = new Bundle();
            bundle.putSerializable("boardmap", _map);
            Intent i = new Intent(getApplicationContext(), Bluetoothconnection.class);
            i.putExtras(bundle);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        });

        btnFastest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MessageBox.sendMessage("STM ", "START_FASTEST");
                //MessageFragment.sendMessage("LDRB -> RPI:\t\t", "START_FASTEST");
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
                                String space = "     ";
                                if(msgCount>=10)
                                    space = "    ";

                                message = (msgCount + space + (direction == ROBOT_MOTOR_FORWARD ? STM_COMMAND_FORWARD : STM_COMMAND_REVERSE));
                                //MessageFragment.sendMessage("BTH -> RPI:\t\t", (direction == ROBOT_MOTOR_FORWARD ? STM_COMMAND_FORWARD : STM_COMMAND_REVERSE));
                                MessageBox.sendMessage("STM ", message);
                                msgCount++;
                                Log.d("ROBOT TOUCH DOWN", _map.getRobo().toString());
                                break;
                            case R.id.btn_left:
                            case R.id.btn_right:
                                message = (direction == ROBOT_MOTOR_FORWARD ? "SR" : "SL");
                                MessageBox.sendMessage("STM ", message);
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
                                //MessageBox.sendMessage("BRLS -> RPI:\t\t", STM_COMMAND_STOP);
                                _map.getRobo().setMotor(ROBOT_MOTOR_STOP);
                                //Log.d("ROBOT TOUCH UP", _map.getRobo().toString());
                                break;
                            case R.id.btn_left:
                            case R.id.btn_right:
                                //MessageBox.sendMessage("BRLS -> RPI:\t\t", STM_COMMAND_CENTRE);
                                _map.getRobo().setServo(ROBOT_SERVO_CENTRE);
                                Log.d("ROBOT TOUCH UP", _map.getRobo().toString());
                                break;
                        }
                        MessageBox.addSeparator();
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
//    private void sendMessage(String msg){
//        if (Bluetoothservice.BluetoothConnectionStatus == true) {
//            byte[] bytes = msg.getBytes(Charset.defaultCharset());
//            Bluetoothservice.write(bytes);
//        }
//    }
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
                showBottomSheetDialog("img");
            }
        });

    }

    private void setupBottomSheet() {
//        bottomSheetToolbar.setTitle(R.string.bottom_sheet_title);
        final PagerAdapter sectionsPagerAdapter = new PagerAdapter(getSupportFragmentManager(), this, TabItem.MESSAGE);
        bottomSheetViewPager.setOffscreenPageLimit(1);
        bottomSheetViewPager.setAdapter(sectionsPagerAdapter);
        bottomSheetTabLayout.setupWithViewPager(bottomSheetViewPager);
        BottomSheetUtils.setupViewPager(bottomSheetViewPager);
    }

//    public void bluetoothOnClickMethods(View v) {
//        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
//            if (fragment instanceof BluetoothFragment) {
//                BluetoothFragment bluetooth_fragment = (BluetoothFragment) fragment;
//                bluetooth_fragment.myClickMethod(v, this);
//            }
//        }
//    }

//    public void refreshMessageReceived(String statusmsg) {
//        statusBox.setText(statusmsg);
//    }

    public void refreshMessageReceivedfromblue() {
        Bluetoothconnection.getMessageReceivedtext().setText(sharedPreferences.getString("message", ""));
    }

    public void sharedPreferences() {
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }


    private void showBottomSheetDialog(String dialog) {
        topToolbar.setVisibility(View.GONE);
        System.out.println(dialog);
        switch (dialog) {
            case "img":
                System.out.println("asdf");
                TimerDialogFragment imgDialog = MessageBox.getTimerDialog("Image Recognition Run");
                imgDialog.show(getSupportFragmentManager(), imgDialog.getTag());
                break;
            case "fastest":
                TimerDialogFragment fastestDialog = MessageBox.getTimerDialog("Fastest Path Run");
                fastestDialog.show(getSupportFragmentManager(), fastestDialog.getTag());
                break;
            case "config":
                final MapConfigDialog mapConfigDialog = new MapConfigDialog();
                mapConfigDialog.show(getSupportFragmentManager(), mapConfigDialog.getTag());
                break;
        }
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshMessageReceivedfromblue();
            String receivedText = sharedPreferences.getString("message", "");
            System.out.println(receivedText + "test");
            useReceivedMessage(_map, mapCanvas, receivedText);
            MessageBox.receiveMessage(receivedText);
            updateRoboStatus();
        }
    };
    public static void useReceivedMessage(BoardMap _map, MapCanvas mapCanvas, String msg) {
        int j;
        String[] cases = {"ROBOT", "TARGET","status:"};
        System.out.println("rubbish");
        String[] parts;
        for(j = 0; j < cases.length; j++)
            if(msg.contains(cases[j]))
                break;
        switch(j) {
            case 0:
                try {
                parts = msg.replace(" ","").replace("ROBOT", "").split(",");
                _map.getRobo().setX(Integer.parseInt(parts[0]));
                _map.getRobo().setY(20-Integer.parseInt(parts[1]));
                _map.getRobo().setFacing(Integer.parseInt(parts[2])); //0123 NSEW
                System.out.println("robot change");
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("exc");

                }
                System.out.println("switch");
                break;
            case 1:
                try {
                parts = msg.replace(" ","").replace("TARGET", "").split(",");
                int targetid = Integer.parseInt(parts[0]);
                int imageid = Integer.parseInt(parts[1]);
                Target t = _map.getTargets().get(targetid-1);
                t.setImg(imageid);
                System.out.println("target change");

                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    msg = msg.replace(" ","").replace("status:", "");
                    MessageBox.receiveStatusMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                System.out.println("invalid");
        }

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        try{
//            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }
}
