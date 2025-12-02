//package com.bmkg.retrofit;
//
//import android.content.Context;
//import android.graphics.drawable.PictureDrawable;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bmkg.retrofit.model.CuacaItem;
//import com.bmkg.retrofit.utils.SvgSoftwareLayerSetter;
//import com.bumptech.glide.Glide;
//
//import java.util.List;
//
//public class WeatherHourAdapter extends RecyclerView.Adapter<WeatherHourAdapter.ViewHolder> {
//
//    private List<CuacaItem> list;
//    private Context context;
//
//    public WeatherHourAdapter(List<CuacaItem> list, Context context) {
//        this.list = list;
//        this.context = context;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View v = LayoutInflater.from(context)
//                .inflate(R.layout.item_weather_hour, parent, false);
//        return new ViewHolder(v);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int pos) {
//        CuacaItem c = list.get(pos);
//
//        holder.jam.setText(c.getHour());
//        holder.suhu.setText(c.t + "°");
//        holder.desc.setText(c.weather_desc);
//
//        String iconUrl = c.image.replace(" ", "%20");
//
//        Glide.with(context)
//                .as(PictureDrawable.class)
//                .load(iconUrl)
//                .into(new SvgSoftwareLayerSetter(holder.icon));
//
//    }
//
//    @Override
//    public int getItemCount() {
//        return list.size();
//    }
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        TextView jam, suhu, desc;
//        ImageView icon;
//        ViewHolder(View v) {
//            super(v);
//            jam = v.findViewById(R.id.tvJam);
//            suhu = v.findViewById(R.id.tvSuhu);
//            desc = v.findViewById(R.id.tvDesc);
//            icon = v.findViewById(R.id.ivIcon);
//        }
//    }
//}

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

import com.bmkg.retrofit.model.CuacaItem;
import com.bmkg.retrofit.utils.SvgSoftwareLayerSetter; // Class Utils SVG Anda
import com.bumptech.glide.Glide;

import java.util.List;

public class WeatherHourAdapter extends RecyclerView.Adapter<WeatherHourAdapter.ViewHolder> {

    private final List<CuacaItem> list;
    private final Context context;

    public WeatherHourAdapter(List<CuacaItem> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_weather_hour, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CuacaItem item = list.get(position);

        // Format Jam: Ambil 5 karakter dari jam (index 11 s/d 16)
        // Contoh "2025-11-26 14:00:00" -> "14:00"
        try {
            String hourOnly = item.local_datetime.substring(11, 16);
            holder.tvJam.setText(hourOnly);
        } catch (Exception e) {
            holder.tvJam.setText("-");
        }

        holder.tvSuhu.setText(item.t + "°");
        holder.tvDesc.setText(item.weather_desc);

        // Load Icon
        String iconUrl = item.image.replace(" ", "%20");
        Glide.with(context)
                .as(PictureDrawable.class)
                .load(iconUrl)
                .into(new SvgSoftwareLayerSetter(holder.ivIcon));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvJam, tvSuhu, tvDesc;
        ImageView ivIcon;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJam = itemView.findViewById(R.id.tvJam);
            tvSuhu = itemView.findViewById(R.id.tvSuhu);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            ivIcon = itemView.findViewById(R.id.ivIcon);
        }
    }
}
