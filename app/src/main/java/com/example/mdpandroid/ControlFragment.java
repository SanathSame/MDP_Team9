package com.example.mdpandroid;

import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import org.json.JSONException;

import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.SENSOR_SERVICE;

public class ControlFragment extends Fragment  {
    // Init
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "ControlFragment";
    private PageViewModel pageViewModel;

    // Declaration Variable
    // Shared Preferences
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    // Control Button
    ImageButton exploreResetButton, fastestResetButton, imageResetButton;
    private static long exploreTimer, fastestTimer, imageTimer;
    ToggleButton exploreButton, fastestButton, imageButton;
    TextView exploreTimeTextView, fastestTimeTextView, robotStatusTextView, imageTimeTextView;
    private static GridMap gridMap;

    public static int counter=0;

    // Timer
    static Handler timerHandler = new Handler();

    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;

            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableImage = new Runnable() {
        @Override
        public void run() {
            long millisImage = System.currentTimeMillis() - imageTimer;
            int secondsImage = (int) (millisImage / 1000);
            int minutesImage = secondsImage / 60;
            secondsImage = secondsImage % 60;

            imageTimeTextView.setText(String.format("%02d:%02d", minutesImage, secondsImage));

            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {
            long millisFastest = System.currentTimeMillis() - fastestTimer;
            int secondsFastest = (int) (millisFastest / 1000);
            int minutesFastest = secondsFastest / 60;
            secondsFastest = secondsFastest % 60;

            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));

            timerHandler.postDelayed(this, 500);
        }
    };

    // Fragment Constructor
    public static ControlFragment newInstance(int index) {
        ControlFragment fragment = new ControlFragment();
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
        // inflate
        View root = inflater.inflate(R.layout.activity_control, container, false);

        // get shared preferences
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);


        // variable initialization
        exploreTimeTextView = root.findViewById(R.id.exploreTimeTextView);
        imageTimeTextView = root.findViewById(R.id.imageTimeTextView);
        fastestTimeTextView = root.findViewById(R.id.fastestTimeTextView);
        exploreButton = root.findViewById(R.id.exploreToggleBtn);
        imageButton = root.findViewById(R.id.imageToggleBtn);
        fastestButton = root.findViewById(R.id.fastestToggleBtn);
        exploreResetButton = root.findViewById(R.id.exploreResetImageBtn);
        imageResetButton = root.findViewById(R.id.imageResetImageBtn);
        fastestResetButton = root.findViewById(R.id.fastestResetImageBtn);

        robotStatusTextView = MainActivity.getRobotStatusTextView();
        fastestTimer = 0;
        exploreTimer = 0;
        imageTimer=0;

        //mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        gridMap = MainActivity.getGridMap();

        // Button Listener
        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked exploreToggleBtn");
                ToggleButton exploreToggleBtn = (ToggleButton) v;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    showToast("Exploration timer stop!");
                    robotStatusTextView.setText("Exploration Stopped");
                    timerHandler.removeCallbacks(timerRunnableExplore);
                    //gridMap.findMDF();
                    //gridMap.stopExplore=true;
                }
                else if (exploreToggleBtn.getText().equals("STOP")) {
                    showToast("Exploration timer start!");
                    MainActivity.printMessage("ES|");
                    robotStatusTextView.setText("Exploration Started");
                    gridMap.setUnSetCellStatus(true);
                    exploreTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableExplore, 0);
                    //gridMap.stopExplore=false;
                }
                else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                showLog("Exiting exploreToggleBtn");
            }
        });
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked imageToggleBtn");
                ToggleButton imageToggleBtn = (ToggleButton) v;
                if (imageToggleBtn.getText().equals("IMAGE")) {
                    showToast("Image timer stop!");
                    robotStatusTextView.setText("Image recognition Stopped");
                    timerHandler.removeCallbacks(timerRunnableImage);
                }
                else if (imageToggleBtn.getText().equals("STOP")) {
                    showToast("Image timer start!");
                    MainActivity.printMessage("IS|");
                    robotStatusTextView.setText("Image recognition Started");
                    imageTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableImage, 0);
                }
                else {
                    showToast("Else statement: " + imageToggleBtn.getText());
                }
                showLog("Exiting imageToggleBtn");
            }
        });

        fastestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked fastestToggleBtn");
                ToggleButton fastestToggleBtn = (ToggleButton) v;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    showToast("Fastest timer stop!");
                    robotStatusTextView.setText("Fastest Path Stopped");
                    timerHandler.removeCallbacks(timerRunnableFastest);
                }
                else if (fastestToggleBtn.getText().equals("STOP")) {
                    showToast("Fastest timer start!");
                    MainActivity.printMessage("FS|");
                    robotStatusTextView.setText("Fastest Path Started");
                    fastestTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableFastest, 0);
                }
                else
                    showToast(fastestToggleBtn.getText().toString());
                showLog("Exiting fastestToggleBtn");
            }
        });

        exploreResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked exploreResetImageBtn");
                showToast("Resetting exploration time...");
                exploreTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if(exploreButton.isChecked())
                    exploreButton.toggle();
                timerHandler.removeCallbacks(timerRunnableExplore);
                showLog("Exiting exploreResetImageBtn");            }
        });

        imageResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked imageResetImageBtn");
                showToast("Resetting image recognition time...");
                imageTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if(imageButton.isChecked())
                    imageButton.toggle();
                timerHandler.removeCallbacks(timerRunnableImage);
                showLog("Exiting imageResetImageBtn");            }
        });

        fastestResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked fastestResetImageBtn");
                showToast("Resetting fastest time...");
                fastestTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if (fastestButton.isChecked())
                    fastestButton.toggle();
                timerHandler.removeCallbacks(timerRunnableFastest);
                showLog("Exiting fastestResetImageBtn");            }
        });

        return root;
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    /*Handler sensorHandler = new Handler();
    boolean sensorFlag= false;

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this,1000);
        }
    };

    @Override
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
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        try{
            mSensorManager.unregisterListener(ControlFragment.this);
        } catch(IllegalArgumentException e){
            e.printStackTrace();
        }
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0, 0);
        toast.show();
    }*/


    /*class MyTimerTask extends TimerTask
    {
        public void run()
        {
            if (counter>1)
                robotStatusTextView.setText("stopped");
            if (counter==0)
                robotStatusTextView.setText("Not Available");
        }
    }*/
}
