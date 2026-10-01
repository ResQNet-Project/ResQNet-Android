package com.resqnet.app;

import java.util.UUID;

public class SOSPacket {

    private String messageId;
    private String senderId;
    private String type;

    private double latitude;
    private double longitude;

    private long timestamp;

    private int hopCount;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SOSPacket(
            String senderId,
            double latitude,
            double longitude
    ) {

        // Unique ID for this SOS
        this.messageId =
                UUID.randomUUID().toString();

        // Device that originally created the SOS
        this.senderId =
                senderId;

        // Message type
        this.type =
                "SOS";

        // GPS location
        this.latitude =
                latitude;

        this.longitude =
                longitude;

        // Creation time
        this.timestamp =
                System.currentTimeMillis();

        // Original message starts at hop 0
        this.hopCount =
                0;
    }


    // =========================================================
    // GETTERS
    // =========================================================

    public String getMessageId() {

        return messageId;
    }


    public String getSenderId() {

        return senderId;
    }


    public String getType() {

        return type;
    }


    public double getLatitude() {

        return latitude;
    }


    public double getLongitude() {

        return longitude;
    }


    public long getTimestamp() {

        return timestamp;
    }


    public int getHopCount() {

        return hopCount;
    }


    // =========================================================
    // INCREASE HOP COUNT
    // =========================================================

    public void incrementHopCount() {

        hopCount++;
    }
}