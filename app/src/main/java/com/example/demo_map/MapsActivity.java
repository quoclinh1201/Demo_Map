package com.example.demo_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.demo_map.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private ActivityMapsBinding binding;

    private int state;
    private Button btnChangeStyle;
    private Location currentLocation;
    private static final LatLng BEN_THANH_MARKET = new LatLng(10.7731, 106.6983);
    private static final LatLng SAI_GON_OPERA_HOUSE = new LatLng(10.7767, 106.7032);
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng p;
    private int status;
    private static final int MY_PERMISSION = 6789;

    private boolean locationPermissionGranted;
    private Location lastKnownLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        state = 0;
        status = 0;
        btnChangeStyle = findViewById(R.id.btnChangeStyle);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Ben Thanh Market and move the camera
//        map.addMarker(new MarkerOptions().position(BEN_THANH_MARKET).title("Marker in Ben Thanh Market"));
//        map.moveCamera(CameraUpdateFactory.newLatLng(BEN_THANH_MARKET));

        // Prompt the user for permission.
        getLocationPermission();
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();
        // Get the current location of the device and set the position of the map.
        getDeviceLocation();
    }

    private void getDeviceLocation() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            String provider = locationManager.getBestProvider(criteria, false);
            currentLocation = locationManager.getLastKnownLocation(provider);

            if (currentLocation != null) {
                LatLng currentPos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                Marker currentMarker = map.addMarker(new MarkerOptions()
                        .position(currentPos)
                        .title("My Location"));
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 15));
                map.animateCamera(CameraUpdateFactory.zoomTo(10), 2000, null);
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage(), e);
        }
    }


    private void updateLocationUI() {
        if (map == null) {
            return;
        }
        try {
            if (locationPermissionGranted) {
                map.setMyLocationEnabled(true);
                map.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                map.setMyLocationEnabled(false);
                map.getUiSettings().setMyLocationButtonEnabled(false);
                lastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.google_map, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (status != 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                getLocationPermission();
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(status != 0) {
            locationManager.removeUpdates(locationListener);
        }
    }

    public void clickToShowTraffic(MenuItem item) {
        if("Show Traffic".equals(item.getTitle())) {
            map.setTrafficEnabled(true);
            item.setTitle("Hide Traffic");
        } else {
            map.setTrafficEnabled(false);
            item.setTitle("Show Traffic");
        }
    }

    public void clickToZoomIn(MenuItem item) {
        map.animateCamera(CameraUpdateFactory.zoomIn());
    }

    public void clickToZoomOut(MenuItem item) {
        map.animateCamera(CameraUpdateFactory.zoomOut());
    }

    public void clickToGoToLocationAndMark(MenuItem item) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(BEN_THANH_MARKET).zoom(17).bearing(90).tilt(30).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        map.addMarker(new MarkerOptions()
                        .position(BEN_THANH_MARKET)
                        .title("Ben Thanh Market")
                        .snippet("HCM City"));
    }

    public void clickToShowCurrentLocation(MenuItem item) {
        currentLocation = map.getMyLocation();
        LatLng currentPos = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentPos).zoom(17).bearing(90).tilt(30).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void clickToLineConnection2Points(MenuItem item) {
        map.addMarker(new MarkerOptions()
                .position(SAI_GON_OPERA_HOUSE)
                .title("Sai Gon Opera House")
                .snippet("HCM City")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        map.addPolyline(new PolylineOptions().add(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), SAI_GON_OPERA_HOUSE).width(5).color(Color.RED));
    }

    public void clickToGetLocationData(MenuItem item) {
        locationListener = new MyLocationListener();
        locationListener.onLocationChanged(currentLocation);
        status = 1;
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            if(location != null) {
                Toast.makeText(getBaseContext(), "Position: " + location.getLatitude() + ":" + location.getLongitude(), Toast.LENGTH_LONG).show();
//                p = new LatLng(location.getLatitude(), location.getLongitude());
//                map.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
//                    @Override
//                    public void onCameraChange(@NonNull CameraPosition cameraPosition) {
//                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(p, 18));
//                    }
//                });
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String strStatus = "";
            switch (status) {
                case LocationProvider.AVAILABLE:
                    strStatus = "Available";
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    strStatus = "Out Of Service";
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    strStatus = "Temporarily Unavailable";
                    break;
            }
            Toast.makeText(getBaseContext(), provider + " " + strStatus, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            Toast.makeText(getBaseContext(), "Enabled provider " + provider, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            Toast.makeText(getBaseContext(), "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
        }
    }

    public void clickToActivateTheMapClick(MenuItem item) {
        map.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                Toast.makeText(getBaseContext(), "Position: " + latLng.latitude + ":" + latLng.longitude, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void clickToFind(View view) {
        EditText edtFind = findViewById(R.id.edtLocation);
        String location = edtFind.getText().toString();
        if(location != null && !location.trim().equals("")) {
            new GeocoderTask().execute(location);
        }
    }

    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected List<Address> doInBackground(String... locationName) {
            Geocoder geo = new Geocoder(getBaseContext());
            List<Address> addresses = null;
            try {
                addresses = geo.getFromLocationName(locationName[0], 3);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {
            super.onPostExecute(addresses);
            if(addresses == null || addresses.size() == 0) {
                Toast.makeText(getBaseContext(), "Not Found", Toast.LENGTH_LONG).show();
                return;
            }

            map.clear();
            for (int i = 0; i < addresses.size(); i++) {
                Address address = addresses.get(i);
                LatLng findPos = new LatLng(address.getLatitude(), address.getLongitude());
                String addressText = String.format("%s %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "", address.getCountryName());

                MarkerOptions mo = new MarkerOptions();
                mo.position(findPos);
                mo.title(addressText);

                map.addMarker(mo);
                if(i == 0) {
                    map.animateCamera(CameraUpdateFactory.newLatLng(findPos));
                }
            }
        }
    }

    public void clickToChangeStyle(View view) {
        String title = "";
        switch (state) {
            case 0:
                map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                title = "Normal - Change to Hybrid";
                state = 1;
                break;
            case 1:
                map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                title = "Hybrid - Change to Satellite";
                state = 2;
                break;
            case 2:
                map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                title = "Satellite - Change to Terrain";
                state = 3;
                break;
            case 3:
                map.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                title = "Terrain - Change to None";
                state = 4;
                break;
            case 4:
                map.setMapType(GoogleMap.MAP_TYPE_NONE);
                title = "None - Change to Normal";
                state = 0;
                break;
        }
        btnChangeStyle.setText(title);
    }
}