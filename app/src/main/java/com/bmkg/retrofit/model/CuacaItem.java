package com.bmkg.retrofit.model;

public class CuacaItem {
    public String local_datetime;
    public int t;            // Suhu

    // --- DATA WAJIB BARU ---
    public String hu;        // Humidity (%)
    public String ws;        // Wind Speed (km/h)
    public String wd;        // Wind Direction (N, SE, dll) -> BARU
    public double tp;        // Precipitation (mm) -> BARU
    public String vs;        // Visibility (km/m) -> BARU
    // -----------------------

    public String weather_desc;
    public String image;

    public String getHour() {
        if (local_datetime != null && local_datetime.length() >= 16) {
            return local_datetime.substring(11, 16);
        }
        return "-";
    }
}