package org.neshan.sample.starter.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.ui.MapView;

public class TrafficLayer extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;
    final int TRAFFIC_INDEX = 1;

    // map UI element
    MapView map;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_traffic_layer);

        // everything related to ui is initialized here
        initLayoutReferences();
    }


    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);
    }


    // Initializing map
    private void initMap(){
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);

        // adding traffic layer to TRAFFIC_INDEX
        map.getLayers().insert(TRAFFIC_INDEX, NeshanServices.createTrafficLayer());
    }

    public void toggleTrafficLayer(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked())
            map.getLayers().insert(TRAFFIC_INDEX, NeshanServices.createTrafficLayer());
        else
            map.getLayers().remove(map.getLayers().get(TRAFFIC_INDEX));
    }
}
