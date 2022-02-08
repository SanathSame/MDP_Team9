package com.example.mdpandroid.bluetooth;

import static com.example.mdpandroid.leaderboard.MapConfigDialog.RPI_COMMAND_READ_OBS;
import static com.example.mdpandroid.leaderboard.TimerDialogFragment.BLUETOOTH_RUN_DONE;
import static com.example.mdpandroid.map.Robot.ROBOT_COMMAND_POS;
import static com.example.mdpandroid.map.Robot.ROBOT_MOTOR_FORWARD;
import static com.example.mdpandroid.map.Robot.ROBOT_MOTOR_REVERSE;
import static com.example.mdpandroid.map.Robot.ROBOT_MOTOR_STOP;
import static com.example.mdpandroid.map.Robot.STM_COMMAND_FORWARD;
import static com.example.mdpandroid.map.Robot.STM_COMMAND_REVERSE;
import static com.example.mdpandroid.map.Robot.STM_COMMAND_STOP;
import static com.example.mdpandroid.map.Target.BLUETOOTH_TARGET_IDENTIFIER;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.mdpandroid.MainActivity;
import com.example.mdpandroid.R;
import com.example.mdpandroid.leaderboard.TimerDialogFragment;
import com.example.mdpandroid.map.BoardMap;
import com.example.mdpandroid.map.MapCanvas;
import com.example.mdpandroid.map.Target;

public class MessageFragment extends Fragment {

    private static String statusWindowTxt = "";
    private static TimerDialogFragment timerDialog;

    Button send;
    static TextView messageReceivedTextView;
    EditText typeBoxEditText;
    static ScrollView scrollView;

    // initializations
    public MessageFragment() {}

    public static TimerDialogFragment getTimerDialog(String runType) {
        timerDialog = new TimerDialogFragment(runType);
        return timerDialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message, container, false);
        send = (Button) rootView.findViewById(R.id.messageButton);
        messageReceivedTextView = (TextView) rootView.findViewById(R.id.messageReceivedTextView);
        typeBoxEditText = (EditText) rootView.findViewById(R.id.typeBoxEditText);
        scrollView = (ScrollView) rootView.findViewById(R.id.scrollView2D);

        //messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());


        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String input = "" + typeBoxEditText.getText().toString();
                sendMessage("TXTB -> RPI:\t\t", input);
                typeBoxEditText.setText("");
            }
        });

        return rootView;
    }

    public static void sendMessage(String prefix, String txt) {
        BluetoothService.getInstance(null, null).sendMessage(txt);
        statusWindowTxt += prefix + txt + '\n';
        messageReceivedTextView.setText(statusWindowTxt);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    public static void addSeparator() {
        statusWindowTxt += "----------------------------------------------------" + '\n';
        messageReceivedTextView.setText(statusWindowTxt);
    }

    public static void receiveMessage(Activity activity, String msg) {
        try {
            BoardMap _map = ((MapCanvas) activity.findViewById(R.id.pathGrid)).getFinder();

            statusWindowTxt += "RPI -> BLTH:\t\t" + msg + "\n";
            statusWindowTxt = statusWindowTxt.replace("\n\n", "\n");

            String[] parts = msg.replace("\n", "").replace("\r", "").split("\\|");
            switch(parts[0]) {
                case STM_COMMAND_FORWARD:
                case STM_COMMAND_REVERSE:
                    _map.getRobo().motorRotate(msg.equals("f") ? ROBOT_MOTOR_FORWARD : ROBOT_MOTOR_REVERSE);
                    MessageFragment.sendMessage("MFIN -> RPI:\t\t", STM_COMMAND_STOP);
                    MessageFragment.addSeparator();
                    _map.getRobo().setMotor(ROBOT_MOTOR_STOP);
                    break;
                case BLUETOOTH_TARGET_IDENTIFIER:
                    if (timerDialog.getRunType().equals("Image Recognition Run")) {
                        int targetid = Integer.parseInt(parts[1]);
                        int imageid = Integer.parseInt(parts[2]);
                        Target t = _map.getTargets().get(targetid-1);
                        t.setImg(imageid);
                        timerDialog.updateCheckPointLbl(targetid, imageid);
                        if (_map.hasReceivedAllTargets()) timerDialog.setBegan(false);
                    }
                    break;
                case BLUETOOTH_RUN_DONE:
                    timerDialog.setBegan(false);
                    break;
                case RPI_COMMAND_READ_OBS:
                    SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
                    String obss = "";
                    for(int i=0; i < parts.length; i++) {
                        String obs = parts[i].replace("OBS", "").replace("[", "").replace("]", "|");
                        if (!obs.equals(""))
                            obss += obs;
                    }
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString(obss, obss);
                    editor.apply();
                    break;
                case ROBOT_COMMAND_POS:
                    _map.getRobo().setX(Integer.parseInt(parts[1]));
                    _map.getRobo().setY(20-Integer.parseInt(parts[2]));
                    _map.getRobo().setFacing(Integer.parseInt(parts[3]));
                    ((MainActivity)activity).updateRoboStatus();
                    break;
            }

            messageReceivedTextView.setText(statusWindowTxt);
            scrollView.fullScroll(View.FOCUS_DOWN);
        } catch (Exception e) {
            //user switched fragment
            return;
        }
    }
}
