package com.example.mdpandroid.leaderboard;

import android.annotation.SuppressLint;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import androidx.appcompat.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import biz.laenger.android.vpbs.ViewPagerBottomSheetBehavior;
import biz.laenger.android.vpbs.ViewPagerBottomSheetDialogFragment;
import com.example.mdpandroid.R;
//import com.example.mdpandroid.bluetooth.MessageFragment;
import com.example.mdpandroid.map.BoardMap;
import com.example.mdpandroid.map.MapCanvas;

/***********************************
 * Note to future self or those who
 * end up have to read this file:
 *
 * It's all spaghetti code.
 * Just give up. Or re-write.
 ***********************************/

@SuppressLint("ValidFragment")
    public class TimerDialogFragment extends ViewPagerBottomSheetDialogFragment {
    static CountDownTimer timer;
    Toolbar bottomSheetToolbar;
    Button btnTimer;
    Button btnClose;
    TextView timeLbl;
    TextView swipeLbl;
    TextView checkPointLbl;
    private ViewPagerBottomSheetBehavior mBehavior;

    boolean hasBegan = false;
    String currentTime = "";
    String runType = "";
    int confirmCount = 3;
    boolean btnStartTouch = false;

    public static final String BLUETOOTH_RUN_DONE = "DONE";

    @SuppressLint("ValidFragment")
    public TimerDialogFragment(String runType) {
        this.runType = runType;
    }

    @SuppressLint({"RestrictedApi", "ClickableViewAccessibility"})
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        final View contentView = View.inflate(getContext(), R.layout.dialog_timer, null);
        bottomSheetToolbar = contentView.findViewById(R.id.bottom_sheet_toolbar);
        btnTimer = contentView.findViewById(R.id.btn_timer);
        btnClose = contentView.findViewById(R.id.btn_close_timer);
        timeLbl = contentView.findViewById(R.id.txtTimer);
        swipeLbl = contentView.findViewById(R.id.txtLblStopTimer);
        bottomSheetToolbar.setTitle(runType);
        currentTime = (String) timeLbl.getText();
        checkPointLbl = contentView.findViewById(R.id.txtTargetCheckpoint);

        dialog.setContentView(contentView);

        LinearLayout ll = contentView.findViewById(R.id.ll_padding);
        if (runType.equals("Fastest Path Run")) {
            ll.setVisibility(View.VISIBLE);
            ll.setMinimumHeight(650);
        } else {
            ll.setVisibility(View.GONE);
        }

        setupTimerButton();

        btnTimer.setOnClickListener(view -> {
            hasBegan = !hasBegan;
            setupTimerButton();
        });

        // I TRIED MY BEST TO PREVENT ACCIDENTAL TOUCH SLIPS
        btnTimer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        dialog.setCancelable(false);
                        btnStartTouch = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        if (btnStartTouch && !hasBegan) dialog.setCancelable(false);
                        else {
                            dialog.setCancelable(true);
                            btnStartTouch = false;
                        }
                        break;
                }
                return false;
            }
        });

        btnClose.setOnClickListener(this::setupStopButton);
    }

    private void setupTimerButton() {
        btnTimer.setText(hasBegan ? "Stop"
                : !currentTime.equals("00:00:00") ? "Restart"
                : "Start");
        btnTimer.setTextColor(hasBegan ? getResources().getColor(R.color.red) : getResources().getColor(R.color.yellow));
        btnClose.setVisibility(hasBegan || currentTime.equals("00:00:00") ? View.GONE : View.VISIBLE);
        swipeLbl.setVisibility(hasBegan || currentTime.equals("00:00:00") ? View.GONE : View.VISIBLE);
        this.setCancelable(btnTimer.getText().equals("Start"));
        if (hasBegan) {
//            MessageFragment.sendMessage("LDRB -> RPI:\t\t", runType.equals("Image Recognition Run") ? "BANANAS" : "LEMON");

            final long totalTime = 30 * 60 * 1000;//30 mins
            final TextView textView = (TextView) timeLbl;
            this.confirmCount = 3;
            setupStopButton(btnClose);
            timer = new CountDownTimer(totalTime, 10) {
                @SuppressLint("SetTextI18n")
                public void onTick(long millisUntilFinished) {
                    int secondsInt = (int) ((totalTime - millisUntilFinished) / 1000 % 60);
                    int msInt = (int) (millisUntilFinished%100);
                    String seconds, mssecs;
                    seconds = secondsInt < 10 ? "0" + secondsInt : "" + secondsInt;
                    mssecs = msInt < 10 ? "0" + msInt : "" + msInt;

                    String mins = "0" + ((totalTime - millisUntilFinished) / 1000 / 60) + "";
                    currentTime = mins + ":" + seconds + ":" + mssecs;
                    textView.setText(currentTime);
                }

                public void onFinish() { timer = null; }

            };
            timer.start();
        } else {
            if (timer != null) timer.cancel();
        }
    }

    private void setupStopButton(View view) {
        switch(this.confirmCount){
            case 3:
                ((Button) view).setText("Tap here 3 times to close");
                break;
            case 2:
                ((Button) view).setText("Confirm Close??");
                break;
            case 1:
                ((Button) view).setText("Close");
                break;
            case 0:
                this.setCancelable(true);
                this.dismiss();
        }
        if (this.confirmCount <= 3) this.confirmCount--;
    }

    @SuppressLint("SetTextI18n")
    public void updateCheckPointLbl(int obsId, int targetId) {
        checkPointLbl.setText(checkPointLbl.getText() + "O" + obsId + "_T" + targetId + "->" + currentTime + "\t\t");
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getActivity().findViewById(R.id.toolbar_top).setVisibility(View.VISIBLE);
        BoardMap _map = ((MapCanvas) getActivity().findViewById(R.id.pathGrid)).getFinder();
        _map.defaceTargets();
    }

    public void setBegan(boolean b) {
        this.hasBegan = b;
        setupTimerButton();
    }

    public String getRunType() { return runType; }
}