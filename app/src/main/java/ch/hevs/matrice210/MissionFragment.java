package ch.hevs.matrice210;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import ch.hevs.matrice210.Interfaces.MocInteraction;
import ch.hevs.matrice210.Interfaces.MocInteractionListener;
import dji.common.error.DJIError;
import dji.common.mission.tapfly.Vector;


public class MissionFragment extends Fragment implements Observer, View.OnClickListener, MocInteraction {

    // UI Elements
    private TextView txtView_console;
    private EditText editTxt_x, editTxt_y, editTxt_z, editTxt_yaw;

    // Listener
    private MocInteractionListener mocIListener;

    @Override
    public void dataReceived(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append((char) b);
        }
        log("Data received (" + bytes.length + ") : " + buffer.toString(), "MOC");
    }

    @Override
    public void onResult(DJIError djiError) {
        if (djiError != null)
            log("Error " + djiError.toString(), "MOC");
        else
            log("Data sent successfully", "MOC");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.mission_layout, container, false);
        view.findViewById(R.id.btn_stopMission).setOnClickListener(this);
        view.findViewById(R.id.btn_position).setOnClickListener(this);
        view.findViewById(R.id.btn_velocity).setOnClickListener(this);
        view.findViewById(R.id.btn_releaseEmergency).setOnClickListener(this);
        view.findViewById(R.id.btn_takeOff).setOnClickListener(this);
        view.findViewById(R.id.btn_landing).setOnClickListener(this);
        txtView_console = (TextView) view.findViewById(R.id.txtView_console);
        txtView_console.setMovementMethod(new ScrollingMovementMethod());
        editTxt_x = (EditText) view.findViewById(R.id.editTxt_x);
        editTxt_y = (EditText) view.findViewById(R.id.editTxt_y);
        editTxt_z = (EditText) view.findViewById(R.id.editTxt_z);
        editTxt_yaw = (EditText) view.findViewById(R.id.editTxt_yaw);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        try {
            mocIListener = (MocInteractionListener) activity;
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new ClassCastException(activity.toString()
                    + " must implement MocInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void update(Observable observable, Object o) {
        // Try catch cause the fragment is not necessary visible
        try {

        } catch (Exception e) {
        }
    }

    public void sendMocData(final String data) {
        log("Send data (" + data.length() + ") : " + data, "MOC");
        mocIListener.sendData(data);
    }

    public void sendMocData(final byte[] data) {
        log("Send data (" + data.length + ") : " + data, "MOC");
        mocIListener.sendData(data);
    }

    public void log(final String log) {
        log(log, "LOG");
    }

    public void log(final String log, final String prefix) {
        log(log, prefix, false);
    }

    public void log(final String log, final String prefix, final boolean clear) {
        // runOnUiThread used to avoid errors
        // "Only the original thread that created a view hierarchy can touch its views"
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                DateFormat df = new SimpleDateFormat("[HH:mm:ss:SSS]");
                String time = df.format(Calendar.getInstance().getTime());
                String line = time + " - " + prefix + " - " + log;
                if(clear) {
                    txtView_console.setText(line);
                } else {
                    txtView_console.setText(line.concat("\n").concat(txtView_console.getText().toString()));
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_stopMission:
                sendMocData(getString(R.string.moc_command_stopMission));
                break;
            case R.id.btn_position: {
                Vector v = readVectorFromEditTexts(editTxt_x, editTxt_y, editTxt_z);
                float yaw = readFloatFromEditText(editTxt_yaw);
                ByteBuffer buffer = ByteBuffer.allocate(32*5+2);
                buffer.putChar('m');
                buffer.putInt(1);
                buffer.putFloat(v.getX());
                buffer.putFloat(v.getY());
                buffer.putFloat(v.getZ());
                buffer.putFloat(yaw);
                sendMocData(buffer.array());
            }
                break;
            case R.id.btn_velocity:

                break;
            case R.id.btn_releaseEmergency:
                sendMocData(getString(R.string.moc_command_emergencyRelease));
                break;
            case R.id.btn_takeOff:
                sendMocData(getString(R.string.moc_command_takeoff));
                break;
            case R.id.btn_landing:
                sendMocData(getString(R.string.moc_command_landing));
                break;
        }
    }

    private float readFloatFromEditText(EditText editText) {
        return Float.valueOf(editText.getText().toString());
    }

    private Vector readVectorFromEditTexts(EditText x, EditText y, EditText z) {
        return new Vector(readFloatFromEditText(x),
                readFloatFromEditText(y),
                readFloatFromEditText(z));
    }

    private static byte [] float2ByteArray (float value)
    {
        return ByteBuffer.allocate(4).putFloat(value).array();
    }
}
