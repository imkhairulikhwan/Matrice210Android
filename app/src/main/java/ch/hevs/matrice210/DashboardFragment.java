package ch.hevs.matrice210;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import dji.sdk.sdkmanager.DJISDKManager;

public class DashboardFragment extends Fragment implements Observer, View.OnClickListener {
    // UI Elements
    private EditText editTxt_bridge;

    // Listener
    private DashboardInteractionListener dashboardIListener;

    public interface DashboardInteractionListener {
        void handleBridgeIP(final String bridgeIp);
        void changeFragment(final MainFragmentActivity.fragments fragment);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dashboard__layout, container, false);
        view.findViewById(R.id.btn_pilot_interface).setOnClickListener(this);
        view.findViewById(R.id.btn_mission_interface).setOnClickListener(this);
        TextView versionText = (TextView) view.findViewById(R.id.version);
        versionText.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        editTxt_bridge = (EditText) view.findViewById(R.id.editTxt_bridge);
        editTxt_bridge.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || event != null
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event != null && event.isShiftPressed()) {
                        return false;
                    } else {
                        // the user is done typing.
                        handleBridgeIPTextChange();
                    }
                }
                return false; // pass on to other listeners.
            }
        });
        editTxt_bridge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    handleBridgeIPTextChange();
                }
            }
        });
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated( savedInstanceState);
    }

    @Override
    public void onAttach( Context activity) {
        super.onAttach( activity);
        try {
            dashboardIListener = (DashboardInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement DashboardInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        dashboardIListener = null;
    }

    @Override
    public void update(Observable observable, Object o) {
        // Try catch cause the fragment is not necessary visible
        try {

        }catch (Exception e) {}
    }

    public void handleBridgeIPTextChange() {
        String bridgeIP = editTxt_bridge.getText().toString();
        if (!TextUtils.isEmpty(bridgeIP)) {
            // Remove new line character
            if (bridgeIP.contains("\n")) {
                bridgeIP = bridgeIP.substring(0, bridgeIP.indexOf('\n'));
                editTxt_bridge.setText(bridgeIP);
            }
            dashboardIListener.handleBridgeIP(bridgeIP);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_pilot_interface:
                dashboardIListener.changeFragment(MainFragmentActivity.fragments.pilot);
                break;
            case R.id.btn_mission_interface:
                dashboardIListener.changeFragment(MainFragmentActivity.fragments.mission);
                break;
        }
    }
}