package com.bmkg.retrofit.api;

import com.bmkg.retrofit.model.WilayahItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface WilayahApiService {
    // Ambil Provinsi
    @GET("propinsi.json")
    Call<List<WilayahItem>> getProvinces();

    // Ambil Kabupaten (parameter ID Provinsi)
    @GET("kabupaten/{id}.json")
    Call<List<WilayahItem>> getRegencies(@Path("id") String provId);

    // Ambil Kecamatan (parameter ID Kabupaten)
    @GET("kecamatan/{id}.json")
    Call<List<WilayahItem>> getDistricts(@Path("id") String regId);

    // Ambil Desa/Kelurahan (parameter ID Kecamatan)
    @GET("kelurahan/{id}.json")
    Call<List<WilayahItem>> getVillages(@Path("id") String distId);
}