package org.neshan.madadi.starter.activity;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.neshan.core.Bounds;
import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.core.Variant;
import org.neshan.core.ViewportBounds;
import org.neshan.core.ViewportPosition;
import org.neshan.graphics.ARGB;
import org.neshan.layers.Layer;
import org.neshan.layers.VectorElementEventListener;
import org.neshan.layers.VectorElementLayer;
import org.neshan.madadi.starter.R;
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
import org.neshan.utils.Log;
import org.neshan.vectorelements.Element;
import org.neshan.vectorelements.ElementVector;
import org.neshan.vectorelements.Marker;

public class RemoveMarker extends AppCompatActivity {

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
    // markerId bottom sheet
    TextView marker_id;
    // remove marker button
    Button remove_marker;
    // bottom sheet layout and behavior
    private View remove_marker_bottom_sheet;
    private BottomSheetBehavior bottomSheetBehavior;
    // save selected Marker for select and deselect function
    Marker selectedMarker = null;
    // Tip Strings
    String firstTipString = "<b>" + "قدم اول: " + "</b> " + "برای ایجاد پین جدید نگهدارید!";
    String secondTipString = "<b>" + "قدم دوم: " + "</b> " + "برای حذف روی پین لمس کنید!";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // starting app in full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_remove_marker);
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

        // map listener: Long Click -> add marker Single Click -> deselect marker
        map.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapClicked(ClickData mapClickInfo) {
                super.onMapClicked(mapClickInfo);

                // long tap on map
                if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_LONG) {
                    // check the bottom sheet expanded or collapsed
                    if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {

                        if (selectedMarker == null) {
                            // if bottom sheet is expanded and no marker selected second tip is going up (for just one time)
                            collapseBottomSheet();
                            remove_marker_bottom_sheet.post(new Runnable() {
                                @Override
                                public void run() {
                                    marker_id.setText(Html.fromHtml(secondTipString));
                                }
                            });
                            // delay for collapsing then expanding bottom sheet
                            remove_marker_bottom_sheet.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    expandBottomSheet();
                                }
                            }, 200);
                        } else {
                            // if bottom sheet is expanded and any marker selected deselect that marker by long tap
                            deselectMarker(selectedMarker);
                        }
                    }
                    // by calling getClickPos(), we can get position of clicking (or tapping)
                    LngLat clickedLocation = mapClickInfo.getClickPos();
                    // addMarker adds a marker (pretty self explanatory :D) to the clicked location
                    addMarker(clickedLocation, markerId);
                    // increment id
                    markerId++;
                } else if (mapClickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE && selectedMarker != null) {
                    // deselect marker when tap on map and a marker is selected
                    deselectMarker(selectedMarker);
                }
            }
        });

        // marker listener for select and deselect markers
        markerLayer.setVectorElementEventListener(new VectorElementEventListener() {
            @Override
            public boolean onVectorElementClicked(ElementClickData clickInfo) {
                if (clickInfo.getClickType() == ClickType.CLICK_TYPE_SINGLE) {
                    final int vectorElementId = (int) clickInfo.getVectorElement().getMetaDataElement("id").getLong();
                    if (selectedMarker != null) {
                        // deselect marker when tap on a marker and a marker is selected
                        deselectMarker(selectedMarker);
                    } else {
                        // select marker when tap on a marker
                        selectMarker((Marker) clickInfo.getVectorElement());
                        remove_marker_bottom_sheet.post(new Runnable() {
                            @Override
                            public void run() {
                                marker_id.setText("از حدف پین " + vectorElementId + " اطمینان دارید؟");
                                remove_marker.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }
                return true;
            }
        });

        // remove marker and deselect that marker
        remove_marker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedMarker != null) {
                    markerLayer.remove(selectedMarker);
                    deselectMarker(selectedMarker);
                }
            }
        });

    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
        marker_id = findViewById(R.id.marker_id);
        remove_marker = findViewById(R.id.remove_marker);
        // bottom sheet include tag and behavior
        remove_marker_bottom_sheet = findViewById(R.id.remove_marker_bottom_sheet_include);
        bottomSheetBehavior = BottomSheetBehavior.from(remove_marker_bottom_sheet);
        remove_marker.setVisibility(View.GONE);
        marker_id.setText(Html.fromHtml(firstTipString));
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        // bottom sheet callback deselect marker for when bottom sheet collapsed manually
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED && selectedMarker != null) {
                    deselectMarker(selectedMarker);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
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
//      If you want to have only one marker on map at a time, uncomment next line to delete all markers before adding a new marker
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
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker_blue)));
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

    // change selected marker color to blue
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
    // change deselected marker color to red
    private void changeMarkerToRed(Marker blueMarker){
        // create new marker style
        MarkerStyleCreator markStCr = new MarkerStyleCreator();
        markStCr.setSize(30f);
        // Setting a new bitmap as marker
        markStCr.setBitmap(BitmapUtils.createBitmapFromAndroidBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker)));
        markStCr.setAnimationStyle(animSt);
        MarkerStyle redMarkSt = markStCr.buildStyle();

        // changing marker style using setStyle
        blueMarker.setStyle(redMarkSt);
    }

    // deselect marker and collapsing bottom sheet
    private void deselectMarker(final Marker deselectMarker) {
        collapseBottomSheet();
        changeMarkerToBlue(deselectMarker);
        selectedMarker = null;
    }
    // select marker and expanding bottom sheet
    private void selectMarker(final Marker selectMarker) {
        expandBottomSheet();
        changeMarkerToRed(selectMarker);
        selectedMarker = selectMarker;
    }

    // collapse bottom sheet
    private void collapseBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }
    // expand bottom sheet
    private void expandBottomSheet() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    // customize back button for when a marker is selected
    @Override
    public void onBackPressed() {
        if (selectedMarker != null) {
            deselectMarker(selectedMarker);
        } else {
            super.onBackPressed();
        }
    }
}
