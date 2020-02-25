package org.neshan.sample.starter.activity;

import android.graphics.BitmapFactory;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.neshan.core.LngLat;
import org.neshan.core.LngLatVector;
import org.neshan.core.Range;
import org.neshan.core.Variant;
import org.neshan.geometry.LineGeom;
import org.neshan.graphics.ARGB;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.network.PubKeyManager;
import org.neshan.sample.starter.task.PolylineEncoding;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.AnimationStyle;
import org.neshan.styles.AnimationStyleBuilder;
import org.neshan.styles.AnimationType;
import org.neshan.styles.LineStyle;
import org.neshan.styles.LineStyleCreator;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.ClickData;
import org.neshan.ui.ClickType;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Line;
import org.neshan.vectorelements.Marker;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

public class Routing extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    // map UI element
    MapView map;

    // define two toggle button and connecting together for two type of routing
    ToggleButton overviewToggleButton;
    ToggleButton stepByStepToggleButton;

    // we save decoded Response of routing encoded string because we don't want request every time we clicked toggle buttons
    List<PolylineEncoding.LatLng> decodedOverviewPath;
    List<PolylineEncoding.LatLng> decodedStepByStepPath;

    // value for difference mapSetZoom
    boolean overview = false;

    // You can add some elements to a VectorElementLayer. We add lines and markers to this layer.
    VectorElementLayer lineLayer;
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
        setContentView(R.layout.activity_routing);
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
                // we not allow user import more than 2 marker for routing
                if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG && markerId < 2) {
                    // by calling getClickPos(), we can get position of clicking (or tapping)
                    LngLat clickedLocation = mapClickInfo.getClickPos();
                    // addMarker adds a marker (pretty self explanatory :D) to the clicked location
                    addMarker(clickedLocation, markerId);
                    // increment id
                    markerId++;

                    // check until second marker is insert draw an overview line between that 2 marker
                    if (markerId == 2) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                overviewToggleButton.setChecked(true);
                                neshanRoutingApi();
                            }
                        });
                    }
                } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG && markerId >= 2) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(Routing.this, "مسیریابی بین دو نقطه انجام میشود!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    // We use findViewByID for every element in our layout file here
    private void initViews(){
        map = findViewById(R.id.map);


        // CheckChangeListener for Toggle buttons
        CompoundButton.OnCheckedChangeListener changeChecker = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                // if any toggle button checked:
                if (isChecked) {
                    // if overview toggle button checked other toggle button is uncheck
                    if (toggleButton == overviewToggleButton) {
                        stepByStepToggleButton.setChecked(false);
                        overview = true;
                    }
                    if (toggleButton == stepByStepToggleButton) {
                        overviewToggleButton.setChecked(false);
                        overview = false;
                    }
                }
                if (!isChecked) {
                    lineLayer.clear();
                }
            }
        };

        // each toggle button has a checkChangeListener for uncheck other toggle button
        overviewToggleButton = findViewById(R.id.overviewToggleButton);
        overviewToggleButton.setOnCheckedChangeListener(changeChecker);

        stepByStepToggleButton = findViewById(R.id.stepByStepToggleButton);
        stepByStepToggleButton.setOnCheckedChangeListener(changeChecker);
    }

    // Initializing map
    private void initMap(){
        // Creating a VectorElementLayer(called lineLayer) to add line to it and adding it to map's layers
        lineLayer = NeshanServices.createVectorElementLayer();
        markerLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(lineLayer);
        map.getLayers().add(markerLayer);

        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(new LngLat(51.330743, 35.767234),0 );
        map.setZoom(14,0);
    }

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
    }

    // request routing method from Neshan Server
    private void neshanRoutingApi() {
        LngLat firstMarkerpos = markerLayer.getAll().get(0).getGeometry().getCenterPos();
        LngLat secondMarkerpos = markerLayer.getAll().get(1).getGeometry().getCenterPos();
        String requestURL = "https://api.neshan.org/v2/direction?origin=" + firstMarkerpos.getY() + "," + firstMarkerpos.getX() + "&destination=" + secondMarkerpos.getY() + "," + secondMarkerpos.getX();

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
                            lineLayer.clear();
                            JSONObject obj = new JSONObject(response);
                            String encodedOverviewPath = obj.getJSONArray("routes").getJSONObject(0).getJSONObject("overview_polyline").getString("points");
                            JSONArray stepByStepPath = obj.getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps");

                            // two type of routing
                            decodedOverviewPath = PolylineEncoding.decode(encodedOverviewPath);
                            decodedStepByStepPath = new ArrayList<>();

                            // decoding each segment of steps and putting to an array
                            for (int i = 0; i < stepByStepPath.length(); i++) {
                                List<PolylineEncoding.LatLng> decodedEachStep = PolylineEncoding.decode(stepByStepPath.getJSONObject(i).getString("polyline"));
                                decodedStepByStepPath.addAll(decodedEachStep);
                            }

                            drawLineGeom(decodedOverviewPath);
//                          Log.e("response", String.valueOf(decodedStepByStepPath));

                        } catch (Exception e) {

                            Log.e("error", e.getMessage());
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
                params.put("Api-Key", "service.VNlPhrWb3wYRzEYmstQh3GrAXyhyaN55AqUSRR3V");
                return params;
            }
        };

        // Add the request to the queue
        requestQueue.add(reverseGeoSearchRequest);
    }

    // Drawing line on map
    public LineGeom drawLineGeom(List<PolylineEncoding.LatLng> paths) {
        // we clear every line that is currently on map
        lineLayer.clear();
        // Adding some LngLat points to a LngLatVector
        LngLatVector lngLatVector = new LngLatVector();
        for (PolylineEncoding.LatLng path : paths) {
            lngLatVector.add(new LngLat(path.lng, path.lat));
        }

        // Creating a lineGeom from LngLatVector
        LineGeom lineGeom = new LineGeom(lngLatVector);
        // Creating a line from LineGeom. here we use getLineStyle() method to define line styles
        Line line = new Line(lineGeom, getLineStyle());
        // adding the created line to lineLayer, showing it on map
        lineLayer.add(line);
        // focusing camera on first point of drawn line
        mapSetPosition(overview);
        return lineGeom;
    }

    // for overview routing we zoom out and review hole route and for stepByStep routing we just zoom to first marker position
    private void mapSetPosition(boolean overview) {
        double centerFirstMarkerX = markerLayer.getAll().get(0).getGeometry().getCenterPos().getX();
        double centerFirstMarkerY = markerLayer.getAll().get(0).getGeometry().getCenterPos().getY();
        if (overview) {
            double centerFocalPositionX = (centerFirstMarkerX + markerLayer.getAll().get(1).getGeometry().getCenterPos().getX()) / 2;
            double centerFocalPositionY = (centerFirstMarkerY + markerLayer.getAll().get(1).getGeometry().getCenterPos().getY()) / 2;
            map.setFocalPointPosition(new LngLat(centerFocalPositionX, centerFocalPositionY),0.5f );
            map.setZoom(14,0.5f);
        } else {
            map.setFocalPointPosition(new LngLat(centerFirstMarkerX, centerFirstMarkerY),0.5f );
            map.setZoom(18,0.5f);
        }

    }

    // In this method we create a LineStyleCreator, set its features and call buildStyle() method
    // on it and return the LineStyle object (the same routine as crating a marker style)
    private LineStyle getLineStyle(){
        LineStyleCreator lineStCr = new LineStyleCreator();
        lineStCr.setColor(new ARGB((short) 2, (short) 119, (short) 189, (short)190));
        lineStCr.setWidth(10f);
        lineStCr.setStretchFactor(0f);
        return lineStCr.buildStyle();
    }

    // call this function with clicking on toggle buttons and draw routing line depend on type of routing requested
    public void findRoute(View view) {
        if (markerLayer.getAll().size() < 2) {
            Toast.makeText(this, "برای مسیریابی باید دو نقطه انتخاب شود", Toast.LENGTH_SHORT).show();
            overviewToggleButton.setChecked(false);
            stepByStepToggleButton.setChecked(false);
        } else if (overviewToggleButton.isChecked()) {
            drawLineGeom(decodedOverviewPath);
        } else if (stepByStepToggleButton.isChecked()) {
            drawLineGeom(decodedStepByStepPath);

        }
    }
}
