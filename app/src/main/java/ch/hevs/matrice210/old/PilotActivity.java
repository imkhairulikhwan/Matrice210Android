package ch.hevs.matrice210.old;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.RelativeLayout;

import com.dji.mapkit.maps.DJIMap;
import com.dji.mapkit.models.DJILatLng;

import ch.hevs.matrice210.R;
import ch.hevs.matrice210.tools.DensityUtil;
import dji.ux.widget.MapWidget;

/** Activity that shows all the UI elements together */
public class PilotActivity extends Activity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pilot_layout);
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

        //mapWidget.onResume();
    }

    @Override
    protected void onPause() {
        //mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        //mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //mapWidget.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        //mapWidget.onLowMemory();
        super.onLowMemory();
    }
}

