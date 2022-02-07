package com.example.mdpandroid;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.json.JSONException;

import static android.content.Context.SENSOR_SERVICE;

public class MapTabFragment extends Fragment implements SensorEventListener{

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "MapFragment";

    private PageViewModel pageViewModel;

    Button resetMapBtn, updateButton,waypointBtn,startPointBtn;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackImageBtn, turnLeftImageBtn;
    Switch manualAutoToggleBtn;
    private static boolean autoUpdate = false;
    GridMap gridMap;
    Switch phoneTiltSwitch;
    public static boolean manualUpdateRequest = false;

    private Sensor mSensor;
    private SensorManager mSensorManager;

    public static MapTabFragment newInstance(int index) {
        MapTabFragment fragment = new MapTabFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.activity_map, container, false);

        gridMap = MainActivity.getGridMap();
        final DirectionFragment directionFragment = new DirectionFragment();
        final WaypointFragment waypointFragment = new WaypointFragment();

        manualAutoToggleBtn = root.findViewById(R.id.manualAutoToggleBtn);
        waypointBtn = root.findViewById(R.id.waypointBtn);
        startPointBtn=root.findViewById(R.id.startPointBtn);
        resetMapBtn=root.findViewById(R.id.resetBtn);
        moveForwardImageBtn = root.findViewById(R.id.forwardImageBtn);
        turnRightImageBtn = root.findViewById(R.id.rightImageBtn);
        moveBackImageBtn = root.findViewById(R.id.backImageBtn);
        turnLeftImageBtn = root.findViewById(R.id.leftImageBtn);
        phoneTiltSwitch = root.findViewById(R.id.phoneTiltSwitch);


        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
                gridMap.resetMap();
                ControlFragment.counter=0;
            }
        });

        mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (gridMap.getAutoUpdate()) {
                    updateStatus("Please press 'MANUAL'");
                    phoneTiltSwitch.setChecked(false);
                }
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    if(phoneTiltSwitch.isChecked()){
                        showToast("MOTION SENSOR ON");
                        moveForwardImageBtn.setVisibility(View.GONE);
                        moveBackImageBtn.setVisibility(View.GONE);
                        turnRightImageBtn.setVisibility(View.GONE);
                        turnLeftImageBtn.setVisibility(View.GONE);
                        phoneTiltSwitch.setPressed(true);

                        mSensorManager.registerListener(MapTabFragment.this, mSensor, mSensorManager.SENSOR_DELAY_NORMAL);
                        sensorHandler.post(sensorDelay);
                    }else{
                        showToast("MOTION SENSOR OFF");
                        moveForwardImageBtn.setVisibility(View.VISIBLE);
                        moveBackImageBtn.setVisibility(View.VISIBLE);
                        turnRightImageBtn.setVisibility(View.VISIBLE);
                        turnLeftImageBtn.setVisibility(View.VISIBLE);
                        showLog("unregistering Sensor Listener");
                        try {
                            mSensorManager.unregisterListener( MapTabFragment.this);
                        }catch(IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        sensorHandler.removeCallbacks(sensorDelay);
                    }
                } else {
                    updateStatus("Please set the 'STARTING POINT'");
                    phoneTiltSwitch.setChecked(false);
                }
                if(phoneTiltSwitch.isChecked()){
                    compoundButton.setText("MOTION SENSOR ON");
                }else
                {
                    compoundButton.setText("MOTION SENSOR OFF");
                }
            }
        }); moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    MainActivity.refreshLabel();
                    if (gridMap.getValidPosition()){
                        MainActivity.printMessage("W1|");
                        updateStatus("moving forward");
                        /*if(counter>1){
                            TimerTask MyTimer = new MyTimerTask();
                            timer.scheduleAtFixedRate(MyTimer, 4000, 6000);
                        }*/
                    }

                    else
                        updateStatus("Unable to move forward");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveForwardImageBtn");
            }
        });

        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnRightImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    MainActivity.refreshLabel();
                    MainActivity.printMessage("D|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnRightImageBtn");
            }
        });

        moveBackImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    MainActivity.refreshLabel();
                    if (gridMap.getValidPosition()){
                        MainActivity.printMessage("S1|");
                        updateStatus("moving backward");
                        /*if(counter>1){
                            TimerTask MyTimer = new MyTimerTask();
                            timer.scheduleAtFixedRate(MyTimer, 4000, 6000);
                        }*/
                    }else
                        updateStatus("Unable to move backward");

                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    MainActivity.refreshLabel();
                    updateStatus("turning left");
                    MainActivity.printMessage("A|");
                }
                else
                    updateStatus("Please press 'STARTING POINT'");
                showLog("Exiting turnLeftImageBtn");
            }
        });
        startPointBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    gridMap.setStartingPointManual();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //gridMap.toggleCheckedBtn("setStartPointToggleBtn");
            }
        });
        waypointBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked waypointBtn");
                waypointFragment.show(getActivity().getFragmentManager(), "Waypoint Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });
        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutoUpdate(true);
                        autoUpdate = true;
                        //gridMap.toggleCheckedBtn("None");
                        phoneTiltSwitch.setVisibility(View.GONE);
                        moveForwardImageBtn.setVisibility(View.GONE);
                        moveBackImageBtn.setVisibility(View.GONE);
                        turnRightImageBtn.setVisibility(View.GONE);
                        turnLeftImageBtn.setVisibility(View.GONE);
                        manualAutoToggleBtn.setText("AUTO");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("AUTO mode");

                }
                else if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        autoUpdate = false;
                        gridMap.setAutoUpdate(false);
                        phoneTiltSwitch.setVisibility(View.VISIBLE);
                        moveForwardImageBtn.setVisibility(View.VISIBLE);
                        moveBackImageBtn.setVisibility(View.VISIBLE);
                        turnRightImageBtn.setVisibility(View.VISIBLE);
                        turnLeftImageBtn.setVisibility(View.VISIBLE);
                        manualAutoToggleBtn.setText("MANUAL");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
                showLog("Exiting manualAutoToggleBtn");
            }
        });

        return root;
    }
    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this,1000);
        }
    };

    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        showLog("SensorChanged X: "+x);
        showLog("SensorChanged Y: "+y);
        showLog("SensorChanged Z: "+z);

        if(sensorFlag) {
            if (y < -2) {
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                MainActivity.refreshLabel();
                MainActivity.printMessage("W1|");
            } else if (y > 2) {
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                MainActivity.refreshLabel();
                MainActivity.printMessage("S1|");
            } else if (x > 2) {
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                MainActivity.refreshLabel();
                MainActivity.printMessage("A|");
            } else if (x < -2) {
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                MainActivity.refreshLabel();
                MainActivity.printMessage("D|");
            }
        }
        sensorFlag = false;
    }
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try{
            mSensorManager.unregisterListener(MapTabFragment.this);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0, 0);
        toast.show();
    }


}
