package org.neshan.sample.starter.activity;

import android.os.Bundle;
import androidx.core.content.res.ResourcesCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ToggleButton;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.ui.MapView;

public class POILayer extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;
    final int POI_INDEX = 1;

    // map UI element
    MapView map;
    // save current map style
    NeshanMapStyle mapStyle;
    // map style control
    ImageView themePreview;
    boolean isPOIEnable = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_poi_layer);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // everything related to ui is initialized here
        initLayoutReferences();
    }

    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();
        // Initializing theme preview
        validateThemePreview();
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);
        themePreview = findViewById(R.id.theme_preview);
    }

    private void validateThemePreview() {
        switch (mapStyle) {
            case STANDARD_DAY:
                themePreview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.map_style_standard_night, getTheme()));
                break;
            case STANDARD_NIGHT:
                themePreview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.map_style_neshan, getTheme()));
                break;
            case NESHAN:
                themePreview.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.map_style_standard_day, getTheme()));
                break;
        }
    }


    // Initializing map
    private void initMap(){
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        mapStyle = NeshanMapStyle.STANDARD_DAY;
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);

        // adding POI layer to POI_INDEX
        map.getLayers().insert(POI_INDEX, NeshanServices.createPOILayer(mapStyle == NeshanMapStyle.STANDARD_NIGHT));
    }

    public void togglePOILayer(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        isPOIEnable = !isPOIEnable;
        if (toggleButton.isChecked())
            map.getLayers().insert(POI_INDEX, NeshanServices.createPOILayer(mapStyle == NeshanMapStyle.STANDARD_NIGHT));
        else
            map.getLayers().remove(map.getLayers().get(POI_INDEX));
    }

    public void changeStyle(View view) {
        NeshanMapStyle previousMapStyle = mapStyle;
        switch (previousMapStyle) {
            case STANDARD_DAY:
                mapStyle = NeshanMapStyle.STANDARD_NIGHT;
                break;
            case STANDARD_NIGHT:
                mapStyle = NeshanMapStyle.NESHAN;
                break;
            case NESHAN:
                mapStyle = NeshanMapStyle.STANDARD_DAY;
                break;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                validateThemePreview();
            }
        });
        map.getLayers().remove(map.getLayers().get(BASE_MAP_INDEX));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(mapStyle));
        if (isPOIEnable) {
            map.getLayers().remove(map.getLayers().get(POI_INDEX));
            map.getLayers().insert(POI_INDEX, NeshanServices.createPOILayer(mapStyle == NeshanMapStyle.STANDARD_NIGHT));
        }
    }
}
