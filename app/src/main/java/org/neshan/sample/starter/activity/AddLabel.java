package org.neshan.sample.starter.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.graphics.ARGB;
import org.neshan.layers.Layer;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.LabelStyle;
import org.neshan.styles.LabelStyleCreator;
import org.neshan.ui.ClickData;
import org.neshan.ui.ClickType;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.vectorelements.Label;

public class AddLabel extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer
    VectorElementLayer labelLayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_label);
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

        // when long clicked on map, a marker is added in clicked location
        // MapEventListener gets all events on map, including single tap, double tap, long press, etc
        // we should check event type by calling getClickType() on mapClickInfo (from ClickData class)
        map.setMapEventListener(new MapEventListener(){
            @Override
            public void onMapClicked(ClickData mapClickInfo){
                super.onMapClicked(mapClickInfo);
                if(mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
                    // by calling getClickPos(), we can get position of clicking (or tapping)
                    LngLat clickedLocation = mapClickInfo.getClickPos();
                    // addMarker adds a marker (pretty self explanatory :D) to the clicked location
                    addLabel(clickedLocation);
                }
            }
        });
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);
    }

    // Initializing map
    private void initMap(){
        // Creating a VectorElementLayer(called labelLayer) to add all markers to it and adding it to map's layers
        labelLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(labelLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        Layer baseMap = NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY,getCacheDir()+"/baseMap",10);
        map.getLayers().insert(BASE_MAP_INDEX, baseMap);

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }

    // This method gets a LngLat as input and adds a label on that position
    private void addLabel(LngLat loc){
        // First, we should clear every marker that is currently located on map
        labelLayer.clear();

        // Creating label style. We should use an object of type LabelStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type LabelStyle
        LabelStyleCreator labelStCr = new LabelStyleCreator();
        labelStCr.setFontSize(15f);
        labelStCr.setBackgroundColor(new ARGB((short)255, (short)150, (short)150, (short)255));
        LabelStyle labelSt = labelStCr.buildStyle();

        // Creating label
        Label label = new Label(loc, labelSt, "مکان انتخاب شده");

        // Adding marker to labelLayer, or showing label on map!
        labelLayer.add(label);
    }
}
