////package com.bmkg.retrofit.model;
////
////public class LokasiModel {
////    public String adm1;
////    public String adm2;
////    public String adm3;
////    public String adm4;
////    public String provinsi;
////    public String kotkab;
////    public String kecamatan;
////    public String desa;
////    public double lon;
////    public double lat;
////    public String timezone;
////}
//
//package com.bmkg.retrofit.model;
//
//public class LokasiModel {
//    public String id; // Kode wilayah (penting untuk Intent ke WeatherActivity)
//    public String provinsi;
//    public String kotkab;
//    public String kecamatan;
//    public String desa;
//
//    // Tambahan data lain jika perlu
//    public double lon;
//    public double lat;
//    public String timezone;
//}

package com.bmkg.retrofit.model;

import com.google.gson.annotations.SerializedName;

public class LokasiModel {
    // Pastikan ada ini!
    // @SerializedName("id") digunakan jika nama di JSON adalah "id"
    @SerializedName("id")
    public String id;

    public String provinsi;
    public String kotkab;
    public String kecamatan;
    public String desa;

    // ... field lainnya (lat, lon, timezone)
}
