package com.bmkg.retrofit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bmkg.retrofit.api.ApiClient;
import com.bmkg.retrofit.api.ApiService;
import com.bmkg.retrofit.api.WilayahApiService;
import com.bmkg.retrofit.model.CuacaItem;
import com.bmkg.retrofit.model.DataItem;
import com.bmkg.retrofit.model.ResponseData;
import com.bmkg.retrofit.model.WilayahItem;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    // Views
    TextView tvMainLocation, tvMainTemp, tvMainCondition;
    TextView tvMainMinMax, tvGridHumid, tvGridWind, tvGridWd, tvGridTp, tvGridVs, tvGridRainChance;
    ImageView btnRefresh, btnSearch;

    private WilayahApiService wilayahService;
    private FusedLocationProviderClient fusedLocationClient;

    private String gpsBmkgId = null;
    private String gpsFullLocationName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Bind Views
        tvMainLocation = findViewById(R.id.tvMainLocation);
        tvMainTemp = findViewById(R.id.tvMainTemp);
        tvMainCondition = findViewById(R.id.tvMainCondition);

        tvMainMinMax = findViewById(R.id.tvMainMinMax);
        tvGridHumid = findViewById(R.id.tvGridHumid);
        tvGridWind = findViewById(R.id.tvGridWind);
        tvGridWd = findViewById(R.id.tvGridWd);
        tvGridTp = findViewById(R.id.tvGridTp);
        tvGridVs = findViewById(R.id.tvGridVs);
        tvGridRainChance = findViewById(R.id.tvGridRainChance);

        btnRefresh = findViewById(R.id.btnRefresh);
        btnSearch = findViewById(R.id.btnSearch);

        // 2. Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ibnux.github.io/data-indonesia/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        wilayahService = retrofit.create(WilayahApiService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 3. Setup Listeners

        // REFRESH: Perbarui GPS & Data
        btnRefresh.setOnClickListener(v -> {
            updateHeroStatus("REFRESHING...");
            checkLocationPermission();
        });

        // SEARCH: Buka Activity Pencarian
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        // DETAIL: Klik suhu untuk buka detail 5 hari
        tvMainTemp.setOnClickListener(v -> openDetailActivity());

        // 4. Start
        checkLocationPermission();
    }

    private void openDetailActivity() {
        if (gpsBmkgId != null && gpsFullLocationName != null) {
            Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
            intent.putExtra("kode_wilayah", gpsBmkgId);
            intent.putExtra("nama_lokasi", gpsFullLocationName);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Data belum siap", Toast.LENGTH_SHORT).show();
        }
    }

    // --- GPS LOGIC ---
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            getCurrentLocation();
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) getCurrentLocation();
                else updateHeroStatus("GPS DENIED");
            });

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) processCoordinates(location.getLatitude(), location.getLongitude());
                else updateHeroStatus("GPS LOST");
            }).addOnFailureListener(e -> updateHeroStatus("GPS ERROR"));
        }
    }

    private void processCoordinates(double lat, double lon) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String provName = addr.getAdminArea() != null ? addr.getAdminArea() : "";
                String kabName = addr.getSubAdminArea() != null ? addr.getSubAdminArea() : "";
                String kecName = addr.getLocality() != null ? addr.getLocality() : "";
                String desaName = addr.getSubLocality() != null ? addr.getSubLocality() : "";

                if (kecName.isEmpty() && !kabName.isEmpty() && kabName.toLowerCase().contains("kecamatan")) {
                    kecName = kabName; kabName = "";
                }

                if (provName.isEmpty()) { updateHeroStatus("UNKNOWN LOC"); return; }
                updateHeroStatus("SEARCHING: " + kecName.toUpperCase());

                // Cari ID berdasarkan nama lokasi (Chaining API)
                findProvinceId(provName, kabName, kecName, desaName);
            } else {
                updateHeroStatus("NOT FOUND");
            }
        } catch (IOException e) { updateHeroStatus("OFFLINE"); }
    }

    private void updateHeroStatus(String msg) {
        tvMainLocation.setText(msg);
        if (msg.contains("ERROR") || msg.contains("DENIED") || msg.contains("LOST")) {
            tvMainTemp.setText("--");
            tvMainCondition.setText("NO DATA");
        }
    }

    // --- API CHAINING UTAMA ---
    private void findProvinceId(String p, String k, String kc, String d) {
        wilayahService.getProvinces().enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) {
                    for(WilayahItem i : r.body()) if(isMatch(i.name, p)) { findRegency(i.id, k, kc, d); return; }
                    updateHeroStatus("PROV ERR");
                }
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) { updateHeroStatus("NET ERR"); }
        });
    }

    private void findRegency(String id, String k, String kc, String d) {
        wilayahService.getRegencies(id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) {
                    List<WilayahItem> list = new ArrayList<>();
                    for(WilayahItem i : r.body()) if(isMatch(i.name, k)) list.add(i);
                    if(list.isEmpty()) checkDistrict(r.body(), 0, kc, d); else checkDistrict(list, 0, kc, d);
                }
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) { updateHeroStatus("NET ERR"); }
        });
    }

    private void checkDistrict(List<WilayahItem> list, int idx, String kc, String d) {
        if(idx>=list.size()) { updateHeroStatus("DIST ERR"); return; }
        if(idx % 2 == 0) updateHeroStatus("SCANNING: " + list.get(idx).name);
        wilayahService.getDistricts(list.get(idx).id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                boolean f = false;
                if(r.isSuccessful() && r.body()!=null) {
                    for(WilayahItem i : r.body()) if(isMatch(i.name, kc)) {
                        f=true; findVillage(i.id, d, i.name); return;
                    }
                }
                if(!f) checkDistrict(list, idx+1, kc, d);
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) { checkDistrict(list, idx+1, kc, d); }
        });
    }

    private void findVillage(String id, String d, String kn) {
        wilayahService.getVillages(id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null && !r.body().isEmpty()) {
                    WilayahItem s = r.body().get(0);
                    if(!d.isEmpty()) for(WilayahItem i : r.body()) if(isMatch(i.name, d)) { s=i; break; }
                    // ID Ketemu -> Ambil Cuaca
                    fetchGpsWeather(formatToBmkgCode(s.id), s.name + ", " + kn);
                } else updateHeroStatus("VIL ERR");
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) { updateHeroStatus("NET ERR"); }
        });
    }

    // --- FETCH WEATHER (ISI 8 GRID) ---
    private void fetchGpsWeather(String idBmkg, String locationName) {
        this.gpsBmkgId = idBmkg;
        this.gpsFullLocationName = locationName;
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getWeatherByAdm4(idBmkg).enqueue(new Callback<ResponseData>() {
            @Override
            public void onResponse(Call<ResponseData> call, Response<ResponseData> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().data.isEmpty()) {
                    DataItem item = response.body().data.get(0);
                    List<CuacaItem> allHours = new ArrayList<>();
                    for (List<CuacaItem> day : item.cuaca) allHours.addAll(day);

                    int index = findCurrentHourIndex(allHours);
                    CuacaItem current = allHours.get(index);

                    // Update UI Utama
                    tvMainLocation.setText(locationName.toUpperCase());
                    tvMainTemp.setText(current.t + "°");
                    tvMainCondition.setText(current.weather_desc.toUpperCase());

                    // Update Grid 8 Lingkaran (DATA LENGKAP)
                    int maxT = -100, minT = 100;
                    for(CuacaItem c : item.cuaca.get(0)) { if(c.t > maxT) maxT = c.t; if(c.t < minT) minT = c.t; }
                    tvMainMinMax.setText(maxT + "°/" + minT + "°");

                    tvGridHumid.setText(current.hu);
                    tvGridWind.setText(current.ws);

                    // Data baru (WD, TP, VS)
                    tvGridWd.setText(current.wd != null ? current.wd.toUpperCase() : "N/A");
                    tvGridTp.setText(String.valueOf(current.tp));
                    tvGridVs.setText(current.vs != null ? current.vs.toUpperCase() : "-");

                    // Rain Chance Simulation
                    if(current.weather_desc.toLowerCase().contains("hujan")) tvGridRainChance.setText("80");
                    else if(current.weather_desc.toLowerCase().contains("berawan")) tvGridRainChance.setText("30");
                    else tvGridRainChance.setText("0");

                } else updateHeroStatus("EMPTY DATA");
            }
            @Override public void onFailure(Call<ResponseData> call, Throwable t) { updateHeroStatus("API ERR"); }
        });
    }

    private int findCurrentHourIndex(List<CuacaItem> list) {
        int selectedIndex = 0;
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (int i = 0; i < list.size(); i++) {
            try {
                Date d = sdf.parse(list.get(i).local_datetime);
                if (d != null && (d.before(now) || d.equals(now))) selectedIndex = i; else break;
            } catch (Exception e) {}
        }
        return selectedIndex;
    }

    // Helper
    private boolean isMatch(String a, String b) {
        if(a==null||b==null)return false; String ca=cln(a), cb=cln(b);
        return ca.equals(cb) || ca.contains(cb) || cb.contains(ca) || (ca.contains("JAKARTA")&&cb.contains("JAKARTA"));
    }
    private String cln(String t) { return t.toUpperCase().replace("PROVINSI","").replace("DKI","").replace("KABUPATEN","").replace("KAB.","").replace("KOTA","").replace("KECAMATAN","").replace("KELURAHAN","").replace("DESA","").trim(); }
    private String formatToBmkgCode(String r) { if(r==null||r.length()<10)return r; return r.substring(0,2)+"."+r.substring(2,4)+"."+r.substring(4,6)+"."+r.substring(6); }
}