package org.horizontal.tella.mobile.presentation.entity;

import com.google.gson.annotations.SerializedName;

import java.util.List;


public class PublicMetadata {
    @SerializedName("timestamp")
    public long timestamp;

    @SerializedName("fileName")
    public String fileName;

    @SerializedName("filePath")
    public String filePath;

    @SerializedName("fileHash")
    public String fileHash;

    @SerializedName("cells")
    public List<String> cells;

    @SerializedName("wifis")
    public List<String> wifis;

    @SerializedName("ambientTemperature")
    public Float ambientTemperature;

    @SerializedName("light")
    public Float light;

    @SerializedName("location")
    public PublicLocation location;

    @SerializedName("locale")
    public String locale;

    @SerializedName("IPv6")
    public String IPv6;

    @SerializedName("IPv4")
    public String IPv4;

    @SerializedName("language")
    public String language;

    @SerializedName("networkType")
    public String networkType;

    @SerializedName("network")
    public String network;

    @SerializedName("manufacturer")
    public String manufacturer;

    @SerializedName("dataType")
    public String dataType;

    @SerializedName("hardware")
    public String hardware;

    @SerializedName("screenSize")
    public String screenSize;

    @SerializedName("wifiMac")
    public String wifiMac;

    @SerializedName("notes")
    public String notes;

    @SerializedName("deviceID")
    public String deviceID;

    public static class PublicLocation {
        @SerializedName("timestamp")
        public long timestamp;

        @SerializedName("latitude")
        public double latitude;

        @SerializedName("longitude")
        public double longitude;

        @SerializedName("altitude")
        public Double altitude;

        @SerializedName("accuracy")
        public Float accuracy;

        @SerializedName("provider")
        public String provider;

        @SerializedName("speed")
        public Float speed;
    }
}
