package com.bmkg.retrofit;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bmkg.retrofit.model.DailyForecast;
import com.bmkg.retrofit.utils.SvgSoftwareLayerSetter;
import com.bumptech.glide.Glide;

import java.util.List;

public class DailyForecastAdapter extends RecyclerView.Adapter<DailyForecastAdapter.ViewHolder> {

    private final List<DailyForecast> list;
    private final Context context;

    public DailyForecastAdapter(List<DailyForecast> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Pastikan layout xml yang diload adalah item_daily_forecast yang baru
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_daily_forecast, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DailyForecast d = list.get(position);

        // 1. Set Hari (Senin, Selasa...)
        holder.tvDate.setText(d.getDateLabel());

        // 2. Set Suhu Terpisah (Max & Min) sesuai desain baru
        holder.tvMaxTemp.setText(d.getMaxTemp() + "°");
        holder.tvMinTemp.setText(d.getMinTemp() + "°");

        // 3. Logic Peluang Hujan
        if (d.getRainChance() > 0) {
            // Tampilkan persen saja biar rapi di layout baru
            holder.tvRainChance.setText(d.getRainChance() + "%");
            holder.tvRainChance.setVisibility(View.VISIBLE);
        } else {
            // Jika 0%, sembunyikan angkanya (atau bisa ganti text "-")
            holder.tvRainChance.setVisibility(View.GONE);
        }

        // 4. Load Icon SVG
        String iconUrl = d.getIconUrl().replace(" ", "%20");
        Glide.with(context)
                .as(PictureDrawable.class)
                .load(iconUrl)
                .into(new SvgSoftwareLayerSetter(holder.ivIconDay));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Deklarasi View sesuai ID di item_daily_forecast.xml yang baru
        TextView tvDate, tvMaxTemp, tvMinTemp, tvRainChance;
        ImageView ivIconDay;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Binding ID baru
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMaxTemp = itemView.findViewById(R.id.tvMaxTemp); // Suhu Putih Tebal
            tvMinTemp = itemView.findViewById(R.id.tvMinTemp); // Suhu Abu-abu
            tvRainChance = itemView.findViewById(R.id.tvRainChance);
            ivIconDay = itemView.findViewById(R.id.ivIconDay);
        }
    }
}