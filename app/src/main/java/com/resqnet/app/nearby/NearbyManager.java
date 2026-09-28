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
public class NearbyManager {

    private Activity activity;
    private ConnectionsClient connectionsClient;
    private static final String SERVICE_ID = "com.resqnet.app";
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
                public void onPayloadReceived(
                        String endpointId,
                        Payload payload) {

                    if (payload.getType() == Payload.Type.BYTES) {

                        String message =
                                new String(payload.asBytes());

                        Toast.makeText(
                                activity,
                                "Received: " + message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }

                @Override
                public void onPayloadTransferUpdate(
                        String endpointId,
                        PayloadTransferUpdate update) {

                    // We will handle transfer status here later
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
                                Payload.fromBytes(
                                        "HELLO FROM RESQNET".getBytes()
                                );

                        connectionsClient.sendPayload(
                                endpointId,
                                payload
                        );
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
}