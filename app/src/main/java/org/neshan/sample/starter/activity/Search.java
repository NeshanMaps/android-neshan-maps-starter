package org.neshan.sample.starter.activity;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import org.neshan.core.Bounds;
import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.core.ViewportBounds;
import org.neshan.core.ViewportPosition;
import org.neshan.layers.VectorElementLayer;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.adapter.SearchAdapter;
import org.neshan.sample.starter.model.search.Item;
import org.neshan.sample.starter.model.search.Location;
import org.neshan.sample.starter.model.search.NeshanSearch;
import org.neshan.sample.starter.network.GetDataService;
import org.neshan.sample.starter.network.RetrofitClientInstance;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.styles.MarkerStyle;
import org.neshan.styles.MarkerStyleCreator;
import org.neshan.ui.MapEventListener;
import org.neshan.ui.MapView;
import org.neshan.utils.BitmapUtils;
import org.neshan.vectorelements.Marker;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Search extends AppCompatActivity implements SearchAdapter.OnSearchItemListener {

    private static final String TAG = "Search";
    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;
    private EditText editText;
    private RecyclerView recyclerView;
    private List<Item> items;
    private SearchAdapter adapter;

    // map UI element
    private MapView map;
    private VectorElementLayer markerLayer;
    private VectorElementLayer centerMarkerLayer;
    private Marker centerMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        // everything related to ui is initialized here
        initLayoutReferences();
    }

    // Initializing layout references (views, map and map events)
    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();

        //listen for search text change
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                search(s.toString());
                Log.i(TAG, "afterTextChanged: " + s.toString());
            }
        });

    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
        editText = findViewById(R.id.search_editText);
        recyclerView = findViewById(R.id.recyclerView);
        items = new ArrayList<>();
        adapter = new SearchAdapter(items, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }


    private void initMap() {
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Creating a VectorElementLayer(called markerLayer) to add all markers to it and adding it to map's layers
        markerLayer = NeshanServices.createVectorElementLayer();
        centerMarkerLayer = NeshanServices.createVectorElementLayer();
        map.getLayers().add(markerLayer);
        map.getLayers().add(centerMarkerLayer);

        // Setting map focal position to a fixed position and setting camera zoom
        LngLat lngLat = new LngLat(51.330743, 35.767234);
        map.setFocalPointPosition(lngLat, 0);
        map.setZoom(14f, 0);
        centerMarker = new Marker(lngLat, getCenterMarkerStyle());
        centerMarkerLayer.add(centerMarker);

    }

    private void search(String term) {
        final double lat = map.getFocalPointPosition().getY();
        final double lng = map.getFocalPointPosition().getX();
        final String requestURL = "https://api.neshan.org/v1/search?term=" + term + "&lat=" + lat + "&lng=" + lng;

        GetDataService api = RetrofitClientInstance.getRetrofitInstance().create(GetDataService.class);
        Call<NeshanSearch> call = api.getNeshanSearch(requestURL);

        call.enqueue(new Callback<NeshanSearch>() {
            @Override
            public void onResponse(Call<NeshanSearch> call, Response<NeshanSearch> response) {
                if (response.isSuccessful()) {
                    NeshanSearch neshanSearch = response.body();
                    items = neshanSearch.getItems();
                    adapter.updateList(items);
                    addCenterMarker(new LngLat(lng , lat));
                } else {
                    Log.i(TAG, "onResponse: " + response.code() + " " + response.message());
                    Toast.makeText(Search.this, "خطا در برقراری ارتباط!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NeshanSearch> call, Throwable t) {
                Log.i(TAG, "onFailure: " + t.getMessage());
                Toast.makeText(Search.this, "ارتباط برقرار نشد!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void addCenterMarker(LngLat lngLat) {
        centerMarker.setPos(lngLat);
    }

    private MarkerStyle getCenterMarkerStyle() {
        MarkerStyleCreator styleCreator = new MarkerStyleCreator();
        styleCreator.setSize(50);
        styleCreator.setBitmap
                (BitmapUtils.createBitmapFromAndroidBitmap
                        (BitmapFactory.decodeResource(getResources(), R.drawable.center_marker)));
        return styleCreator.buildStyle();
    }


    private void addMarker(LngLat lngLat, float size) {
        Marker marker = new Marker(lngLat, getMarkerStyle(size));
        markerLayer.add(marker);
    }

    private MarkerStyle getMarkerStyle(float size) {
        MarkerStyleCreator styleCreator = new MarkerStyleCreator();
        styleCreator.setSize(size);
        styleCreator.setBitmap
                (BitmapUtils.createBitmapFromAndroidBitmap
                        (BitmapFactory.decodeResource(getResources(), R.drawable.ic_marker)));
        return styleCreator.buildStyle();
    }

    private void closeKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }


    public void showSearchClick(View view) {
        closeKeyBoard();
        adapter.updateList(items);
        markerLayer.clear();
    }

    public void showMarkersClick(View view) {
        adapter.updateList(new ArrayList<Item>());
        closeKeyBoard();
        markerLayer.clear();
        double minLat = Double.MAX_VALUE;
        double minLng = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double maxLng = Double.MIN_VALUE;
        for (Item item : items) {
            Location location = item.getLocation();
            LngLat lngLat = new LngLat(location.getX(), location.getY());
            addMarker(lngLat, 15f);
            minLat = Math.min(lngLat.getY(), minLat);
            minLng = Math.min(lngLat.getX(), minLng);
            maxLat = Math.max(lngLat.getY(), maxLat);
            maxLng = Math.max(lngLat.getX(), maxLng);
        }

        if (items.size() > 0) {
            map.moveToCameraBounds(new Bounds(new LngLat(minLng, minLat), new LngLat(maxLng, maxLat)),
                    new ViewportBounds(new ViewportPosition(0, 0), new ViewportPosition(map.getWidth(), map.getHeight())),
                    true, 0.5f);
        }


    }

    @Override
    public void onSeachItemClick(LngLat lngLat) {
        closeKeyBoard();
        markerLayer.clear();
        adapter.updateList(new ArrayList<Item>());
        map.setFocalPointPosition(lngLat, 0);
        map.setZoom(16f, 0);
        addMarker(lngLat, 30f);

    }
}
