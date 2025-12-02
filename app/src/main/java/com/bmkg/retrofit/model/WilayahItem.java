package com.bmkg.retrofit.model;

import com.google.gson.annotations.SerializedName;

public class WilayahItem {
    public String id;

    @SerializedName("nama") // Sesuaikan dengan key JSON IbnuX
    public String name;

    @Override
    public String toString() {
        return name;
    }
}