package ch.hevs.matrice210;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import ch.hevs.matrice210.Interfaces.MocInteraction;
import dji.common.error.DJIError;

public class MocFragment extends Fragment implements Observer, View.OnClickListener, MocInteraction {
    // UI Elements
    private Button btn_send, btn_led;
    private TextView txtView_console;
    EditText editTxt_command;

    // Listener
    MocInteractionListener mocIListener;

    @Override
    public void dataReceived(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        for (byte b : bytes) {
            buffer.append((char) b);
        }
        log("Data received (" + buffer.length() + ") : " + buffer.toString(), "MOC");
    }

    @Override
    public void onResult(DJIError djiError) {
        if (djiError != null)
            log("Error " + djiError.toString(), "MOC");
        else
            log("Data sent successfully", "MOC");
    }

    public interface MocInteractionListener {
        void sendData(final String data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.moc_layout, container, false);
        btn_send = (Button) view.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        btn_led = (Button) view.findViewById(R.id.btn_led);
        btn_led.setOnClickListener(this);
        txtView_console = (TextView) view.findViewById(R.id.txtView_console);
        editTxt_command = (EditText) view.findViewById(R.id.editTxt_command);
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
    public void log(final String log) {
        log(log, "LOG");
    }

    public void log(final String log, final String prefix) {
        // runOnUiThread used to avoid errors
        // "Only the original thread that created a view hierarchy can touch its views"
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DateFormat df = new SimpleDateFormat("[HH:mm:ss]");
                String time = df.format(Calendar.getInstance().getTime());
                String line = time + " - " + prefix + " - " + log;
                txtView_console.setText(line.concat("\n").concat(txtView_console.getText().toString()));
            }
        });
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
