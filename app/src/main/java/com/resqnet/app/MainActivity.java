package com.resqnet.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import java.util.UUID;

import com.resqnet.app.nearby.NearbyManager;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;



    private NearbyManager nearbyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);
        nearbyManager = new NearbyManager(this);
        if (!nearbyManager.hasNearbyPermissions()) {

            nearbyManager.requestNearbyPermissions();

        } else {

            nearbyManager.startAdvertising();
            nearbyManager.startDiscovery();
        }
        // =========================
        // DRAWER
        // =========================

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toolbar = findViewById(R.id.toolbar);


        // Open left drawer
        toolbar.setNavigationOnClickListener(v -> {

            drawerLayout.openDrawer(navigationView);

        });


        // Drawer menu clicks
        navigationView.setNavigationItemSelectedListener(item -> {

            int id = item.getItemId();


            if (id == R.id.nav_home) {

                drawerLayout.closeDrawer(navigationView);

            }

            else if (id == R.id.nav_notifications) {

                Toast.makeText(
                        this,
                        "Notifications / Chat",
                        Toast.LENGTH_SHORT
                ).show();

                drawerLayout.closeDrawer(navigationView);

            }

            else if (id == R.id.nav_contacts) {

                Toast.makeText(
                        this,
                        "Emergency Contacts",
                        Toast.LENGTH_SHORT
                ).show();

                drawerLayout.closeDrawer(navigationView);

            }

            else if (id == R.id.nav_history) {

                Toast.makeText(
                        this,
                        "SOS History",
                        Toast.LENGTH_SHORT
                ).show();

                drawerLayout.closeDrawer(navigationView);

            }

            else if (id == R.id.nav_settings) {

                Toast.makeText(
                        this,
                        "Settings",
                        Toast.LENGTH_SHORT
                ).show();

                drawerLayout.closeDrawer(navigationView);

            }

            else if (id == R.id.nav_about) {

                Toast.makeText(
                        this,
                        "ResQNet",
                        Toast.LENGTH_SHORT
                ).show();

                drawerLayout.closeDrawer(navigationView);

            }

            return true;
        });


        // =========================
        // LOCATION
        // =========================

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);


        // =========================
        // SOS BUTTON
        // =========================

        Button btnSOS = findViewById(R.id.btnSOS);


        btnSOS.setOnClickListener(v -> {

            // Check location permission
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        100
                );

                return;
            }


            // Get last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null) {

                            double latitude =
                                    location.getLatitude();

                            double longitude =
                                    location.getLongitude();


                            // Get device ID
                            String senderId =
                                    getResQNetDeviceId();


                            // Create SOS packet
                            SOSPacket packet =
                                    new SOSPacket(
                                            senderId,
                                            latitude,
                                            longitude
                                    );


                            // Convert to JSON
                            Gson gson = new Gson();

                            String jsonPacket =
                                    gson.toJson(packet);


                            // Save locally
                            SharedPreferences prefs =
                                    getSharedPreferences(
                                            "ResQNet",
                                            MODE_PRIVATE
                                    );

                            prefs.edit()
                                    .putString(
                                            "sos_packet",
                                            jsonPacket
                                    )
                                    .apply();


                            // Read saved packet
                            String savedPacket =
                                    prefs.getString(
                                            "sos_packet",
                                            null
                                    );


                            // Show result
                            Toast.makeText(
                                    this,
                                    savedPacket,
                                    Toast.LENGTH_LONG
                            ).show();

                        }

                        else {

                            Toast.makeText(
                                    this,
                                    "Unable to get location",
                                    Toast.LENGTH_SHORT
                            ).show();

                        }

                    });

        });
        // =========================
// ADD MESSAGE
// =========================

        Button btnAddMessage =
                findViewById(R.id.btnAddMessage);

        btnAddMessage.setOnClickListener(v -> {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            AddMessageActivity.class
                    );

            startActivity(intent);

        });

    }


    // =========================
    // DEVICE ID
    // =========================

    private String getResQNetDeviceId() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "ResQNet",
                        MODE_PRIVATE
                );


        String deviceId =
                prefs.getString(
                        "device_id",
                        null
                );


        if (deviceId == null) {

            deviceId =
                    UUID.randomUUID().toString();


            prefs.edit()
                    .putString(
                            "device_id",
                            deviceId
                    )
                    .apply();

        }


        return deviceId;
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == 200) {

            if (nearbyManager.hasNearbyPermissions()) {

                Toast.makeText(
                        this,
                        "Nearby permissions granted",
                        Toast.LENGTH_SHORT
                ).show();

                nearbyManager.startAdvertising();
                nearbyManager.startDiscovery();

            } else {

                Toast.makeText(
                        this,
                        "Nearby permissions denied",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}