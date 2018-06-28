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
    private Button btn_send, btn_reset_counter, btn_ack_test, btn_send_test;
    private TextView txtView_console, txtView_rate, txtView_counter;
    private int receivedFrames;
    private EditText editTxt_command, editTxt_length, editTxt_delay;

    // Listener
    private MocInteractionListener mocIListener;

    private long lastTime;

    enum debugMode {
        loopFrame,
        receiveFrames
    }
    private debugMode mode;

    @Override
    public void dataReceived(byte[] bytes) {

        if(mode == debugMode.loopFrame) {
            if (bytes.length == 100) {
                boolean sendData = true;
                byte index = bytes[0];
                log("Data received (" + bytes.length + ") : " + index, "MOC");
                if (index == 0) {
                    log("Start of test");
                    lastTime = System.currentTimeMillis();
                } else if (index == 100) {
                    sendData = false;
                    long diffTime = System.currentTimeMillis() - lastTime;
                    log("End of test");
                    setRate(diffTime);
                    mode = debugMode.receiveFrames;
                }

                if (sendData) {
                    index++;
                    byte[] b = new byte[2];
                    b[0] = '#';
                    b[1] = index;
                    sendMocData(b);
                }
            }
        } else if (mode == debugMode.receiveFrames) {
            receivedFrames++;
            if (receivedFrames % 10 == 0) {
                long currentTime = System.currentTimeMillis();
                long diffTime = currentTime - lastTime;
                // 10 frames received in diffTime ms
                //long rate = 10 * 1000 / diffTime;
                setRate(diffTime);
                lastTime = System.currentTimeMillis();
            }
            setCounter(receivedFrames);
        }
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
        void sendData(final byte[] data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        receivedFrames = 0;
        mode = debugMode.receiveFrames;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.moc_layout, container, false);
        btn_send = (Button) view.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        btn_send_test = (Button) view.findViewById(R.id.btn_send_test);
        btn_send_test.setOnClickListener(this);
        btn_ack_test = (Button) view.findViewById(R.id.btn_ack_test);
        btn_ack_test.setOnClickListener(this);
        btn_reset_counter = (Button) view.findViewById(R.id.btn_reset_coutner);
        btn_reset_counter.setOnClickListener(this);
        txtView_console = (TextView) view.findViewById(R.id.txtView_console);
        txtView_counter = (TextView) view.findViewById(R.id.txtView_counter);
        txtView_rate = (TextView) view.findViewById(R.id.txtView_rate);
        editTxt_command = (EditText) view.findViewById(R.id.editTxt_command);
        editTxt_length = (EditText) view.findViewById(R.id.editTxt_length);
        editTxt_delay = (EditText) view.findViewById(R.id.editTxt_delay);
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

    public void sendMocData(final byte[] data) {
        log("Send data (" + data.length + ") : " + data, "MOC");
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
    public void setCounter(final int counter) {
        // runOnUiThread used to avoid errors
        // "Only the original thread that created a view hierarchy can touch its views"
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtView_counter.setText("Counter : " + counter);
            }
        });
    }

    public void setRate(final long rate) {
        // runOnUiThread used to avoid errors
        // "Only the original thread that created a view hierarchy can touch its views"
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtView_rate.setText("Rate : " + rate);
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
            case R.id.btn_reset_coutner:
                mode = debugMode.receiveFrames;
                log("Received frames counter reset");
                receivedFrames = 0;
                setCounter(0);
                break;
            case R.id.btn_ack_test:
                mode = debugMode.loopFrame;
                byte[] b = new byte[2];
                b[0] = '#';
                b[1] = 0;
                sendMocData(b);
                break;
            case R.id.btn_send_test:
                int delay = Integer.parseInt(editTxt_delay.getText().toString());
                int length = Integer.parseInt(editTxt_length.getText().toString());
                if(length > 100)
                    length = 100;

                log("Up-test running : length = " + length + "bytes, delay = " + delay + " ms");

                byte[] frame = new byte[length];
                int framesToSend = 300;
                byte n = 0;
                while (true) {
                    frame[(int)n] = n;
                    n++;
                    if (n == length) {
                        break;
                    }
                }
                try {
                    while(framesToSend != 0) {
                        sendMocData(frame);
                        log("Frame " + (300-framesToSend) + " sent");
                        framesToSend--;
                        Thread.sleep(delay);
                    }
                    log("Up-test ended : 300 frames sent");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                break;
        }
    }
}
