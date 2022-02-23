package com.example.mdpandroid.start;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.mdpandroid.R;
import com.example.mdpandroid.map.BoardMap;
import java.nio.charset.Charset;
import com.example.mdpandroid.leaderboard.TimerDialogFragment;
import com.example.mdpandroid.map.Target;

public class MessageBox extends Fragment {
    private static final String TAG = "TARGET MAP";
    private static SharedPreferences sharedPreferences;
    private BoardMap _map = new BoardMap();
    private BoardMap mapPass = new BoardMap();
    private static String statusWindowTxt = "";
    private static Context context;
    private static SharedPreferences.Editor editor;
    Button send;
    public static TextView messageSentTextView, messageReceivedTextView;
    static EditText typeBoxEditText;
    static ScrollView scrollView;
    private static TimerDialogFragment timerDialog;




    public static void addSeparator() {
        statusWindowTxt += "----------------------------------------------------" + '\n';
        messageSentTextView.setText(statusWindowTxt);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootview = inflater.inflate(R.layout.messagebox, container, false);
        send = (Button) rootview.findViewById(R.id.messageButton);
        messageSentTextView = (TextView) rootview.findViewById(R.id.messageSentTextView);
        messageReceivedTextView = (TextView) rootview.findViewById(R.id.editText);
        typeBoxEditText = (EditText) rootview.findViewById(R.id.typeBoxEditText);
        scrollView = (ScrollView) rootview.findViewById(R.id.scrollView2D);


        send.setOnClickListener(view -> {
            String input = "" + typeBoxEditText.getText().toString();
            sendMessage("ANDROID -> RPI:\t\t", input);
            typeBoxEditText.setText("");
        });
        return rootview;
    }

    public static void sendMessage(String prefix, String txt) {
        String sentText = prefix + txt;
        statusWindowTxt += sentText + "\n";
        messageSentTextView.setText(statusWindowTxt);
        if (Bluetoothservice.BluetoothConnectionStatus) {
            byte[] bytes = sentText.getBytes(Charset.defaultCharset());
            Bluetoothservice.write(bytes);
        }
        System.out.println(sentText);
    }

    public static void refreshMessageReceived() {
        messageReceivedTextView.setText(sharedPreferences.getString("message", ""));
    }

    public void refreshMessageReceivedfromblue() {
        Bluetoothconnection.getMessageReceivedtext().setText(sharedPreferences.getString("message", ""));
    }
    public static TimerDialogFragment getTimerDialog(String runType) {
        timerDialog = new TimerDialogFragment(runType);
        return timerDialog;
    }
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("try", "catch");
            String message = intent.getStringExtra("receivedmessage");
//            refreshMessageReceivedfromblue();
            Log.d("summit", "ye");
            refreshMessageReceived();
//            String[] parts = message.split(",");
//            String type = parts[0];
//            int index;
//            switch (type) {
            int j;
            String[] cases = {"TARGET", "ROBOT", "INSTRUCTIONS"};
            String[] parts;
            for(j = 0; j < cases.length; j++)
                if(message.contains(cases[j]))
                    break;
            switch(j) {
                case 0: {
//                    Log.d(TAG, "Interpreting TARGET message");
//                    try {
//                        String obsID = parts[1];
//                        String targetID = parts[2];
//                        if (targetID.contains("TARGET"))
//                            if (targetID.indexOf("T") != 0) {
//                                index = targetID.indexOf("T");
//                                targetID = targetID.substring(0, index);
//                            }
//                        //TODO code to set target ID from obstacle ID
//
//                    } catch (Exception e) {
//                        Log.d(TAG, "Invalid message");
//                        e.printStackTrace();
//                    }
                    parts = message.replace(" ","").replace("TARGET", "").split(",");
                    int targetid = Integer.parseInt(parts[0]);
                    int imageid = Integer.parseInt(parts[1]);
                    Target t = _map.getTargets().get(targetid-1);
                    t.setImg(imageid);
                    break;
                }
                case 1: {
//                    Log.d(TAG, "Interpreting ROBOT message");
//                    try {
//                        int x = Integer.parseInt(parts[1]);
//                        int y = Integer.parseInt(parts[2]);
//                        String direction = parts[3];
//                        //TODO set robot location based on input location
//                    } catch (Exception e) {
//                        Log.d(TAG, "Invalid message");
//                        e.printStackTrace();
//                    }
                    parts = message.replace(" ","").replace("ROBOT", "").split(",");
                    _map.getRobo().setX(Integer.parseInt(parts[0]));
                    _map.getRobo().setY(20-Integer.parseInt(parts[1]));
                    _map.getRobo().setFacing(Integer.parseInt(parts[2])); //0123 NSEW
                    break;
                }
                case 2: {
                    Log.d(TAG, "Interpreting ROBOT message");
                    parts = message.split(",");
                    messageReceivedTextView.setText(parts.toString());
                    break;
                }
                default: {
                    Log.d(TAG, "Invalid format: unable to recognize message type");
                }
            }
        }
    };

    public void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(MessageBox.context).unregisterReceiver(messageReceiver);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }
}


