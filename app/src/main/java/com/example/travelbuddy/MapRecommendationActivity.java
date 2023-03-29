package com.example.travelbuddy;
import com.example.travelbuddy.AppwriteUserHelper;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.ApplicationInfo;

import java.util.UUID;

import io.appwrite.Client;
import io.appwrite.coroutines.CoroutineCallback;
import io.appwrite.exceptions.AppwriteException;

import io.appwrite.services.Account;
import io.appwrite.services.Databases;




import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class MapRecommendationActivity extends FragmentActivity implements OnMapReadyCallback {
    private static final String PLACES_API_BASE_URL = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

    private Fragment homeFragment;
    private Fragment searchFragment;
    private String userId;
    private AppwriteUserHelper appwriteUserHelper;

    private Fragment activeFragment;
    private FragmentManager fragmentManager;

    private GoogleMap mMap;
    private String category;
    PlacesClient placesClient;

    RectangularBounds bounds;
    TypeFilter typeFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_recommendation);
        AppwriteClientManager.initialize(this);

        appwriteUserHelper = new AppwriteUserHelper();
        userId = appwriteUserHelper.getUserId();

        // Get the intent and the category
        Intent intent = getIntent();
        category = intent.getStringExtra("category");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        // Initialize your fragments
        homeFragment = getSupportFragmentManager().findFragmentById(R.id.map);
       // searchFragment = new FragmentA(); // Replace with your desired fragment
        AccountFragment accountFragment = new AccountFragment();

        fragmentManager = getSupportFragmentManager();

        // Set the initial fragment
       // fragmentManager.beginTransaction().add(R.id.fragmentContainer, searchFragment).hide(searchFragment).commit();
        fragmentManager.beginTransaction().add(R.id.fragmentContainer, accountFragment).hide(accountFragment).commit();
        activeFragment = homeFragment;

        // Set up the BottomNavigationView listener
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        replaceFragment(homeFragment);
                        return true;
                    // case R.id.navigation_search:
                    //     replaceFragment(searchFragment);
                    //     return true;
                    case R.id.navigation_account:
                        replaceFragment(accountFragment);
                        return true;
                }
                return false;
            }
        });

    }

    private void replaceFragment(Fragment fragment) {
        if (activeFragment != fragment) {
            fragmentManager.beginTransaction().hide(activeFragment).show(fragment).commit();
            activeFragment = fragment;
        }
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this));

        mMap.setOnInfoWindowClickListener(marker -> {
            // Show a custom dialog or bottom sheet with interactive elements
            showBottomSheet(marker);
        });


        requestLocationPermissions();
        // Use the Places API to search for nearby places based on the selected category
        // and add markers to the map
    }

    private void showBottomSheet(Marker marker) {
        // Inflate the bottom sheet view
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet, null);
        TextView titleView = bottomSheetView.findViewById(R.id.bottom_sheet_title);
        TextView snippetView = bottomSheetView.findViewById(R.id.bottom_sheet_snippet);
        Button addButton = bottomSheetView.findViewById(R.id.add_to_list_button);

        titleView.setText(marker.getTitle());
        snippetView.setText(marker.getSnippet());

        // Set the click listener for the "Add to list" button
        addButton.setOnClickListener(v -> {
            // Show the AlertDialog when the button is clicked
            new AlertDialog.Builder(MapRecommendationActivity.this)
                    .setTitle("Add to list")
                    .setMessage("Are you sure you want to add '" + marker.getTitle() + "' to your list?")
                    .setPositiveButton("Yes, add it!", (dialog, which) -> {
                        // Call the addPlaceToAppwrite() function to save the place
                        try {
                            addPlaceToAppwrite(marker.getTitle(), marker.getSnippet(), marker.getPosition().latitude, marker.getPosition().longitude);
                        } catch (AppwriteException e) {
                            throw new RuntimeException(e);
                        }
                        Toast.makeText(MapRecommendationActivity.this, "Added: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("No, cancel", null) // No action needed for the negative button
                    .show();
        });

        // Show the bottom sheet
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }


    private void addPlaceToAppwrite(String name, String address, double latitude, double longitude) throws AppwriteException {
        Client client = AppwriteClientManager.getClient();
        Account account = AppwriteClientManager.getAccount();
        Databases databases = AppwriteClientManager.getDatabase();


        // Do something with the userId here
            Map<String, Object> placeData = new HashMap<>();
            placeData.put("name", name);
            placeData.put("address", address);
            placeData.put("latitude", latitude);
            placeData.put("longitude", longitude);
        placeData.put("userId", userId);


            String uniqueDocumentId = UUID.randomUUID().toString();

            databases.createDocument(
                    "641e5388852e4b190226",
                    "641e53904d90ec403156",
                    uniqueDocumentId,
                    placeData,
                    new CoroutineCallback<>((result, error) -> {
                        if (error != null) {
                            Log.d("Appwrite", "error" + name + address + latitude + longitude);
                            error.printStackTrace();
                            return;
                        }

                        Log.d("Appwrite", result.toString());
                        Log.d("Appwrite", "Place added successfully:");
                    })
            );

        }



    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private void requestLocationPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private FusedLocationProviderClient fusedLocationClient;

    private void getCurrentLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions();
            return;
        }

        Task<Location> locationTask = fusedLocationClient.getLastLocation();
        locationTask.addOnSuccessListener(location -> {
            if (location != null) {
                // Use the user's current location
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                LatLng currentLatLng = new LatLng(latitude, longitude);

                // Add a marker on the user's current location
              mMap.addMarker(new MarkerOptions().position(currentLatLng).title("You are here"));

                // Move the camera to the user's current location
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
               // fetchNearbyPlaces(currentLatLng);
                try {
                    getNearbyPlacesBasedOnCategory(latitude, longitude);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }

            } else {
                requestLocationPermissions();
                Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getNearbyPlacesBasedOnCategory(double latitude, double longitude) throws PackageManager.NameNotFoundException {
        LatLng currentLatLng = new LatLng(latitude, longitude);
        double radiusInMeters = 5000; // 5 km

        // Use the Nearby Search API to search for nearby restaurants
        ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        String apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY");
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" +
                currentLatLng.latitude + "," + currentLatLng.longitude +
                "&radius=" + radiusInMeters +
                "&type=" + category +
                "&key=" + apiKey;

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray results = json.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        JSONObject result = results.getJSONObject(i);
                        JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                        String name = result.getString("name");
                        String address = result.getString("vicinity");
                        double lat = location.getDouble("lat");
                        double lng = location.getDouble("lng");
                        LatLng placeLatLng = new LatLng(lat, lng);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mMap.addMarker(new MarkerOptions()
                                        .position(placeLatLng)
                                        .title(name)
                                        .snippet(address));
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
