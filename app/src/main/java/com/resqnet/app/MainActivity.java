package com.resqnet.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import java.util.UUID;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;


public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
 //comment to undertand niggas

        // Connect Android's location service to our app
        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        // Enable edge-to-edge display
        EdgeToEdge.enable(this);

        // Load activity_main.xml
        setContentView(R.layout.activity_main);


        // Find the SOS button from activity_main.xml
        Button btnSOS = findViewById(R.id.btnSOS);


        // Run this code when SOS button is clicked
        btnSOS.setOnClickListener(v -> {

            // Check if location permission is granted
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {

                // Ask the user for location permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION
                        },
                        100
                );

                return;
            }


            // Get the phone's last known location
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {

                        if (location != null) {

                            // Get latitude and longitude
                            double latitude =
                                    location.getLatitude();

                            double longitude =
                                    location.getLongitude();


                            // Get this phone's unique ID
                            String senderId =
                                    getResQNetDeviceId();


                            // Create SOS packet
                            SOSPacket packet =
                                    new SOSPacket(
                                            senderId,
                                            latitude,
                                            longitude
                                    );


                            // Convert Java object into JSON
                            Gson gson = new Gson();

                            String jsonPacket =
                                    gson.toJson(packet);


                            // Open local storage
                            SharedPreferences prefs =
                                    getSharedPreferences(
                                            "ResQNet",
                                            MODE_PRIVATE
                                    );


                            // Save JSON packet locally
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


                            // Show saved packet on screen
                            Toast.makeText(
                                    this,
                                    savedPacket,
                                    Toast.LENGTH_LONG
                            ).show();
                        }

                    });
        });
    }


    // Get existing device ID
    // OR create a new one if this is the first time
    private String getResQNetDeviceId() {

        // Open ResQNet local storage
        SharedPreferences prefs =
                getSharedPreferences(
                        "ResQNet",
                        MODE_PRIVATE
                );


        // Try to get an existing device ID
        String deviceId =
                prefs.getString(
                        "device_id",
                        null
                );


        // If no device ID exists, create one
        if (deviceId == null) {

            deviceId =
                    UUID.randomUUID().toString();


            // Save the generated device ID
            prefs.edit()
                    .putString(
                            "device_id",
                            deviceId
                    )
                    .apply();
        }


        // Return the device ID
        return deviceId;
    }
}