package org.neshan.sample.starter.activity;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import org.json.JSONObject;
import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.model.NeshanAddress;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.AnimationStyle;
import org.neshan.styles.AnimationStyleBuilder;
import org.neshan.styles.AnimationType;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.ClickData;
import org.neshan.ui.ClickType;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Marker;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class APIOkHttp extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer
    VectorElementLayer markerLayer;

    // a bottomsheet to show address on
    private View reverseBottomSheetView;
    private BottomSheetBehavior reverseBottomSheetBehavior;

    //ui elements in bottom sheet
    private TextView addressTitle;
    private TextView addressDetails;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_api_volley);
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
                    addMarker(clickedLocation);

                    //calling NeshanReverseAPI to get address of a location and showing it on a bottom sheet
                    neshanReverseAPI(clickedLocation);
                }
            }
        });
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);

        // UI elements in bottom sheet
        addressTitle = findViewById(R.id.title);
        addressDetails = findViewById(R.id.details);

        reverseBottomSheetView = findViewById(R.id.reverse_bottom_sheet_include);
        reverseBottomSheetBehavior = BottomSheetBehavior.from(reverseBottomSheetView);
        // bottomsheet is collapsed at first
        reverseBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    // Initializing map
    private void initMap(){
        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        markerLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(markerLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }


    // This method gets a LngLat as input and adds a marker on that position
    private void addMarker(LngLat loc){
        // First, we should clear every marker that is currently located on map
        markerLayer.clear();

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


    private void neshanReverseAPI(LngLat loc) {
        String requestURL = "https://api.neshan.org/v1/reverse?lat=" + loc.getY() + "&lng=" + loc.getX();
        final String latLngAddr = String.format("%.6f", loc.getY()) + "," + String.format("%.6f", loc.getX());

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                //TODO: replace "YOUR_API_KEY" with your api key
                .header("Api-Key", "YOUR_API_KEY")
                .url(requestURL)
                .build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {

                    String neighbourhood = "آدرس نامشخص";
                    String address = latLngAddr;

                    try {
                        String jsonData = response.body().string();
                        JSONObject obj = new JSONObject(jsonData);

                        neighbourhood = obj.getString("neighbourhood");
                        address = obj.getString("address");


                        // if server was able to return neighbourhood and address to us
                        if(neighbourhood.equals("null") && address.equals("null")) {
                            neighbourhood = "آدرس نامشخص";
                            address = latLngAddr;
                        }

                    }
                    catch (Exception e){
                        Log.d("nehsnaReverse", Log.getStackTraceString(e));
                        neighbourhood = "آدرس نامشخص";
                        address = latLngAddr;
                    }
                    finally {

                        final String fNeighbourhood = neighbourhood;
                        final String fAddrees = address;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addressTitle.setText(fNeighbourhood);
                                addressDetails.setText(fAddrees);
                            }
                        });
                    }
                }
            }
        });

    }
}
