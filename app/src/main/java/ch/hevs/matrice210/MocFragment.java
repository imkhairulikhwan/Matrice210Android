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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Observable;
import java.util.Observer;

import ch.hevs.matrice210.Interfaces.MocInteraction;
import dji.common.error.DJIError;


public class MocFragment extends Fragment implements Observer, View.OnClickListener, MocInteraction {
    public static final char START_CHAR = '@';
    public static final char END_CHAR = '#';

    // UI Elements
    private TextView txtView_console, txtView_counter;
    private long receivedBytes, receivedFrames;
    private EditText editTxt_command, editTxt_length, editTxt_delay;

    // Listener
    private MocInteractionListener mocIListener;

    // Moc tests variables
    private long ackCounter;
    private long startTime;
    private byte[] testFrame;
    // 4 existing test modes
    // down     : launched by M210, aircraft send 300 frames of 1-100 bytes each x ms, x is chosen on launch
    // up       : launched on Android, ground station send 100 frames of 1-100 bytes x ms, x is chosen on launch
    // ack up   : launched on Android, ground station ask numbered frames and aircraft send 100 bytes as ack. 100 frames are asked
    // ack down :launched on M210, aircraft ask numbered frames and ground station send 100 bytes as ack. 100 frames are asked
    enum MocTestMode {
        none(0),
        down(1),
        up(2),
        ackUp(3),
        ackDown(4);

        private final int value;
        MocTestMode(int value) {
            this.value = value;
        }

        public byte getByteValue() {
            return (byte)value;
        }

        public static MocTestMode convert(byte value) {
            return MocTestMode.values()[value];
        }
    }
    private MocTestMode testMode;
    // Up test class
    class UpTestParam extends Thread {
        private final static int frameToSend = 100;
        private int delayMs, frameSent = 300;
        byte[] frame;

        UpTestParam(int length, int delayMs) {
            if(length > 100)
                length = 100;
            frame = new byte[length];
            frame[0] = '-'; // '-' to avoid START_CHAR and ENC_CHAR
            byte n = 1;
            while (true) {
                frame[(int)n] = n;
                n++;
                if (n == length) {
                    break;
                }
            }
            this.delayMs = delayMs;
        }

        public void run() {
            try {
                startTime = System.currentTimeMillis();
                frameSent = 0;
                while(frameSent < frameToSend) {
                    frame[1] = (byte)frameSent;
                    sendMocData(frame);
                    log("Frame " + frameSent + " sent");
                    frameSent++;
                    Thread.sleep(delayMs);
                }
                long diffTimeMs = System.currentTimeMillis() - startTime;
                // Remove last delay
                diffTimeMs -= delayMs;
                // End of test
                Thread.sleep(1000);
                sendModeEndRequest(MocTestMode.up);
                long bytesSent = frame.length * frameToSend;
                log("Up test ended (length = " + frame.length + " bytes, delay = " + delayMs + " ms) : " + frameToSend + " frames sent (" + bytesSent + " Bytes) in " + diffTimeMs + " ms", "MOC");
                double dataFlow = bytesSent * 1000 / diffTimeMs;
                log("Data flow = " + dataFlow + " Bytes/s", "MOC");
                testMode = MocTestMode.none;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    private UpTestParam upTestParam;

    @Override
    public void dataReceived(byte[] bytes) {
        switch (bytes[0]) {
            // New test started
            case START_CHAR:
                switch (MocTestMode.convert(bytes[1])) {
                    case down:
                        testMode = MocTestMode.down;
                        log("Down test launched", "MOC");
                        resetDataCounter();
                        startTime = System.currentTimeMillis();
                        break;
                    case up:
                        testMode = MocTestMode.up;
                        log("Up test running ...", "MOC");
                        upTestParam.run();
                        break;
                    case ackUp:
                        testMode = MocTestMode.ackUp;
                        log("Ack up test launched", "MOC");
                        resetDataCounter();
                        byte[] b = new byte[2];
                        b[0] = '-'; // '-' to avoid START_CHAR or END_CHAR
                        b[1] = 0;
                        startTime = System.currentTimeMillis();
                        sendMocData(b);
                        break;
                    case ackDown:
                        log("Ack down test launched");
                        testMode = MocTestMode.ackDown;
                        ackCounter = 0;
                        sendModeStartRequest(MocTestMode.ackDown);
                        break;
                    default:
                        testMode = MocTestMode.none;
                        log("Unknown test launched", "MOC");
                        break;
                }
                break;
            // Test ended
            case END_CHAR:
                switch (MocTestMode.convert(bytes[1])) {
                    case down:
                        long diffTime = System.currentTimeMillis() - startTime - 1500;   // 1000ms before send of first frame + 500ms before send of # end character
                        // last send delay is not subtracted !
                        log("Down test ended : " + receivedFrames + " frames received (" + receivedBytes + " Bytes) in " + diffTime + " ms", "MOC");
                        double dataFlow = receivedBytes * 1000 / diffTime;
                        log("Data flow = " + dataFlow + " Bytes/s", "MOC");
                        testMode = MocTestMode.none;
                        break;
                    case up:
                        log("Up test ended", "MOC");
                        testMode = MocTestMode.none;
                        break;
                    case ackUp:
                        // No end request are supposed to be received from aircraft
                        log("Ack up test ended ??", "MOC");
                        testMode = MocTestMode.none;
                        break;
                    case ackDown:
                        log("Ack down test ended : " + ackCounter + " frames sent", "MOC");
                        testMode = MocTestMode.none;
                        break;
                    case none:
                        log("Reset test status", "MOC", true);
                        resetDataCounter();
                        testMode = MocTestMode.none;
                        break;
                    default:
                        log("Unknown test ended", "MOC");
                        resetDataCounter();
                        testMode = MocTestMode.none;
                        break;

                }
                break;
             // Data received
            default:
                switch (testMode) {
                    case down:
                        // display byte as unsigned
                        log("Down test : Data received (" + bytes.length + ") : " + (bytes[1] & 0xFF) + "/" + receivedFrames, "MOC");
                        receivedBytes += bytes.length;
                        receivedFrames++;
                        setCounter(receivedFrames, receivedBytes);
                        break;
                    case up:
                        // No data are supposed to be received in this mode
                        break;
                    case ackUp:
                        byte index = bytes[1];
                        log("Ack up test: Data received (" + bytes.length + ") : " + index, "MOC");
                        receivedBytes += bytes.length;
                        receivedFrames++;
                        setCounter(receivedFrames, receivedBytes);

                        if (index == 99) {  // 100 frames received
                            sendModeEndRequest(MocTestMode.ackUp);
                            long diffTime = System.currentTimeMillis() - startTime;
                            log("Ack up test ended : " + receivedFrames + " frames received (" + receivedBytes + " Bytes) in " + diffTime + " ms", "MOC");
                            double dataFlow = receivedBytes * 1000 / diffTime;
                            log("Data flow = " + dataFlow + " Bytes/s", "MOC");
                            testMode = MocTestMode.none;
                        } else {
                            // Send next frame request
                            index++;
                            byte[] b = new byte[2];
                            b[0] = '-'; // random char to avoid START_CHAR or END_CHAR
                            b[1] = index;
                            log("Ack up test : Send request " + index, "MOC");
                            sendMocData(b);
                        }
                        break;
                    case ackDown:
                        // Verify that requested frame is correctly numbered
                        if(bytes[1] == ackCounter) {
                            testFrame[0] = '-'; // '-' to avoid START_CHAR or END_CHAR
                            testFrame[1] = bytes[1];
                            log("Ack down test : Send frame " + bytes[1], "MOC");
                            sendMocData(testFrame);
                            ackCounter++;
                        } else {
                            log("Ack down test : Frame counter error");
                            testMode = MocTestMode.none;
                        }
                        break;
                    case none:
                        // Data received displayed as string
                        StringBuilder buffer = new StringBuilder();
                        for (byte b : bytes) {
                             buffer.append((char) b);
                        }
                        log("Data received (" + bytes.length + ") : " + buffer.toString(), "MOC");
                        break;
                }
                break;
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
        testMode = MocTestMode.none;
        // Fill test frame with numbers
        testFrame = new byte[100];
        byte n = 0;
        while (n < testFrame.length) {
            testFrame[(int)n] = n;
            n++;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.moc_layout, container, false);
        Button btn_send, btn_reset_test_mode, btn_ack_test, btn_send_test;
        btn_send = (Button) view.findViewById(R.id.btn_send);
        btn_send.setOnClickListener(this);
        btn_send_test = (Button) view.findViewById(R.id.btn_up_test);
        btn_send_test.setOnClickListener(this);
        btn_ack_test = (Button) view.findViewById(R.id.btn_ackUp_test);
        btn_ack_test.setOnClickListener(this);
        btn_reset_test_mode = (Button) view.findViewById(R.id.btn_reset_test_mode);
        btn_reset_test_mode.setOnClickListener(this);
        txtView_console = (TextView) view.findViewById(R.id.txtView_console);
        txtView_console.setMovementMethod(new ScrollingMovementMethod());
        txtView_counter = (TextView) view.findViewById(R.id.txtView_counter);
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

    private void resetDataCounter() {
        receivedFrames = 0;
        receivedBytes = 0;
        setCounter(receivedFrames, receivedBytes);
    }

    public void sendModeStartRequest(MocTestMode testMode) {
        sendModeRequest(START_CHAR, testMode);

    }

    public void sendModeEndRequest(MocTestMode testMode) {
        sendModeRequest(END_CHAR, testMode);
    }

    public void sendModeRequest(char operation, MocTestMode testMode) {
        byte[] b = new byte[2];
        b[0] = (byte)operation;
        b[1] = testMode.getByteValue();
        sendMocData(b);
    }

    public void sendMocData(final String data) {
        log("Send data (" + data.length() + ") : " + data, "MOC");
        mocIListener.sendData(data);
    }

    public void sendMocData(final byte[] data) {
        log("Send data (" + data.length + ") : " + data, "MOC");
        mocIListener.sendData(data);
    }

    public void setCounter(final long frames, final long bytes) {
        // runOnUiThread used to avoid errors
        // "Only the original thread that created a view hierarchy can touch its views"
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtView_counter.setText("Data received : " + frames + " frames, " + bytes + "  bytes");
            }
        });
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
            case R.id.btn_send:
                final String command = editTxt_command.getText().toString();
                sendMocData(command);
                break;
            case R.id.btn_reset_test_mode:
                testMode = MocTestMode.none;
                sendModeEndRequest(MocTestMode.none);
                break;
            case R.id.btn_ackUp_test:
                log("Ack up test initialization", "MOC");
                sendModeStartRequest(MocTestMode.ackUp);
                break;
            case R.id.btn_up_test:
                // Get parameters
                int delay = Integer.parseInt(editTxt_delay.getText().toString());
                int length = Integer.parseInt(editTxt_length.getText().toString());
                // Max frame length
                if(length > 100)
                    length = 100;

                // Send initializing command to aircraft
                log("Up test initializing : length = " + length + " bytes, delay = " + delay + " ms ...", "MOC");
                upTestParam = new UpTestParam(length, delay);
                sendModeStartRequest(MocTestMode.up);
                break;
        }
    }
}
