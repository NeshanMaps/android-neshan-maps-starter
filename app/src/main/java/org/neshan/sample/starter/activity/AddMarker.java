package org.neshan.sample.starter.activity;

import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.core.Variant;
import org.neshan.layers.Layer;
import org.neshan.layers.VectorElementEventListener;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.AnimationStyle;
import org.neshan.styles.AnimationStyleBuilder;
import org.neshan.styles.AnimationType;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.ClickData;
import org.neshan.ui.ClickType;
import org.neshan.ui.ElementClickData;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Marker;

public class AddMarker extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;
    // You can add some elements to a VectorElementLayer
    VectorElementLayer markerLayer;
    // Marker that will be added on map
    Marker marker;
    // an id for each marker
    long markerId = 0;
    // marker animation style
    AnimationStyle animSt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_add_marker);
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
        map.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapClicked(ClickData mapClickInfo) {
                super.onMapClicked(mapClickInfo);
                if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
                    // by calling getClickPos(), we can get position of clicking (or tapping)
                    LngLat clickedLocation = mapClickInfo.getClickPos();
                    // addMarker adds a marker (pretty self explanatory :D) to the clicked location
                    addMarker(clickedLocation, markerId);
                    // increment id
                    markerId++;
                }
            }
        });
    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
    }


    // Initializing map
    private void initMap() {
        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        markerLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(markerLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        Layer baseMap = NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY, getCacheDir() + "/baseMap", 10);
        map.getLayers().insert(BASE_MAP_INDEX, baseMap);

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234), 0);
        map.setZoom(14, 0);
    }


    // This method gets a LngLat as input and adds a marker on that position
    private void addMarker(LngLat loc, long id) {
        // If you want to have only one marker on map at a time, uncomment next line to delete all markers before adding a new marker
//        markerLayer.clear();

        // Creating animation for marker. We should use an object of type AnimationStyleBuilder, set
        // all animation features on it and then call buildStyle() method that returns an object of type
        // AnimationStyle
        AnimationStyleBuilder animStBl = new AnimationStyleBuilder();
        animStBl.setFadeAnimationType(AnimationType.ANIMATION_TYPE_SMOOTHSTEP);
        animStBl.setSizeAnimationType(AnimationType.ANIMATION_TYPE_SPRING);
        animStBl.setPhaseInDuration(0.5f);
        animStBl.setPhaseOutDuration(0.5f);
        animSt = animStBl.buildStyle();

        // Creating marker style. We should use an object of type MarkerStyleCreator, set all features on it
        // and then call buildStyle method on it. This method returns an object of type MarkerStyle
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(30f);
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker)));
        // AnimationStyle object - that was created before - is used here
        markStCr.setAnimationStyle(animSt);
        MarkerStyle markSt = markStCr.buildStyle();

        // Creating marker
        marker = new Marker(loc, markSt);
        // Setting a metadata on marker, here we have an id for each marker
        marker.setMetaDataElement("id", new Variant(id));

        // Adding marker to markerLayer, or showing marker on map!
        markerLayer.add(marker);

        //handling events on markerLayer
        markerLayer.setVectorElementEventListener(new VectorElementEventListener() {
                                                      @Override
                                                      public boolean onVectorElementClicked(ElementClickData clickInfo) {
                                                          // If a double click happens on a marker...
                                                          if (clickInfo.getClickType() == ClickType.CLICK_TYPE_DOUBLE) {
                                                              final long removeId = clickInfo.getVectorElement().getMetaDataElement("id").getLong();
                                                              runOnUiThread(new Runnable() {
                                                                  @Override
                                                                  public void run() {
                                                                      Toast.makeText(AddMarker.this, "نشانگر شماره " + removeId + " حذف شد!", Toast.LENGTH_SHORT).show();
                                                                  }
                                                              });
                                                              //getting marker reference from clickInfo and remove that marker from markerLayer
                                                              markerLayer.remove(clickInfo.getVectorElement());

                                                              // If a single click happens...
                                                          } else if (clickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE) {
                                                              // changing marker to blue
                                                              changeMarkerToBlue((Marker)clickInfo.getVectorElement());
                                                          }
                                                          return true;
                                                      }

                                                  }
        );
    }

    private void changeMarkerToBlue(Marker redMarker){
        // create new marker style
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(30f);
        // Setting a new bitmap as marker
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker_blue)));
        markStCr.setAnimationStyle(animSt);
        MarkerStyle blueMarkSt = markStCr.buildStyle();

        // changing marker style using setStyle
        redMarker.setStyle(blueMarkSt);
    }
}
