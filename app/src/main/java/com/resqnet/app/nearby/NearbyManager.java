package com.resqnet.app.nearby;

import android.Manifest;
import android.widget.Toast;
import android.app.Activity;
import android.content.pm.PackageManager;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;

import com.google.android.gms.nearby.connection.Payload;


import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;

import java.util.HashSet;
import java.util.Set;
import com.google.gson.Gson;
import com.resqnet.app.SOSPacket;
public class NearbyManager {

    private Activity activity;
    private ConnectionsClient connectionsClient;
    private static final String SERVICE_ID = "com.resqnet.app";

    private final Set<String> connectedEndpoints = new HashSet<>();
    public NearbyManager(Activity activity) {

        this.activity = activity;

        connectionsClient =
                Nearby.getConnectionsClient(activity);
    }

    public boolean hasNearbyPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            boolean bluetoothPermissions =
                    ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH_SCAN
                    ) == PackageManager.PERMISSION_GRANTED

                            && ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED

                            && ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.BLUETOOTH_ADVERTISE
                    ) == PackageManager.PERMISSION_GRANTED;

            if (!bluetoothPermissions) {
                return false;
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 32) {

            if (ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.NEARBY_WIFI_DEVICES
            ) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }

        return true;
    }
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {

                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {

                    if (payload.getType() != Payload.Type.BYTES) {
                        return;
                    }

                    String json = new String(payload.asBytes());

                    Gson gson = new Gson();

                    try {

                        SOSPacket packet = gson.fromJson(json, SOSPacket.class);

                        String messageId = packet.getMessageId();

                        // Check whether we have already seen this SOS
                        android.content.SharedPreferences prefs =
                                activity.getSharedPreferences(
                                        "ResQNet",
                                        android.content.Context.MODE_PRIVATE
                                );

                        String seenKey = "seen_" + messageId;

                        if (prefs.getBoolean(seenKey, false)) {

                            Toast.makeText(
                                    activity,
                                    "Duplicate SOS ignored",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // Mark this SOS as seen
                        prefs.edit()
                                .putBoolean(seenKey, true)
                                .apply();

                        // Increase hop count
                        packet.incrementHopCount();

                        // Store received SOS locally
                        String updatedJson = gson.toJson(packet);

                        prefs.edit()
                                .putString("sos_packet", updatedJson)
                                .apply();

                        Toast.makeText(
                                activity,
                                "SOS received! Hop: "
                                        + packet.getHopCount(),
                                Toast.LENGTH_LONG
                        ).show();

                        // Forward to other connected devices
                        forwardSOS(packet, endpointId);

                    } catch (Exception e) {

                        Toast.makeText(
                                activity,
                                "Invalid SOS packet",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(
                        String endpointId,
                        PayloadTransferUpdate update) {
                }
            };
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {

                @Override
                public void onEndpointFound(
                        String endpointId,
                        DiscoveredEndpointInfo info) {

                    Toast.makeText(
                            activity,
                            "ResQNet device found",
                            Toast.LENGTH_SHORT
                    ).show();

                    connectionsClient.requestConnection(
                            "ResQNet Device",
                            endpointId,
                            connectionLifecycleCallback
                    );
                }

                @Override
                public void onEndpointLost(String endpointId) {

                    Toast.makeText(
                            activity,
                            "ResQNet device lost",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            };
    public void startAdvertising() {

        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder()
                        .setStrategy(Strategy.P2P_CLUSTER)
                        .build();

        connectionsClient.startAdvertising(
                        "ResQNet Device",
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        advertisingOptions
                )
                .addOnSuccessListener(unused -> {

                    // Advertising started successfully

                })
                .addOnFailureListener(e -> {

                    // Advertising failed

                });
    }
    public void startDiscovery() {

        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder()
                        .setStrategy(Strategy.P2P_CLUSTER)
                        .build();

        connectionsClient.startDiscovery(
                        SERVICE_ID,
                        endpointDiscoveryCallback,
                        discoveryOptions
                )
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            activity,
                            "ResQNet discovery started",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            activity,
                            "Discovery failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                });
    }
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        String endpointId,
                        ConnectionInfo connectionInfo) {

                    connectionsClient.acceptConnection(
                            endpointId,
                            payloadCallback
                    );
                }

                @Override
                public void onConnectionResult(
                        String endpointId,
                        ConnectionResolution result) {

                    if (result.getStatus().getStatusCode()
                            == ConnectionsStatusCodes.STATUS_OK) {

                        Toast.makeText(
                                activity,
                                "ResQNet device connected",
                                Toast.LENGTH_SHORT
                        ).show();

                        Payload payload =
                                Payload.fromBytes("HELLO FROM RESQNET".getBytes());

                        connectionsClient.sendPayload(endpointId, payload);
                    }else {

                        Toast.makeText(
                                activity,
                                "Connection failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {

                    connectedEndpoints.remove(endpointId);

                    Toast.makeText(
                            activity,
                            "ResQNet device disconnected",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            };
    public void requestNearbyPermissions() {

        if (android.os.Build.VERSION.SDK_INT >= 31) {

            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                    },
                    200
            );
        }
    }

    private void forwardSOS(SOSPacket packet, String receivedFromEndpoint) {

        Gson gson = new Gson();

        String json = gson.toJson(packet);

        Payload payload = Payload.fromBytes(
                json.getBytes()
        );

        for (String endpointId : connectedEndpoints) {

            // Don't immediately send it back to the phone
            // that sent it to us.
            if (endpointId.equals(receivedFromEndpoint)) {
                continue;
            }

            connectionsClient.sendPayload(
                    endpointId,
                    payload
            );
        }

        Toast.makeText(
                activity,
                "SOS forwarded. Hop: " + packet.getHopCount(),
                Toast.LENGTH_SHORT
        ).show();
    }
    public void sendSOS(SOSPacket packet) {

        Gson gson = new Gson();

        String json = gson.toJson(packet);

        Payload payload =
                Payload.fromBytes(json.getBytes());

        if (connectedEndpoints.isEmpty()) {

            Toast.makeText(
                    activity,
                    "No ResQNet devices connected",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        for (String endpointId : connectedEndpoints) {

            connectionsClient.sendPayload(
                    endpointId,
                    payload
            );
        }

        Toast.makeText(
                activity,
                "SOS sent to nearby devices",
                Toast.LENGTH_SHORT
        ).show();
    }
}