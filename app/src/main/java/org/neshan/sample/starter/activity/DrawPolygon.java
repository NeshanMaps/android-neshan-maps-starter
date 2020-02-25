package org.neshan.sample.starter.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import org.neshan.core.LngLat;
import org.neshan.core.LngLatVector;
import org.neshan.core.Range;
import org.neshan.geometry.PolygonGeom;
import org.neshan.graphics.ARGB;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.LineStyle;
import org.neshan.styles.LineStyleCreator;
import org.neshan.styles.PolygonStyle;
import org.neshan.styles.PolygonStyleCreator;
import org.neshan.ui.MapView;
import org.neshan.vectorelements.Polygon;


public class DrawPolygon extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer. We add polygons to this layer.
    VectorElementLayer polygonLayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_draw_polygon);
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
        polygonLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(polygonLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }


    // Drawing polygon on map
    public PolygonGeom drawPolygonGeom(View view){
        // we clear every polygon that is currently on map
        polygonLayer.clear();
        // Adding some LngLat points to a LngLatVector
        LngLatVector lngLatVector = new LngLatVector();
        lngLatVector.add(new LngLat(51.325525, 35.762294));
        lngLatVector.add(new LngLat(51.323768, 35.756548));
        lngLatVector.add(new LngLat(51.328617, 35.755394));
        lngLatVector.add(new LngLat(51.330666, 35.760905));
        // Creating a polygonGeom from LngLatVector
        PolygonGeom polygonGeom = new PolygonGeom(lngLatVector);
        // Creating a polygon from polygonGeom. here we use getPolygonGeom() method to define polygon styles
        Polygon polygon = new Polygon(polygonGeom, getPolygonStyle());
        // adding the created polygon to polygonLayer, showing it on map
        polygonLayer.add(polygon);
        // focusing camera on first point of drawn polygon
        map.setFocalPointPosition(new LngLat(51.325525, 35.762294),0.25f );
        map.setZoom(14,0);
        return polygonGeom;
    }

    // In this method we create a PolygonStyleCreator and set its features.
    // One feature is its lineStyle, getLineStyle() method is used to get polygon's line style
    // By calling buildStyle() method on polygonStrCr, an object of type PolygonStyle is returned
    private PolygonStyle getPolygonStyle(){
        PolygonStyleCreator polygonStCr = new PolygonStyleCreator();
        polygonStCr.setLineStyle(getLineStyle());
        return polygonStCr.buildStyle();
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
