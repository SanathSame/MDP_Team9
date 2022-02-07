package com.example.mdpandroid;

import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import org.json.JSONException;

public class WaypointFragment extends DialogFragment {

    private static final String TAG = "WaypointFragment";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    GridMap gridMap;

    Button saveBtn, cancelBtn;
    String direction = "";
    View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        showLog("Entering onCreateView");
        rootView = inflater.inflate(R.layout.activity_waypoint, container, false);
        super.onCreate(savedInstanceState);

        //getDialog().setTitle("Edit Waypoint");
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        saveBtn = rootView.findViewById(R.id.saveBtn);
        cancelBtn = rootView.findViewById(R.id.cancelBtn);

        direction = sharedPreferences.getString("direction","");

        if (savedInstanceState != null)
            direction = savedInstanceState.getString("direction");


        final Spinner spinnerx = (Spinner) rootView.findViewById(R.id.waypointXDropdownSpinner);
        final Spinner spinnery = (Spinner) rootView.findViewById(R.id.waypointYDropdownSpinner);

        Integer[] x_array = new Integer[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14};
        Integer[] y_array = new Integer[]{0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};

        ArrayAdapter<Integer> adapterx = new ArrayAdapter<Integer>(getActivity(),android.R.layout.simple_spinner_item,x_array);
        // Specify the layout to use when the list of choices appears
        adapterx.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnerx.setAdapter(adapterx);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<Integer> adaptery = new ArrayAdapter<Integer>(getActivity(),android.R.layout.simple_spinner_item,y_array);
        // Specify the layout to use when the list of choices appears
        adaptery.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinnery.setAdapter(adaptery);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked saveBtn");
                int waypointX = (Integer) spinnerx.getSelectedItem();
                int waypointY = (Integer) spinnery.getSelectedItem();

                //editor.putString("direction",direction);
                try {
                    ((MainActivity)getActivity()).refreshWaypoint(waypointX,waypointY);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //gridMap.setWaypointCoordManual(waypointX,waypointY);

                Toast.makeText(getActivity(), "Saving waypoint: "+"x:"+waypointX+", y:"+waypointY, Toast.LENGTH_SHORT).show();
                showLog("Exiting saveBtn");
                editor.commit();
                getDialog().dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked cancelDirectionBtn");
                showLog( "Exiting cancelDirectionBtn");
                getDialog().dismiss();
            }
        });
        showLog("Exiting onCreateView");
        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);
        saveBtn = rootView.findViewById(R.id.saveBtn);
        showLog("Exiting onSaveInstanceState");
        outState.putString(TAG, direction);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        showLog("Entering onDismiss");
        super.onDismiss(dialog);
        showLog("Exiting onDismiss");
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }
}
