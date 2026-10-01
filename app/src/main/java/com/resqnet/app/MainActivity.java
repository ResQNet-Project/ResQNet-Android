package com.resqnet.app;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.resqnet.app.nearby.NearbyManager;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // =========================================================
    // VARIABLES
    // =========================================================

    private FusedLocationProviderClient fusedLocationClient;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MaterialToolbar toolbar;

    private NearbyManager nearbyManager;

    private static final int REQUEST_ALL_PERMISSIONS = 200;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);


        // =====================================================
        // NEARBY MANAGER
        // =====================================================

        nearbyManager = new NearbyManager(this);


        // =====================================================
        // LOCATION CLIENT
        // =====================================================

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);


        // =====================================================
        // PERMISSIONS
        // =====================================================

        if (hasAllRequiredPermissions()) {

            startNearby();

        } else {

            requestAllPermissions();
        }


        // =====================================================
        // DRAWER
        // =====================================================

        drawerLayout =
                findViewById(R.id.drawerLayout);

        navigationView =
                findViewById(R.id.navigationView);

        toolbar =
                findViewById(R.id.toolbar);


        // Open drawer
        toolbar.setNavigationOnClickListener(v -> {

            drawerLayout.openDrawer(navigationView);

        });


        // Drawer menu
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


        // =====================================================
        // SOS BUTTON
        // =====================================================

        Button btnSOS =
                findViewById(R.id.btnSOS);


        btnSOS.setOnClickListener(v -> {


            // =================================================
            // CHECK LOCATION PERMISSION
            // =================================================

            boolean fineLocation =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;


            boolean coarseLocation =
                    ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED;


            // Accept either FINE or COARSE location
            if (!fineLocation && !coarseLocation) {

                Toast.makeText(
                        this,
                        "Location permission is required",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // =================================================
            // GET LOCATION
            // =================================================

            fusedLocationClient
                    .getLastLocation()
                    .addOnSuccessListener(location -> {


                        if (location != null) {


                            // =================================
                            // GET LATITUDE
                            // =================================

                            double latitude =
                                    location.getLatitude();


                            // =================================
                            // GET LONGITUDE
                            // =================================

                            double longitude =
                                    location.getLongitude();


                            // =================================
                            // DEVICE ID
                            // =================================

                            String senderId =
                                    getResQNetDeviceId();


                            // =================================
                            // CREATE SOS PACKET
                            // =================================

                            SOSPacket packet =
                                    new SOSPacket(
                                            senderId,
                                            latitude,
                                            longitude
                                    );


                            // =================================
                            // CONVERT SOS TO JSON
                            // =================================

                            Gson gson =
                                    new Gson();

                            String jsonPacket =
                                    gson.toJson(packet);


                            // =================================
                            // LOCAL STORAGE
                            // =================================

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
                                    .putBoolean(
                                            "seen_" +
                                                    packet.getMessageId(),
                                            true
                                    )
                                    .apply();


                            // =================================
                            // SEND THROUGH NEARBY
                            // =================================

                            nearbyManager.sendSOS(packet);


                            // =================================
                            // READ SAVED PACKET
                            // =================================

                            String savedPacket =
                                    prefs.getString(
                                            "sos_packet",
                                            null
                                    );


                            // =================================
                            // SHOW SOS
                            // =================================

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

                    })
                    .addOnFailureListener(e -> {

                        Toast.makeText(
                                this,
                                "Location error: "
                                        + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    });

        });


        // =====================================================
        // ADD MESSAGE BUTTON
        // =====================================================

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


    // =========================================================
    // CHECK ALL REQUIRED RUNTIME PERMISSIONS
    // =========================================================

    private boolean hasAllRequiredPermissions() {


        // =====================================================
        // LOCATION
        // =====================================================

        boolean fineLocation =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        boolean coarseLocation =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;


        if (!fineLocation && !coarseLocation) {

            return false;
        }


        // =====================================================
        // BLUETOOTH - ANDROID 12+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 31) {


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADVERTISE
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }


        // =====================================================
        // NEARBY WIFI - ANDROID 13+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 33) {


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }


        // =====================================================
        // LOCAL NETWORK - ANDROID 17+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 37) {


            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_LOCAL_NETWORK
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // REQUEST ALL PERMISSIONS
    // ONLY ONE REQUEST IS MADE
    // =========================================================

    private void requestAllPermissions() {


        ArrayList<String> permissions =
                new ArrayList<>();


        // =====================================================
        // LOCATION
        // =====================================================

        permissions.add(
                Manifest.permission.ACCESS_FINE_LOCATION
        );

        permissions.add(
                Manifest.permission.ACCESS_COARSE_LOCATION
        );


        // =====================================================
        // BLUETOOTH - ANDROID 12+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            permissions.add(
                    Manifest.permission.BLUETOOTH_SCAN
            );

            permissions.add(
                    Manifest.permission.BLUETOOTH_CONNECT
            );

            permissions.add(
                    Manifest.permission.BLUETOOTH_ADVERTISE
            );
        }


        // =====================================================
        // NEARBY WIFI - ANDROID 13+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 33) {

            permissions.add(
                    Manifest.permission.NEARBY_WIFI_DEVICES
            );
        }


        // =====================================================
        // LOCAL NETWORK - ANDROID 17+
        // =====================================================

        if (android.os.Build.VERSION.SDK_INT >= 37) {

            permissions.add(
                    Manifest.permission.ACCESS_LOCAL_NETWORK
            );
        }


        // =====================================================
        // ONE REQUEST ONLY
        // =====================================================

        ActivityCompat.requestPermissions(
                this,
                permissions.toArray(
                        new String[0]
                ),
                REQUEST_ALL_PERMISSIONS
        );
    }


    // =========================================================
    // START NEARBY
    // =========================================================

    private void startNearby() {

        nearbyManager.startAdvertising();

        nearbyManager.startDiscovery();
    }


    // =========================================================
    // PERMISSION RESULT
    // =========================================================

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );


        if (requestCode == REQUEST_ALL_PERMISSIONS) {


            // ================================================
            // CHECK AGAIN
            // ================================================

            if (hasAllRequiredPermissions()) {


                Toast.makeText(
                        this,
                        "All permissions granted",
                        Toast.LENGTH_SHORT
                ).show();


                // ============================================
                // START NEARBY
                // ============================================

                startNearby();

            }

            else {

                Toast.makeText(
                        this,
                        "Required permissions denied",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }


    // =========================================================
    // DEVICE ID
    // =========================================================

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
}