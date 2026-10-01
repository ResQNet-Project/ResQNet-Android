package com.resqnet.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;

import java.util.UUID;

public class AddMessageActivity extends AppCompatActivity {

    // =========================================================
    // VARIABLES
    // =========================================================

    private FusedLocationProviderClient fusedLocationClient;

    private TextInputEditText etMessage;

    private TextView tvLocationStatus;

    private MaterialButton btnSendMessage;


    // Permission request code
    private static final int LOCATION_PERMISSION_REQUEST = 101;


    // =========================================================
    // ON CREATE
    // =========================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_message);


        // =====================================================
        // FIND VIEWS
        // =====================================================

        etMessage =
                findViewById(R.id.etMessage);

        tvLocationStatus =
                findViewById(R.id.tvLocationStatus);

        btnSendMessage =
                findViewById(R.id.btnSendMessage);


        // =====================================================
        // LOCATION CLIENT
        // =====================================================

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(
                        this
                );


        // =====================================================
        // SEND BUTTON
        // =====================================================

        btnSendMessage.setOnClickListener(v -> {

            sendEmergencyMessage();

        });
    }


    // =========================================================
    // SEND EMERGENCY MESSAGE
    // =========================================================

    private void sendEmergencyMessage() {

        // =====================================================
        // GET MESSAGE
        // =====================================================

        String message =
                etMessage
                        .getText()
                        .toString()
                        .trim();


        // =====================================================
        // CHECK EMPTY MESSAGE
        // =====================================================

        if (message.isEmpty()) {

            etMessage.setError(
                    "Please describe your situation"
            );

            return;
        }


        // =====================================================
        // CHECK LOCATION PERMISSION
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


        // Accept either Fine OR Coarse
        if (!fineLocation && !coarseLocation) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }


        // =====================================================
        // GET LOCATION
        // =====================================================

        tvLocationStatus.setText(
                "📍 Getting your location..."
        );


        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(location -> {


                    // =================================================
                    // LOCATION NOT AVAILABLE
                    // =================================================

                    if (location == null) {

                        tvLocationStatus.setText(
                                "❌ Unable to get your location"
                        );


                        Toast.makeText(
                                this,
                                "Location unavailable",
                                Toast.LENGTH_SHORT
                        ).show();


                        return;
                    }


                    // =================================================
                    // GET COORDINATES
                    // =================================================

                    double latitude =
                            location.getLatitude();


                    double longitude =
                            location.getLongitude();


                    // =================================================
                    // UPDATE UI
                    // =================================================

                    tvLocationStatus.setText(
                            "📍 Location attached"
                    );


                    // =================================================
                    // GET DEVICE ID
                    // =================================================

                    String senderId =
                            getResQNetDeviceId();


                    // =================================================
                    // CREATE SOS PACKET
                    // =================================================

                    SOSPacket packet =
                            new SOSPacket(
                                    senderId,
                                    latitude,
                                    longitude
                            );


                    // =================================================
                    // CONVERT PACKET TO JSON
                    // =================================================

                    Gson gson =
                            new Gson();


                    String jsonPacket =
                            gson.toJson(packet);


                    // =================================================
                    // SAVE TO SHARED PREFERENCES
                    // =================================================

                    SharedPreferences prefs =
                            getSharedPreferences(
                                    "ResQNet",
                                    MODE_PRIVATE
                            );


                    prefs.edit()
                            .putString(
                                    "emergency_message",
                                    message
                            )
                            .putString(
                                    "message_packet",
                                    jsonPacket
                            )
                            .apply();


                    // =================================================
                    // SHOW SUCCESS
                    // =================================================

                    Toast.makeText(
                            this,
                            "Emergency message saved",
                            Toast.LENGTH_LONG
                    ).show();


                    // =================================================
                    // CLOSE ACTIVITY
                    // =================================================

                    finish();

                })

                .addOnFailureListener(e -> {

                    tvLocationStatus.setText(
                            "❌ Unable to get your location"
                    );


                    Toast.makeText(
                            this,
                            "Location error: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }


    // =========================================================
    // GET RESQNET DEVICE ID
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


        // =====================================================
        // CREATE DEVICE ID IF NEEDED
        // =====================================================

        if (deviceId == null) {

            deviceId =
                    UUID.randomUUID()
                            .toString();


            prefs.edit()
                    .putString(
                            "device_id",
                            deviceId
                    )
                    .apply();
        }


        return deviceId;
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


        // =====================================================
        // LOCATION PERMISSION RESULT
        // =====================================================

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {


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


            // =================================================
            // PERMISSION GRANTED
            // =================================================

            if (fineLocation || coarseLocation) {

                sendEmergencyMessage();

            }

            // =================================================
            // PERMISSION DENIED
            // =================================================

            else {

                Toast.makeText(
                        this,
                        "Location permission is required",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}