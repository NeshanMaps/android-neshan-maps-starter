package org.neshan.sample.starter.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.neshan.core.LngLat;
import org.neshan.core.LngLatVector;
import org.neshan.core.Range;
import org.neshan.geometry.LineGeom;
import org.neshan.graphics.ARGB;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.LineStyle;
import org.neshan.styles.LineStyleCreator;
import org.neshan.ui.MapView;
import org.neshan.vectorelements.Line;


public class DrawLine extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer. We add lines to this layer.
    VectorElementLayer lineLayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_draw_line);
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
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);
    }

    // Initializing map
    private void initMap(){
        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        lineLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(lineLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }

    // Drawing line on map
    public LineGeom drawLineGeom(View view){
        // we clear every line that is currently on map
        lineLayer.clear();
        // Adding some LngLat points to a LngLatVector
        LngLatVector lngLatVector = new LngLatVector();
        lngLatVector.add(new LngLat(51.327650, 35.769368));
        lngLatVector.add(new LngLat(51.323889, 35.756670));
        // Creating a lineGeom from LngLatVector
        LineGeom lineGeom = new LineGeom(lngLatVector);
        // Creating a line from LineGeom. here we use getLineStyle() method to define line styles
        Line line = new Line(lineGeom, getLineStyle());
        // adding the created line to lineLayer, showing it on map
        lineLayer.add(line);
        // focusing camera on first point of drawn line
        map.setFocalPointPosition(new LngLat(51.327650, 35.769368),0.25f );
        map.setZoom(14,0);
        return lineGeom;
    }

    // In this method we create a LineStyleCreator, set its features and call buildStyle() method
    // on it and return the LineStyle object (the same routine as crating a marker style)
    private LineStyle getLineStyle(){
        LineStyleCreator lineStCr = new LineStyleCreator();
        lineStCr.setColor(new ARGB((short) 2, (short) 119, (short) 189, (short)190));
        lineStCr.setWidth(12f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }
}
