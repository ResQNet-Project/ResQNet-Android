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

    private FusedLocationProviderClient fusedLocationClient;

    private TextInputEditText etMessage;
    private TextView tvLocationStatus;
    private MaterialButton btnSendMessage;

    private static final int LOCATION_PERMISSION_REQUEST = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_add_message);

        etMessage = findViewById(R.id.etMessage);

        tvLocationStatus =
                findViewById(R.id.tvLocationStatus);

        btnSendMessage =
                findViewById(R.id.btnSendMessage);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        btnSendMessage.setOnClickListener(v -> {

            sendEmergencyMessage();

        });
    }


    private void sendEmergencyMessage() {

        String message =
                etMessage.getText()
                        .toString()
                        .trim();

        if (message.isEmpty()) {

            etMessage.setError(
                    "Please describe your situation"
            );

            return;
        }


        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST
            );

            return;
        }


        tvLocationStatus.setText(
                "📍 Getting your location..."
        );


        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(location -> {

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


                    double latitude =
                            location.getLatitude();

                    double longitude =
                            location.getLongitude();


                    tvLocationStatus.setText(
                            "📍 Location attached"
                    );


                    String senderId =
                            getResQNetDeviceId();


                    SOSPacket packet =
                            new SOSPacket(
                                    senderId,
                                    latitude,
                                    longitude
                            );


                    Gson gson =
                            new Gson();

                    String jsonPacket =
                            gson.toJson(packet);


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


                    Toast.makeText(
                            this,
                            "Emergency message saved",
                            Toast.LENGTH_LONG
                    ).show();


                    finish();

                });
    }


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

        if (requestCode ==
                LOCATION_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                sendEmergencyMessage();

            } else {

                Toast.makeText(
                        this,
                        "Location permission is required",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}