package com.example.mdpandroid.leaderboard;

import static com.example.mdpandroid.map.BoardMap.TARGET_CELL_CODE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import biz.laenger.android.vpbs.ViewPagerBottomSheetDialogFragment;
import com.example.mdpandroid.R;
import com.example.mdpandroid.map.MapCanvas;
import com.example.mdpandroid.map.Target;

public class MapConfigDialog extends ViewPagerBottomSheetDialogFragment {
    Toolbar bottomSheetToolbar;
    Button saveBtn;
    LinearLayout linearLayout;

    public static final String RPI_COMMAND_READ_OBS = "OBS";

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);
        final View contentView = View.inflate(getContext(), R.layout.dialog_map_config, null);
        bottomSheetToolbar = contentView.findViewById(R.id.config_bottom_sheet_toolbar);
        bottomSheetToolbar.setTitle("Map Configurations");
        saveBtn = contentView.findViewById(R.id.btn_save);
        linearLayout = (LinearLayout) contentView.findViewById(R.id.linear_scroll_savedMap);
        dialog.setContentView(contentView);

        //getActivity().getPreferences(Context.MODE_PRIVATE).edit().clear().commit(); //clear all saved obs
        MapCanvas _map = (MapCanvas) getActivity().findViewById(R.id.pathGrid);
        if (_map.getFinder().getTargets().size() <= 0) saveBtn.setVisibility(View.GONE);

        saveBtn.setOnClickListener(view -> {
            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

            int n = 0;
            String message = "";
            while (n < _map.getFinder().getTargets().size()) {
                message += (n+1) + "," + _map.getFinder().getTargets().get(n).getX() + "," + (21-_map.getFinder().getTargets().get(n).getY()) + "," + _map.getFinder().getTargets().get(n).getF() + "|";
                n++;
            }

            if (!message.equals("")) {
                message = StringUtils.removeEnd(message, "|");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString(message, message);
                editor.apply();
                readSavedMaps(_map);
            }
        });
        readSavedMaps(_map);
    }

    private void readSavedMaps(MapCanvas _map) {
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPref.getAll();
        linearLayout.removeAllViews();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Button button = new Button(getContext());
            button.setText(entry.getValue().toString());
            button.setBackgroundColor(getResources().getColor(R.color.black_bg));
            button.setTextColor(getResources().getColor(R.color.blue_hl));
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 5);
            button.setLayoutParams(params);
            button.setOnClickListener(view -> {
                String msg = (String) button.getText();
                String[] obss = msg.split("\\|");
                _map.getFinder().resetGrid();
                for(int i=0; i <= obss.length-1 ; i++) {
                    String[] xy = obss[i].split(",");
                    _map.getFinder().getTargets().add(new Target(Integer.parseInt(xy[1]), 21-Integer.parseInt(xy[2]), Integer.parseInt(xy[0])-1, Integer.parseInt(xy[3])));
                    _map.getFinder().getBoard()[_map.getFinder().getTargets().get(i).getX()][_map.getFinder().getTargets().get(i).getY()] = TARGET_CELL_CODE;
                }
            });
            button.setOnLongClickListener(view -> {
                sharedPref.edit().remove((String) button.getText()).commit();
                linearLayout.removeView(button);
                return true;
            });
            linearLayout.addView(button);
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        getActivity().findViewById(R.id.toolbar_top).setVisibility(View.VISIBLE);
    }
}
