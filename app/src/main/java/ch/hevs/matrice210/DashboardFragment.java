package ch.hevs.matrice210;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
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
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);
        findViewById(R.id.btn_pilot_interface).setOnClickListener(this);
        findViewById(R.id.btn_moc_interface).setOnClickListener(this);
        TextView versionText = (TextView) findViewById(R.id.version);
        versionText.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        EditText editTxt_bridge = (EditText) findViewById(R.id.editTxt_bridge);
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pilot_layout, container, false);
        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated( savedInstanceState);
    }

    @Override
    public void onAttach( Context activity) {
        super.onAttach( activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update(Observable observable, Object o) {
        // Try catch cause the fragment is not necessary visible
        try {

        }catch (Exception e) {}
    }

    @Override
    public void onClick(View view) {
        Class nextActivityClass;
        Intent intent;
        switch (view.getId()) {
            case R.id.btn_pilot_interface:
                nextActivityClass = PilotActivity.class;
                intent = new Intent(this, nextActivityClass);
                startActivity(intent);
                break;
            case R.id.btn_moc_interface:
                nextActivityClass = MocActivity.class;
                intent = new Intent(this, nextActivityClass);
                startActivity(intent);
                break;
        }
    }
}