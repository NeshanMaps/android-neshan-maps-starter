package org.neshan.sample.starter.activity;

import android.graphics.BitmapFactory;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;
import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.model.address.NeshanAddress;
import org.neshan.sample.starter.network.PubKeyManager;
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class APIVolley extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // You can add some elements to a VectorElementLayer
    VectorElementLayer markerLayer;

    // Result of reverseGeocoding
    private NeshanAddress neshanAddress;

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
        markStCr.setSize(30f);
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

        TrustManager tm[] = {new PubKeyManager("30820122300d06092a864886f70d01010105000382010f003082010a0282010100b2d2b372f340619bdd691d443d5cc5c4fa458eb02709d232702b29bab76dd91a5fb13de61ba32100604c0071664feb928bafe4226204e605017d92dfbeaff27debf9c9d47709894a53d5717fac9a6c0f562697fc8ffaac1d633fa0c3781bf4d665940340bb603f6b821a460aa730eecb624acc165ab5e765b894938437702cbe582dd038c79c41603034258f675c63beb68b76cb844f916a800d222d5393eead1b1cff218b6a9b7abd71eada18f262b57fd378130bc1dd4ff1558c5d1c1823219b2a35a43cd4c0f178f5b85a00efc7c83dc6cfce8a2a24fba879bc401c276466f0f13fbb16ac70516badb03e1a01676a4a8199be2096f2a09e719de5c084999d0203010001")};
        SSLSocketFactory pinnedSSLSocketFactory = null;
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, tm, null);
            pinnedSSLSocketFactory = context.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        RequestQueue requestQueue = Volley.newRequestQueue(this, new HurlStack(null, pinnedSSLSocketFactory));

        StringRequest reverseGeoSearchRequest = new StringRequest(
                Request.Method.GET,
                requestURL,
                new com.android.volley.Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            String neighbourhood = obj.getString("neighbourhood");
                            String address = obj.getString("address");

                            // if server was able to return neighbourhood and address to us
                            if(!neighbourhood.equals("null") && !address.equals("null")) {
                                addressTitle.setText(neighbourhood);
                                addressDetails.setText(address);
                            }
                            else{
                                addressTitle.setText("آدرس نامشخص");
                                addressDetails.setText(latLngAddr);
                            }

                        } catch (Exception e) {

                            addressTitle.setText("آدرس نامشخص");
                            addressDetails.setText(latLngAddr);
                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<>();
                // TODO: replace "YOUR_API_KEY" with your api key
                params.put("Api-Key", "service.kREahwU7lND32ygT9ZgPFXbwjzzKukdObRZsnUAJ");
                return params;
            }
        };

        // Add the request to the queue
        requestQueue.add(reverseGeoSearchRequest);
    }
}
