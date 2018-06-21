package ch.hevs.matrice210;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.util.CommonCallbacks;

public class MocActivity extends Activity implements View.OnClickListener {
    private Button btn_send, btn_led;
    private TextView txtView_console;
    EditText editTxt_command;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moc);
        btn_send = (Button) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        btn_led  = (Button) findViewById(R.id.btn_led);
        btn_led.setOnClickListener(this);
        txtView_console = (TextView)findViewById(R.id.txtView_console);
        editTxt_command = (EditText) findViewById(R.id.editTxt_command);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public void log(String log) {
        txtView_console.setText(log.concat("\n").concat(txtView_console.getText().toString()));
    }

    public void sendMocData(String data) {
        if (MainActivity.mFlightController != null) {
            log(data);
            MainActivity.mFlightController.sendDataToOnboardSDKDevice(data.getBytes(), new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    //log("Result");
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "sendMocData error - No aircraft connected", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_send:
                final String command = editTxt_command.getText().toString();
                sendMocData(command);
                break;
            case R.id.btn_led:
                sendMocData("#1");
                break;
        }
    }
}
