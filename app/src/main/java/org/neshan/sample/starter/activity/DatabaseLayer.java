package org.neshan.sample.starter.activity;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ToggleButton;

import org.neshan.core.Bounds;
import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.core.ViewportBounds;
import org.neshan.core.ViewportPosition;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.database_helper.AssetDatabaseHelper;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.AnimationStyle;
import org.neshan.styles.AnimationStyleBuilder;
import org.neshan.styles.AnimationType;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Marker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseLayer extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer
    VectorElementLayer markerLayer;

    // our database points
    SQLiteDatabase pointsDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_database_layer);

        // everything related to ui is initialized here
        initLayoutReferences();
    }

    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();
        // copy database.sqlite file from asset folder to /data/data/... and read points and add marker on map
        getDBPoints();
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);
    }

    // Initializing map
    private void initMap(){
        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        markerLayer = NeshanServices.createVectorElementLayer();

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }


    // copy database.sqlite file from asset folder to /data/data/... and read points and add marker on map
    private void getDBPoints(){
        // we create an AssetDatabaseHelper object, create a new database in mobile storage
        // and copy database.sqlite file into the new created database
        // Then we open the database and return the SQLiteDatabase object
        AssetDatabaseHelper myDbHelper = new AssetDatabaseHelper(getApplicationContext());
        myDbHelper = new AssetDatabaseHelper(this);

        try {
            myDbHelper.createDataBase();
        }
        catch (IOException ioe) {
            throw new Error("Unable to create database");
        }

        try {
            pointsDB = myDbHelper.openDataBase();
        }
        catch(SQLException sqle){
            throw sqle;
        }


        // creating a cursor and query all rows of points table
        Cursor cursor = pointsDB.rawQuery("select * from points",null);

        //reading all points and adding a marker for each one
        if (cursor.moveToFirst()) {
            map.getLayers().add(markerLayer);
            // variable for creating bound
            // min = south-west
            // max = north-east
            double minLat = Double.MAX_VALUE;
            double minLng = Double.MAX_VALUE;
            double maxLat = Double.MIN_VALUE;
            double maxLng = Double.MIN_VALUE;
            while (!cursor.isAfterLast()) {
                double lng = cursor.getDouble(cursor.getColumnIndex("lng"));
                double lat = cursor.getDouble(cursor.getColumnIndex("lat"));
                LngLat lngLat = new LngLat(lng, lat);

                // validating min and max
                minLat = Math.min(lngLat.getY(), minLat);
                minLng = Math.min(lngLat.getX(), minLng);
                maxLat = Math.max(lngLat.getY(), maxLat);
                maxLng = Math.max(lngLat.getX(), maxLng);

                addMarker(lngLat);

                cursor.moveToNext();
            }
            map.moveToCameraBounds(
                    new Bounds(new LngLat(minLng, minLat), new LngLat(maxLng, maxLat)),
                    new ViewportBounds(
                            new ViewportPosition(0,0),
                            new ViewportPosition(map.getWidth(),map.getHeight())
                    ),
                    true, 0.25f);
        }
    }


    // This method gets a LngLat as input and adds a marker on that position
    private void addMarker(LngLat loc){
        // Creating animation for marker. We should use an object of type AnimationStyleBuilder, set
        // all animation features on it and then call buildStyle() method that returns an object of type
        // AnimationStyle
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(0.5f);
        animStBl.setPhaseOutDuration(0.5f);
        AnimationStyle animSt = animStBl.buildStyle();

        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(20f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker)));
        // AnimationStyle object - that was created before - is used here
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating marker
        Marker marker = new Marker(loc, markSt);

        // Adding marker to markerLayer, or showing marker on map!
        markerLayer.add(marker);
    }

    public void toggleDatabaseLayer(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked())
            getDBPoints();
        else
            map.getLayers().remove(markerLayer);
    }
}
