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

import java.util.Observable;
import java.util.Observer;

public class MocFragment extends Fragment implements Observer {
    private Button btn_send, btn_led;
    private TextView txtView_console;
    EditText editTxt_command;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState);

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
}
