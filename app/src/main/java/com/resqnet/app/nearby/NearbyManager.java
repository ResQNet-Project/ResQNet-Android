package com.resqnet.app.nearby;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.gson.Gson;
import com.resqnet.app.SOSPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class NearbyManager {

    // =========================================================
    // CONSTANTS
    // =========================================================

    private static final String SERVICE_ID =
            "com.resqnet.app";

    private static final String PREFS_NAME =
            "ResQNet";

    private static final String LOCAL_NETWORK_PERMISSION =
            "android.permission.ACCESS_LOCAL_NETWORK";


    // =========================================================
    // VARIABLES
    // =========================================================

    private final Activity activity;

    private final ConnectionsClient connectionsClient;

    private final Gson gson =
            new Gson();


    // Currently connected devices
    private final Set<String> connectedEndpoints =
            new HashSet<>();


    // Devices for which connection request is in progress
    private final Set<String> pendingEndpoints =
            new HashSet<>();


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public NearbyManager(Activity activity) {

        this.activity = activity;

        this.connectionsClient =
                Nearby.getConnectionsClient(activity);
    }


    // =========================================================
    // PERMISSIONS
    // =========================================================

    private boolean granted(String permission) {

        return ActivityCompat.checkSelfPermission(
                activity,
                permission
        ) == PackageManager.PERMISSION_GRANTED;
    }


    public boolean hasNearbyPermissions() {

        // -----------------------------------------------------
        // Android 11 and below
        // -----------------------------------------------------

        if (Build.VERSION.SDK_INT <= 31) {

            if (!granted(
                    Manifest.permission.ACCESS_FINE_LOCATION
            )) {

                return false;
            }
        }


        // -----------------------------------------------------
        // Bluetooth - Android 12+
        // -----------------------------------------------------

        if (Build.VERSION.SDK_INT >= 31) {

            if (!granted(
                    Manifest.permission.BLUETOOTH_SCAN
            )
                    || !granted(
                    Manifest.permission.BLUETOOTH_CONNECT
            )
                    || !granted(
                    Manifest.permission.BLUETOOTH_ADVERTISE
            )) {

                return false;
            }
        }


        // -----------------------------------------------------
        // Nearby Wi-Fi Devices - Android 13+
        // -----------------------------------------------------

        if (Build.VERSION.SDK_INT >= 33) {

            if (!granted(
                    Manifest.permission.NEARBY_WIFI_DEVICES
            )) {

                return false;
            }
        }


        // -----------------------------------------------------
        // Local Network - Android 17+
        // -----------------------------------------------------

        if (Build.VERSION.SDK_INT >= 37) {

            if (!granted(
                    LOCAL_NETWORK_PERMISSION
            )) {

                return false;
            }
        }


        return true;
    }


    // =========================================================
    // REQUEST NEARBY PERMISSIONS
    //
    // NOTE:
    // MainActivity now handles the single permission request.
    // This method is kept here for compatibility but should
    // NOT be called from MainActivity anymore.
    // =========================================================

    public void requestNearbyPermissions() {

        ArrayList<String> permissions =
                new ArrayList<>();


        // Android 11 and below
        if (Build.VERSION.SDK_INT <= 31) {

            permissions.add(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );

            permissions.add(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );
        }


        // Android 12+
        if (Build.VERSION.SDK_INT >= 31) {

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


        // Android 13+
        if (Build.VERSION.SDK_INT >= 33) {

            permissions.add(
                    Manifest.permission.NEARBY_WIFI_DEVICES
            );
        }


        // Android 17+
        if (Build.VERSION.SDK_INT >= 37) {

            permissions.add(
                    LOCAL_NETWORK_PERMISSION
            );
        }


        if (permissions.isEmpty()) {

            return;
        }


        ActivityCompat.requestPermissions(
                activity,
                permissions.toArray(
                        new String[0]
                ),
                200
        );
    }


    // =========================================================
    // RECEIVE PAYLOAD
    // =========================================================

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {

                @Override
                public void onPayloadReceived(
                        String endpointId,
                        Payload payload
                ) {

                    // We only handle byte payloads
                    if (payload.getType()
                            != Payload.Type.BYTES) {

                        return;
                    }


                    byte[] bytes =
                            payload.asBytes();


                    if (bytes == null) {

                        return;
                    }


                    // Convert bytes → JSON
                    String json =
                            new String(
                                    bytes,
                                    StandardCharsets.UTF_8
                            );


                    try {

                        // JSON → SOSPacket
                        SOSPacket packet =
                                gson.fromJson(
                                        json,
                                        SOSPacket.class
                                );


                        // Validate packet
                        if (packet == null
                                || packet.getMessageId() == null) {

                            toast(
                                    "Invalid SOS packet",
                                    Toast.LENGTH_SHORT
                            );

                            return;
                        }


                        SharedPreferences prefs =
                                prefs();


                        String seenKey =
                                "seen_" +
                                        packet.getMessageId();


                        // =================================================
                        // DUPLICATE CHECK
                        // =================================================

                        if (prefs.getBoolean(
                                seenKey,
                                false
                        )) {

                            toast(
                                    "Duplicate SOS ignored",
                                    Toast.LENGTH_SHORT
                            );

                            return;
                        }


                        // =================================================
                        // MARK MESSAGE AS SEEN
                        // =================================================

                        prefs.edit()
                                .putBoolean(
                                        seenKey,
                                        true
                                )
                                .apply();


                        // =================================================
                        // INCREASE HOP COUNT
                        // =================================================

                        packet.incrementHopCount();


                        // =================================================
                        // STORE SOS LOCALLY
                        // =================================================

                        prefs.edit()
                                .putString(
                                        "sos_packet",
                                        gson.toJson(packet)
                                )
                                .apply();


                        // =================================================
                        // SHOW RECEIVED SOS
                        // =================================================

                        toast(
                                "SOS received! Hop: "
                                        + packet.getHopCount(),
                                Toast.LENGTH_LONG
                        );


                        // =================================================
                        // FORWARD SOS
                        // =================================================

                        forwardSOS(
                                packet,
                                endpointId
                        );


                    } catch (Exception e) {

                        toast(
                                "Invalid SOS packet",
                                Toast.LENGTH_SHORT
                        );
                    }
                }


                @Override
                public void onPayloadTransferUpdate(
                        String endpointId,
                        PayloadTransferUpdate update
                ) {

                    // Not required for BYTES payloads
                }
            };


    // =========================================================
    // ENDPOINT DISCOVERY
    // =========================================================

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {

                @Override
                public void onEndpointFound(
                        String endpointId,
                        DiscoveredEndpointInfo info
                ) {

                    // Already connected?
                    if (connectedEndpoints.contains(
                            endpointId
                    )) {

                        return;
                    }


                    // Connection request already running?
                    if (pendingEndpoints.contains(
                            endpointId
                    )) {

                        return;
                    }


                    // Mark request as pending
                    pendingEndpoints.add(
                            endpointId
                    );


                    toast(
                            "ResQNet device found",
                            Toast.LENGTH_SHORT
                    );


                    // Request connection
                    connectionsClient.requestConnection(
                                    "ResQNet Device",
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnFailureListener(e -> {

                                pendingEndpoints.remove(
                                        endpointId
                                );

                            });
                }


                @Override
                public void onEndpointLost(
                        String endpointId
                ) {

                    pendingEndpoints.remove(
                            endpointId
                    );


                    toast(
                            "ResQNet device lost",
                            Toast.LENGTH_SHORT
                    );
                }
            };


    // =========================================================
    // START ADVERTISING
    // =========================================================

    public void startAdvertising() {

        AdvertisingOptions options =
                new AdvertisingOptions.Builder()
                        .setStrategy(
                                Strategy.P2P_CLUSTER
                        )
                        .build();


        connectionsClient.startAdvertising(
                        "ResQNet Device",
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        options
                )
                .addOnSuccessListener(
                        unused -> {

                            toast(
                                    "ResQNet advertising started",
                                    Toast.LENGTH_SHORT
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            toast(
                                    "Advertising failed: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // =========================================================
    // START DISCOVERY
    // =========================================================

    public void startDiscovery() {

        DiscoveryOptions options =
                new DiscoveryOptions.Builder()
                        .setStrategy(
                                Strategy.P2P_CLUSTER
                        )
                        .build();


        connectionsClient.startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        options
                )
                .addOnSuccessListener(
                        unused -> {

                            toast(
                                    "ResQNet discovery started",
                                    Toast.LENGTH_SHORT
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {

                            android.util.Log.e(
                                    "ResQNet",
                                    "Discovery failed",
                                    e
                            );


                            toast(
                                    "Discovery failed: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            );
                        }
                );
    }


    // =========================================================
    // STOP EVERYTHING
    // =========================================================

    public void stopAll() {

        connectionsClient.stopAdvertising();

        connectionsClient.stopDiscovery();

        connectionsClient.stopAllEndpoints();


        connectedEndpoints.clear();

        pendingEndpoints.clear();
    }


    // =========================================================
    // CONNECTION LIFECYCLE
    // =========================================================

    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId,
                        ConnectionInfo connectionInfo
                ) {

                    // Automatically accept connection
                    connectionsClient.acceptConnection(
                            endpointId,
                            payloadCallback
                    );
                }


                @Override
                public void onConnectionResult(
                        String endpointId,
                        ConnectionResolution result
                ) {

                    // Request is no longer pending
                    pendingEndpoints.remove(
                            endpointId
                    );


                    int code =
                            result.getStatus()
                                    .getStatusCode();


                    // =================================================
                    // CONNECTION SUCCESS
                    // =================================================

                    if (code ==
                            ConnectionsStatusCodes.STATUS_OK) {


                        // VERY IMPORTANT:
                        // Store connected endpoint
                        connectedEndpoints.add(
                                endpointId
                        );


                        toast(
                                "ResQNet device connected",
                                Toast.LENGTH_SHORT
                        );
                    }


                    // =================================================
                    // ALREADY CONNECTED
                    // =================================================

                    else if (
                            code ==
                                    ConnectionsStatusCodes
                                            .STATUS_ALREADY_CONNECTED_TO_ENDPOINT
                    ) {

                        connectedEndpoints.add(
                                endpointId
                        );
                    }


                    // =================================================
                    // CONNECTION FAILED
                    // =================================================

                    else {

                        toast(
                                "Connection failed",
                                Toast.LENGTH_SHORT
                        );
                    }
                }


                @Override
                public void onDisconnected(
                        String endpointId
                ) {

                    // Remove disconnected device
                    connectedEndpoints.remove(
                            endpointId
                    );


                    pendingEndpoints.remove(
                            endpointId
                    );


                    toast(
                            "ResQNet device disconnected",
                            Toast.LENGTH_SHORT
                    );
                }
            };


    // =========================================================
    // FORWARD SOS
    // =========================================================

    private void forwardSOS(
            SOSPacket packet,
            String receivedFromEndpoint
    ) {

        // Convert packet → JSON → bytes
        Payload payload =
                Payload.fromBytes(
                        gson.toJson(packet)
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );


        // Send to every connected device
        for (String endpointId :
                connectedEndpoints) {


            // IMPORTANT:
            // Don't immediately send it back
            // to the phone that sent it.
            if (endpointId.equals(
                    receivedFromEndpoint
            )) {

                continue;
            }


            connectionsClient.sendPayload(
                    endpointId,
                    payload
            );
        }


        toast(
                "SOS forwarded. Hop: "
                        + packet.getHopCount(),
                Toast.LENGTH_SHORT
        );
    }


    // =========================================================
    // SEND ORIGINAL SOS
    // =========================================================

    public void sendSOS(
            SOSPacket packet
    ) {


        // No connected devices
        if (connectedEndpoints.isEmpty()) {

            toast(
                    "No ResQNet devices connected",
                    Toast.LENGTH_SHORT
            );

            return;
        }


        // =====================================================
        // MARK OUR OWN SOS AS SEEN
        // =====================================================

        if (packet.getMessageId() != null) {

            prefs()
                    .edit()
                    .putBoolean(
                            "seen_" +
                                    packet.getMessageId(),
                            true
                    )
                    .apply();
        }


        // =====================================================
        // CREATE PAYLOAD
        // =====================================================

        Payload payload =
                Payload.fromBytes(
                        gson.toJson(packet)
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );


        // =====================================================
        // SEND TO ALL CONNECTED DEVICES
        // =====================================================

        for (String endpointId :
                connectedEndpoints) {

            connectionsClient.sendPayload(
                    endpointId,
                    payload
            );
        }


        toast(
                "SOS sent to nearby devices",
                Toast.LENGTH_SHORT
        );
    }


    // =========================================================
    // SHARED PREFERENCES
    // =========================================================

    private SharedPreferences prefs() {

        return activity.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }


    // =========================================================
    // TOAST HELPER
    // =========================================================

    private void toast(
            String msg,
            int length
    ) {

        Toast.makeText(
                activity,
                msg,
                length
        ).show();
    }
}