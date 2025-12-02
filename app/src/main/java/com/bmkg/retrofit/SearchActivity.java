package com.bmkg.retrofit;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bmkg.retrofit.api.WilayahApiService;
import com.bmkg.retrofit.model.Location;
import com.bmkg.retrofit.model.WilayahItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SearchActivity extends AppCompatActivity {

    TextView tvListTitle;
    EditText etSearch;
    RecyclerView rvLokasi;
    LocationAdapter locationAdapter;
    private WilayahApiService wilayahService;

    // Navigation State
    private String selectedProvId = "", selectedRegId = "", selectedDistId = "", selectedKecamatanName = "";
    private int currentLevel = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        tvListTitle = findViewById(R.id.tvListTitle);
        etSearch = findViewById(R.id.etSearch);
        rvLokasi = findViewById(R.id.rvLokasi);
        rvLokasi.setLayoutManager(new LinearLayoutManager(this));

        // Setup API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://ibnux.github.io/data-indonesia/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        wilayahService = retrofit.create(WilayahApiService.class);

        // Listener Filter Lokal
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (locationAdapter != null) locationAdapter.filterList(s.toString());
            }
        });

        // Listener Search Global (Enter)
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String query = etSearch.getText().toString();
                if (!query.isEmpty()) performGlobalSearch(query);
                return true;
            }
            return false;
        });

        // Load Provinsi
        loadProvinces();

        // Handle Back Button
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentLevel > 0) {
                    currentLevel--;
                    etSearch.setText("");
                    if (currentLevel == 0) loadProvinces();
                    else if (currentLevel == 1) loadRegencies(selectedProvId);
                    else if (currentLevel == 2) loadDistricts(selectedRegId);
                } else {
                    finish();
                }
            }
        });
    }

    // ==========================================
    // LOGIKA GLOBAL SEARCH (GEOCODER)
    // ==========================================
    private void performGlobalSearch(String query) {
        tvListTitle.setText("SEARCHING ONLINE...");
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);

        Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
        new Thread(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    if (addresses != null && !addresses.isEmpty()) {
                        Address addr = addresses.get(0);
                        String prov = addr.getAdminArea() != null ? addr.getAdminArea() : "";
                        String kab = addr.getSubAdminArea() != null ? addr.getSubAdminArea() : "";
                        String kec = addr.getLocality() != null ? addr.getLocality() : "";
                        String desa = addr.getSubLocality() != null ? addr.getSubLocality() : "";

                        if (kec.isEmpty() && !kab.isEmpty() && kab.toLowerCase().contains("kecamatan")) {
                            kec = kab; kab = "";
                        }

                        if(kab.isEmpty() && kec.isEmpty()) {
                            Toast.makeText(this, "Lokasi terlalu umum.", Toast.LENGTH_SHORT).show();
                            tvListTitle.setText("TRY SPECIFIC CITY");
                            return;
                        }

                        tvListTitle.setText("FOUND: " + (kec.isEmpty()?kab:kec).toUpperCase());
                        findProvinceId(prov, kab, kec, desa);

                    } else {
                        tvListTitle.setText("LOCATION NOT FOUND");
                    }
                });
            } catch (IOException e) {
                runOnUiThread(() -> tvListTitle.setText("CONNECTION ERROR"));
            }
        }).start();
    }

    // ==========================================
    // LOGIKA CHAINING PENCARIAN
    // ==========================================
    private void findProvinceId(String p, String k, String kc, String d) {
        wilayahService.getProvinces().enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) {
                    for(WilayahItem i : r.body()) if(isMatch(i.name, p)) { findRegency(i.id, k, kc, d); return; }
                    tvListTitle.setText("PROV NOT MATCH");
                }
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
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
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }

    private void checkDistrict(List<WilayahItem> list, int idx, String kc, String d) {
        if(idx>=list.size()) { tvListTitle.setText("DISTRICT NOT FOUND"); return; }
        if(idx%2==0) tvListTitle.setText("SCANNING: " + list.get(idx).name);

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

                    // LANGSUNG BUKA WEATHER ACTIVITY
                    Intent intent = new Intent(SearchActivity.this, WeatherActivity.class);
                    intent.putExtra("kode_wilayah", formatToBmkgCode(s.id));
                    intent.putExtra("nama_lokasi", s.name + ", " + kn);
                    startActivity(intent);
                    finish(); // Tutup search activity agar back langsung ke main
                } else tvListTitle.setText("VILLAGE NOT FOUND");
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }

    // ==========================================
    // LIST MANUAL
    // ==========================================
    private void loadProvinces() {
        currentLevel = 0; tvListTitle.setText("SELECT PROVINCE");
        wilayahService.getProvinces().enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) displayData(r.body(), i->{ selectedProvId=i.id; loadRegencies(i.id); });
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }
    private void loadRegencies(String id) {
        currentLevel = 1; tvListTitle.setText("SELECT CITY");
        wilayahService.getRegencies(id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) displayData(r.body(), i->{ selectedRegId=i.id; loadDistricts(i.id); });
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }
    private void loadDistricts(String id) {
        currentLevel = 2; tvListTitle.setText("SELECT DISTRICT");
        wilayahService.getDistricts(id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) displayData(r.body(), i->{ selectedDistId=i.id; selectedKecamatanName=i.name; loadVillagesList(i.id); });
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }
    private void loadVillagesList(String id) {
        currentLevel = 3; tvListTitle.setText("SELECT VILLAGE");
        wilayahService.getVillages(id).enqueue(new Callback<List<WilayahItem>>() {
            @Override public void onResponse(Call<List<WilayahItem>> c, Response<List<WilayahItem>> r) {
                if(r.isSuccessful() && r.body()!=null) displayData(r.body(), i->{
                    Intent intent = new Intent(SearchActivity.this, WeatherActivity.class);
                    intent.putExtra("kode_wilayah", formatToBmkgCode(i.id));
                    intent.putExtra("nama_lokasi", i.name + ", " + selectedKecamatanName);
                    startActivity(intent);
                });
            }
            @Override public void onFailure(Call<List<WilayahItem>> c, Throwable t) {}
        });
    }

    private void displayData(List<WilayahItem> data, OnItemClick listener) {
        Collections.sort(data, (o1, o2) -> o1.name.compareTo(o2.name));
        List<Location> l = new ArrayList<>();
        for(WilayahItem w : data) { Location loc = new Location(); loc.setName(w.name); l.add(loc); }
        locationAdapter = new LocationAdapter(l, loc -> {
            for(WilayahItem ori : data) if(ori.name.equals(loc.getName())) { listener.onClick(ori); break; }
        });
        rvLokasi.setAdapter(locationAdapter);
    }

    private boolean isMatch(String a, String b) {
        if(a==null||b==null)return false; String ca=cln(a), cb=cln(b);
        return ca.equals(cb) || ca.contains(cb) || cb.contains(ca) || (ca.contains("JAKARTA")&&cb.contains("JAKARTA"));
    }
    private String cln(String t) { return t.toUpperCase().replace("PROVINSI","").replace("DKI","").replace("KABUPATEN","").replace("KAB.","").replace("KOTA","").replace("KECAMATAN","").replace("KELURAHAN","").replace("DESA","").trim(); }
    private String formatToBmkgCode(String r) { if(r==null||r.length()<10)return r; return r.substring(0,2)+"."+r.substring(2,4)+"."+r.substring(4,6)+"."+r.substring(6); }
    interface OnItemClick { void onClick(WilayahItem item); }
}