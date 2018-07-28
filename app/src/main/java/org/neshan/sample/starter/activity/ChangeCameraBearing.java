package org.neshan.sample.starter.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.custom_view.CircularSeekBar;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;


public class ChangeCameraBearing extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;
    // camera bearing control
    CircularSeekBar bearingSeekBar;

    // variable that hold camera bearing
    float cameraBearing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_change_camera_bearing);

        // everything related to ui is initialized here
        initLayoutReferences();
    }


    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();
        // connect bearing seek bar to camera
        bearingSeekBar.setOnSeekBarChangeListener(new CircularSeekBar.OnCircularSeekBarChangeListener() {
            @Override
            public void onProgressChanged(CircularSeekBar circularSeekBar, float progress, boolean fromUser) {
                // change camera bearing programmatically
                map.setBearing(progress, 0f);
            }

            @Override
            public void onStopTrackingTouch(CircularSeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(CircularSeekBar seekBar) {

            }
        });

        map.setMapEventListener(new MapEventListener() {

            // detect user input ( zoom, change tilt, change bearing, etc )
            @Override
            public void onMapMoved() {
                super.onMapMoved();
                // updating seek bar with new camera bearing value
                if (map.getBearing() < 0) {
                    cameraBearing = (180 + map.getBearing()) + 180;
                } else {
                    cameraBearing = map.getBearing();
                }
                // updating own ui element must run on ui thread not in map ui thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bearingSeekBar.setProgress(cameraBearing);
                    }
                });
            }
        });
    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
        bearingSeekBar = findViewById(R.id.bearing_seek_bar);
    }

    // Initializing map
    private void initMap() {
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234), 0);
        map.setZoom(14, 0);
    }

}
