package com.example.gpstracker.view;

import android.Manifest;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.gpstracker.R;
import com.example.gpstracker.model.database.TrackerRepository;
import com.example.gpstracker.model.pojo.LocationPoint;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private final String TAG = MainActivity.class.getSimpleName();

    //interval for location updates
    private final int UPDATE_INTERVAL_MILLIS = 120000;

    //stores location from internal storage
    List<LocationPoint> list;

    //db instance
    private TrackerRepository repository;

    //access to Google map API
    GoogleMap map;

    //access to fused location API
    private FusedLocationProviderClient fusedLocationProviderClient;

    //access to location settings
    private SettingsClient settingsClient;

    //used to request FusedLocation Api.
    private LocationRequest locationRequest;

    //used to check optimal location settings
    private LocationSettingsRequest locationSettingsRequest;

    //callback for location requests
    private LocationCallback locationCallback;

    //contains current location
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = TrackerRepository.getInstance(this);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        initLocationCallback();
        initLocationRequest();
        buildLocationSettingsRequest();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);

        updateLocation();
    }

    //check if a device has required location settings
    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        locationSettingsRequest = builder.build();
    }

    //inits fine location request
    private void initLocationRequest() {

        locationRequest = new LocationRequest();

        //Desired interval to request updates. Might be vary
        locationRequest.setInterval(UPDATE_INTERVAL_MILLIS);

        //Desired minimum interval to request updates
        locationRequest.setFastestInterval(UPDATE_INTERVAL_MILLIS / 2);
    }

    //initing callback to retrieve location updates
    private void initLocationCallback() {
        locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                currentLocation = locationResult.getLastLocation();
                addNewLocation(currentLocation);
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng def = new LatLng(55.798210, 49.105466);
        drawMarkers();
        map.moveCamera(CameraUpdateFactory.newLatLng(def));
    }

    private void drawMarkers() {
        TrackerRepository repository = TrackerRepository.getInstance(this);
        list = new ArrayList<>();

        try {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... voids) {
                    list = TrackerRepository.getAll();
                    return null;
                }

            }.execute().get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        for (LocationPoint locationPoint : list) {
            Log.d("Main", "doInBackground: " + locationPoint.getTimeStamp());
            LatLng def = new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude());
            map.addMarker(new MarkerOptions().position(def).title(locationPoint.getTimeStamp()));
        }
    }

    void updateLocation() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 8);
                                return;
                            }
                        }
                        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        switch (((ApiException) e).getStatusCode()) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.d(TAG, "Location settings are not OK. Trying to evaluate");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(MainActivity.this, 1);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, sie.getMessage());
                                }
                                break;
                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are not met requirements. Check your settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    void addNewLocation(Location location) {
        Log.d(TAG, "Added New Location: " + location.getLongitude() + ", " + location.getLatitude() + ", Date:"+(new Date().toString()));
        LocationPoint locationPoint = new LocationPoint(location.getLatitude(),location.getLongitude(),(new Date().toString()));
        TrackerRepository.insert(locationPoint);
        LatLng def = new LatLng(locationPoint.getLatitude(), locationPoint.getLongitude());
        map.addMarker(new MarkerOptions().position(def).title(locationPoint.getTimeStamp()));
    }

    /*@RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermissions() {
        if (ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)) {
            } else {
                // do request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 8);
            }
        }
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ){
            //code
        }
    }

    //stop location updates to decrease battery usage.
    @Override
    protected void onPause() {
        super.onPause();

        stopUpdates();
    }

    //resume location update
    @Override
    public void onResume() {
        super.onResume();
        updateLocation();
    }

    //stop location update
    private void stopUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                });
    }

}
