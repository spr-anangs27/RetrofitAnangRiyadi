package com.bmkg.retrofit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bmkg.retrofit.api.ApiClient;
import com.bmkg.retrofit.api.ApiService;
import com.bmkg.retrofit.model.CuacaItem;
import com.bmkg.retrofit.model.DailyForecast;
import com.bmkg.retrofit.model.DataItem;
import com.bmkg.retrofit.model.ResponseData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherActivity extends AppCompatActivity {

    // Header & Hero Views
    TextView tvLokasi, tvSuhu, tvFeelsLike, tvCuaca, tvLastUpdate;
    ImageView ivMainIcon, btnBack;

    // Grid Views (8 Grid)
    TextView tvDetailMinMax, tvDetailHumid, tvDetailWind, tvDetailWd, tvDetailTp, tvDetailVs, tvDetailRainChance;

    // Lists
    RecyclerView rvJam, rvDaily;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // --- 1. Bind Views ---
        tvLokasi = findViewById(R.id.tvLokasi);
        tvSuhu = findViewById(R.id.tvSuhu);
        tvFeelsLike = findViewById(R.id.tvFeelsLike);
        tvCuaca = findViewById(R.id.tvCuaca);
        tvLastUpdate = findViewById(R.id.tvLastUpdate);
        ivMainIcon = findViewById(R.id.ivMainIcon);
        btnBack = findViewById(R.id.btnBack);

        // Bind Grid Details
        tvDetailMinMax = findViewById(R.id.tvDetailMinMax);
        tvDetailHumid = findViewById(R.id.tvDetailHumid);
        tvDetailWind = findViewById(R.id.tvDetailWind);
        tvDetailWd = findViewById(R.id.tvDetailWd); // Baru
        tvDetailTp = findViewById(R.id.tvDetailTp); // Baru
        tvDetailVs = findViewById(R.id.tvDetailVs); // Baru
        tvDetailRainChance = findViewById(R.id.tvDetailRainChance); // Baru

        // Bind Recyclers
        rvJam = findViewById(R.id.rvJam);
        rvJam.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        rvDaily = findViewById(R.id.rvDaily);
        rvDaily.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        // --- 2. Action ---
        btnBack.setOnClickListener(v -> finish()); // Fungsi tombol Back

        // --- 3. Get Intent Data ---
        String kodeAdm4 = getIntent().getStringExtra("kode_wilayah");
        String namaLokasi = getIntent().getStringExtra("nama_lokasi");

        if (namaLokasi != null) {
            // Ambil nama kota saja
            String shortName = namaLokasi.contains(",") ? namaLokasi.split(",")[0] : namaLokasi;
            tvLokasi.setText(shortName.toUpperCase());
        }

        if (kodeAdm4 != null && !kodeAdm4.isEmpty()) {
            loadWeather(kodeAdm4);
        } else {
            Toast.makeText(this, "Kode Wilayah Error", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadWeather(String adm4Code) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.getWeatherByAdm4(adm4Code).enqueue(new Callback<ResponseData>() {
            @Override
            public void onResponse(Call<ResponseData> call, Response<ResponseData> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().data.isEmpty()) {
                    Toast.makeText(WeatherActivity.this, "Data tidak ditemukan", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    DataItem item = response.body().data.get(0);

                    // Flatten semua jam
                    List<CuacaItem> allHours = new ArrayList<>();
                    for (List<CuacaItem> day : item.cuaca) allHours.addAll(day);

                    // Cari jam sekarang
                    int currentIndex = findCurrentHourIndex(allHours);
                    CuacaItem current = allHours.get(currentIndex);

                    // === UPDATE HERO SECTION ===
                    tvSuhu.setText(current.t + "°");

                    // Simulasi Feels Like
                    int feelsLike = current.t + 2;
                    tvFeelsLike.setText("FEELS LIKE " + feelsLike + "°");

                    tvCuaca.setText(current.weather_desc.toUpperCase());

                    // Set Icon
                    updateMainIcon(current.weather_desc);

                    // Update Waktu
                    try {
                        SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", new Locale("id", "ID"));
                        SimpleDateFormat outFmt = new SimpleDateFormat("HH:mm", new Locale("id", "ID"));
                        Date dateObj = inFmt.parse(current.local_datetime);
                        tvLastUpdate.setText("UPDATED " + outFmt.format(dateObj));
                    } catch (Exception e) {
                        tvLastUpdate.setText("UPDATED --:--");
                    }

                    // === UPDATE GRID 8 LINGKARAN (DATA LENGKAP) ===

                    // 1. Temp Min/Max (Hitung dari hari ini)
                    int maxT = -100, minT = 100;
                    for(CuacaItem c : item.cuaca.get(0)) {
                        if(c.t > maxT) maxT = c.t;
                        if(c.t < minT) minT = c.t;
                    }
                    tvDetailMinMax.setText(maxT + "°/" + minT + "°");

                    // 2. Humidity
                    tvDetailHumid.setText(current.hu);

                    // 3. Wind Speed
                    tvDetailWind.setText(current.ws);

                    // 4. Wind Direction
                    tvDetailWd.setText(current.wd != null ? current.wd.toUpperCase() : "N/A");

                    // 5. Precipitation (TP)
                    tvDetailTp.setText(String.valueOf(current.tp));

                    // 6. Visibility (VS)
                    tvDetailVs.setText(current.vs != null ? current.vs.toUpperCase() : "-");

                    // 7. Rain Chance (Logic simulasi)
                    String rainChance = "0";
                    if(current.weather_desc.toLowerCase().contains("hujan")) rainChance = "85";
                    else if(current.weather_desc.toLowerCase().contains("berawan")) rainChance = "45";
                    tvDetailRainChance.setText(rainChance);

                    // (8. UV sudah hardcoded "LOW" di XML)


                    // === UPDATE LIST FORECAST ===

                    // Hourly
                    rvJam.setAdapter(new WeatherHourAdapter(allHours, WeatherActivity.this));
                    rvJam.scrollToPosition(currentIndex);

                    // Daily (7 Days)
                    List<DailyForecast> dailyList = new ArrayList<>();
                    int maxHari = Math.min(item.cuaca.size(), 7);

                    for (int d = 0; d < maxHari; d++) {
                        List<CuacaItem> hariList = item.cuaca.get(d);
                        if (hariList.isEmpty()) continue;

                        int dMax = -100, dMin = 100;
                        double totalTp = 0;
                        for (CuacaItem c : hariList) {
                            if (c.t > dMax) dMax = c.t;
                            if (c.t < dMin) dMin = c.t;
                            totalTp += c.tp;
                        }
                        int chance = (totalTp > 0) ? 60 : 0;
                        CuacaItem rep = hariList.get(hariList.size() / 2);

                        String dayName = "DAY";
                        try {
                            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                            SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.ENGLISH);
                            Date dt = inFmt.parse(rep.local_datetime);
                            dayName = dayFmt.format(dt).toUpperCase();
                            if (d == 0) dayName = "TODAY";
                        } catch (Exception e) {}

                        dailyList.add(new DailyForecast(dayName, dMax, dMin, rep.weather_desc, rep.image, chance));
                    }
                    rvDaily.setAdapter(new DailyForecastAdapter(dailyList, WeatherActivity.this));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseData> call, Throwable t) {
                Toast.makeText(WeatherActivity.this, "Koneksi Gagal", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMainIcon(String desc) {
        if (desc == null) return;

        String lowerDesc = desc.toLowerCase();

        // Reset gambar dulu
        ivMainIcon.setImageResource(0);

        if (lowerDesc.contains("hujan petir") || lowerDesc.contains("petir")) {
            ivMainIcon.setImageResource(R.drawable.ic_thunder);
        }
        else if (lowerDesc.contains("hujan")) {
            ivMainIcon.setImageResource(R.drawable.ic_rainy);
        }
        else if (lowerDesc.contains("cerah") || lowerDesc.contains("terang")) {
            ivMainIcon.setImageResource(R.drawable.ic_sunny);
        }
        else if (lowerDesc.contains("berawan") || lowerDesc.contains("mendung")) {
            ivMainIcon.setImageResource(R.drawable.ic_cloudy);
        }
        else {
            // Default jika cuaca tidak dikenali
            ivMainIcon.setImageResource(R.drawable.ic_cloudy);
        }

        // Pastikan icon berwarna PUTIH (Sesuai tema Nothing OS)
        ivMainIcon.setColorFilter(ContextCompat.getColor(this, R.color.nothing_white));
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
}