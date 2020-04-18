package org.neshan.sample.starter.activity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.neshan.core.LngLat;
import org.neshan.core.Range;
import org.neshan.sample.starter.R;
import org.neshan.sample.starter.model.address.NeshanAddress;
import org.neshan.sample.starter.network.RetrofitClientInstance;
import org.neshan.sample.starter.network.ReverseService;
import org.neshan.services.NeshanMapStyle;
import org.neshan.services.NeshanServices;
import org.neshan.ui.MapView;

import java.util.concurrent.TimeUnit;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public class APIRetrofit extends AppCompatActivity {

    // layer number in which map is added
    final int BASE_MAP_INDEX = 0;

    public static final ReverseService GET_DATA_SERVICE = RetrofitClientInstance.getRetrofitInstance().create(ReverseService.class);
    private final PublishSubject<LngLat> locationPublishSubject = PublishSubject.create();

    //ui elements in bottom sheet
    private TextView addressTitle;
    private ProgressBar progressBar;
    private MapView map;

    private Disposable disposable;
    private LngLat currentLocation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_api_retrofit);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // everything related to ui is initialized here
        initLayoutReferences();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (disposable != null && !disposable.isDisposed()){
            disposable.dispose();
        }
    }

    private void initLayoutReferences() {
        // Initializing views
        initViews();
        // Initializing mapView element
        initMap();

        initReverseObserver();

    }

    private void initReverseObserver() {

        disposable = locationPublishSubject
                // Request for reverse after n Milliseconds delay to prevent from unnecessary requests during user location change
                .debounce(100, TimeUnit.MILLISECONDS)
                .filter(it -> {
                    runOnUiThread(()-> progressBar.setVisibility(View.VISIBLE));
                    return true;
                })
                // Request for reverse geo and change MapPos observable to ReverseResult observable flow
                .flatMap(this::getReverseObserver)
                // Will run onNext on Android mainThread because we need update UI
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {

                    String address = result.getAddress();
                    if (result.getAddress() == null)
                        return;

                    if (address != null && !address.isEmpty()) {
                        addressTitle.setText(address);
                    } else {
                        addressTitle.setText("معبر بی‌نام");
                    }
                    progressBar.setVisibility(View.INVISIBLE);
                }, t -> {
                    addressTitle.setText("معبر بی‌نام");
                    progressBar.setVisibility(View.INVISIBLE);
                });

        locationPublishSubject.onNext(currentLocation);
    }

    private Observable<NeshanAddress> getReverseObserver(LngLat it){
        return GET_DATA_SERVICE
                .getReverse("https://api.neshan.org/v1/reverse?lat=" + it.getY() + "&lng=" + it.getX())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // We use findViewByID for every element in our layout file here
    private void initViews() {
        map = findViewById(R.id.map);
        addressTitle = findViewById(R.id.addressTitle);
        progressBar = findViewById(R.id.progressBar);
    }

    // Initializing map
    @SuppressLint("ClickableViewAccessibility")
    private void initMap() {
        currentLocation = new LngLat(51.330743, 35.767234);
        // add Standard_day map to layer BASE_MAP_INDEX
        map.getOptions().setZoomRange(new Range(4.5f, 18f));
        map.getLayers().insert(BASE_MAP_INDEX, NeshanServices.createBaseMap(NeshanMapStyle.STANDARD_DAY));

        // Setting map focal position to a fixed position and setting camera zoom
        map.setFocalPointPosition(currentLocation, 0);
        map.setZoom(14, 0);

        map.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                currentLocation = map.getFocalPointPosition();
                locationPublishSubject.onNext(currentLocation);
            }
            return false;
        });
    }

}
