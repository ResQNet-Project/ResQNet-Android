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

    public SOSPacket(
            String senderId,
            double latitude,
            double longitude
    ) {
        this.messageId = UUID.randomUUID().toString();
        this.senderId = senderId;
        this.type = "SOS";

        this.latitude = latitude;
        this.longitude = longitude;

        this.timestamp = System.currentTimeMillis();

        this.hopCount = 0;
    }

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


    public void incrementHopCount() {
        hopCount++;
    }
}