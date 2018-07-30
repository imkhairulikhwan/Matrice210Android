package ch.hevs.matrice210;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.hevs.matrice210.Interfaces.MocInteraction;
import ch.hevs.matrice210.Interfaces.MocInteractionListener;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainFragmentActivity extends FragmentActivity
        implements DashboardFragment.DashboardInteractionListener, MocInteractionListener, View.OnClickListener {
    // Fragment
     private FragmentManager fragmentManager;
    private DashboardFragment dashboardFragment;
    private PilotFragment pilotFragment;
    private MissionFragment missionFragment;
    private MocInteraction mocInteraction;

    public enum fragments {
        dashboard,
        pilot,
        mission
    }

    // UI
    private TextView txtView_ob_state;

    // DJI
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private FlightController mFlightController = null;
    private List<String> missingPermission = new ArrayList<>();

    // Watchdog
    final int watchdogLimit = 3;
    private int watchdogCnt = watchdogLimit;
    private Handler watchdogHandler;
    private Runnable watchdogRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                sendData("#w");
                watchdogCnt++;
                // watchdogCnt is reset on ack from Ob computer.
                if(watchdogCnt >= watchdogLimit) {
                    watchdogCnt = watchdogLimit;
                    setObState(false);
                } else {
                    setObState(true);
                }
            } finally {
                watchdogHandler.postDelayed(watchdogRunnable, 500);
            }
        }
    };

    private void setObState(final boolean state) {
        ((Activity)this).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(state) {
                    txtView_ob_state.setText(getString(R.string.ob_state_connected));
                    txtView_ob_state.setTextColor(Color.GREEN);
                } else {
                    txtView_ob_state.setText(getString(R.string.ob_state_disconnected));
                    txtView_ob_state.setTextColor(Color.RED);
                }
            }
        });
    }

    private final int REQUEST_PERMISSION_CODE = 12345;
    private final String[] REQUIRED_PERMISSION_LIST = new String[] {
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


    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {
        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                toast("SDK registration succeeded!");

                loginAccount();
            } else {
                toast("SDK registration failed, check network and retry!");
            }
        }

        @Override
        public void onProductChange(BaseProduct oldProduct, BaseProduct newProduct) {
            if(newProduct != null) {
                toast("Aircraft connected");
                Aircraft mAircraft = (Aircraft)newProduct;
                mFlightController = mAircraft.getFlightController();
                if(mFlightController != null) {
                    mFlightController.setOnboardSDKDeviceDataCallback(new FlightController.OnboardSDKDeviceDataCallback() {
                        @Override
                        public void onReceive(byte[] bytes) {
                            boolean redirectReceivedData = true;
                            if(bytes.length == 2) {
                                StringBuilder buffer = new StringBuilder();
                                for (byte b : bytes) {
                                    buffer.append((char) b);
                                }
                                // Reset watchdogCnt on ack from Ob computer
                                if(buffer.toString().contentEquals(getString(R.string.moc_command_watchdog))) {
                                    watchdogCnt = 0;
                                    redirectReceivedData = false;
                                }
                            }

                            // Redirect data to active fragment
                            if(mocInteraction != null && redirectReceivedData)
                                mocInteraction.dataReceived(bytes);
                        }
                    });
                    startWatchdog();
                }
            } else {
                toast("Aircraft disconnected");
                stopWatchdog();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainfragment);

        // UI
        Button btn_emergencyStop = (Button) findViewById(R.id.btn_emergencyStop);
        btn_emergencyStop.setOnClickListener(this);
        txtView_ob_state = (TextView) findViewById(R.id.txtView_ob_state);

        // Fragment
        fragmentManager = getSupportFragmentManager();
        dashboardFragment = new DashboardFragment();
        pilotFragment = new PilotFragment();
        missionFragment = new MissionFragment();
        fragmentManager.beginTransaction().replace(R.id.main_container_fragment, dashboardFragment).commit();

        checkAndRequestPermissions();


        watchdogHandler = new Handler();
        //*/
    }

    @Override
    protected void onDestroy() {
        // Prevent memory leak by releasing DJISDKManager's references to this activity
        if (DJISDKManager.getInstance() != null) {
            DJISDKManager.getInstance().destroy();
        }
        super.onDestroy();
    }

    @Override
    public void handleBridgeIP(final String bridgeIP) {
        try {
            DJISDKManager.getInstance().enableBridgeModeWithBridgeAppIP(bridgeIP);
            if (!TextUtils.isEmpty(bridgeIP)) {
                toast("BridgeMode ON!\nIP: " + bridgeIP);
            }
        } catch (IllegalThreadStateException e) {
            toast("BridgeMode failed, please restart");
        }
    }

    @Override
    public void changeFragment(fragments fragment) {
        Fragment nextFragment = null;
        switch (fragment) {
            case dashboard:
                nextFragment = dashboardFragment;
                break;
            case pilot:
                nextFragment = pilotFragment;
                break;
            case mission:
                nextFragment = missionFragment;
                break;
        }

        if(nextFragment != null) {
            fragmentManager.beginTransaction().replace(R.id.main_container_fragment, nextFragment).addToBackStack(fragment.toString()).commit();
            mocInteraction = null;
            try {
                mocInteraction = (MocInteraction) nextFragment;
            } catch (ClassCastException e) {
               System.err.println(nextFragment.toString() + " must implement MocInteraction");
            }
        } else {
            System.err.println("Fragment not found");
        }
    }

    @Override
    public void sendData(String data) {
        sendData(data.getBytes());
    }

    @Override
    public void sendData(byte[] data) {
        if (mFlightController != null) {
            mFlightController.sendDataToOnboardSDKDevice(data, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(mocInteraction != null)
                        mocInteraction.onResult(djiError);
                }
            });
        } else {
            toast("sendMocData error - No aircraft connected");
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.btn_emergencyStop:
                toast("Emergency stop");
                sendData(getString(R.string.moc_command_emergencyStop));
                break;
        }
    }

    private void loginAccount(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        toast("Login succeeded!");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        toast("Login failed!");
                    }
                });
    }

    void startWatchdog() {
        watchdogRunnable.run();
    }

    void stopWatchdog() {
        watchdogHandler.removeCallbacks(watchdogRunnable);
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
            toast("Missing permissions!!!");
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

    public void toast(final String text)
    {
        ((Activity)this).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                System.out.println(text);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}