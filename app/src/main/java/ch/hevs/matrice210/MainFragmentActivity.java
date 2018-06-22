package ch.hevs.matrice210;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainFragmentActivity extends FragmentActivity implements View.OnClickListener{
    // Fragment
    private FragmentManager fragmentManager;
    private DashboardFragment dashboardFragment;
    private PilotActivity pilotActivity;
    private MocFragment mocFragment;

    // DJI
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private Aircraft mAircraft = null;
    private FlightController mFlightController = null;
    private List<String> missingPermission = new ArrayList<>();

    // UI Elements
    private EditText editTxt_bridge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainfragment);

        dashboardFragment = new DashboardFragment();
        pilotActivity = new PilotActivity();
        mocFragment = new MocFragment();

        fragmentManager.beginTransaction().replace(R.id.main_container_fragment, dashboardFragment).commit();
        //fragmentManager.beginTransaction().replace(R.id.main_container_fragment, pilotActivity).addToBackStack(null).commit();

        findViewById(R.id.btn_pilot_interface).setOnClickListener(this);
        findViewById(R.id.btn_moc_interface).setOnClickListener(this);
        TextView versionText = (TextView) findViewById(R.id.version);
        versionText.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        editTxt_bridge = (EditText) findViewById(R.id.editTxt_bridge);
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

        checkAndRequestPermissions();
    }

    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                Toast.makeText(getApplicationContext(), "SDK registration succeeded!", Toast.LENGTH_LONG).show();

                loginAccount();
            } else {
                Toast.makeText(getApplicationContext(),
                        "SDK registration failed, check network and retry!",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            if(newProduct != null) {
                Toast.makeText(getApplicationContext(), "Aircraft connected", Toast.LENGTH_SHORT).show();
                mAircraft = (Aircraft)newProduct;
                mFlightController = mAircraft.getFlightController();
                if(MainActivity.mFlightController != null) {
                    MainActivity.mFlightController.setOnboardSDKDeviceDataCallback(new FlightController.OnboardSDKDeviceDataCallback() {
                        @Override
                        public void onReceive(byte[] bytes) {
                            //log(bytes.toString());
                        }
                    });
                }
            } else {
                Toast.makeText(getApplicationContext(), "Aircraft disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private static final String[] REQUIRED_PERMISSION_LIST = new String[] {
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    private void handleBridgeIPTextChange() {
        // the user is done typing.
        String bridgeIP = editTxt_bridge.getText().toString();
        if (bridgeIP != null) {
            // Remove new line characcter
            if(bridgeIP.toString().contains("\n")) {
                bridgeIP = bridgeIP.substring(0, bridgeIP.indexOf('\n'));
                editTxt_bridge.setText(bridgeIP);
            }
            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
            if (!TextUtils.isEmpty(bridgeIP)) {
                Toast.makeText(getApplicationContext(), "BridgeMode ON!\nIP: " + bridgeIP, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Prevent memory leak by releasing DJISDKManager's references to this activity
        if (DJISDKManager.getInstance() != null) {
            DJISDKManager.getInstance().destroy();
        }
        super.onDestroy();
    }

    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Toast.makeText(getApplicationContext(), "Login succeeded!", Toast.LENGTH_LONG).show();
                        handleBridgeIPTextChange();
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        Toast.makeText(getApplicationContext(), "Login failed!", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            Toast.makeText(getApplicationContext(), "Missing permissions!!!", Toast.LENGTH_LONG).show();
        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    DJISDKManager.getInstance().registerApp(MainFragmentActivity.this, registrationCallback);
                }
            });
        }
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

    public void toast(final String str)
    {
        Toast.makeText( getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }
}