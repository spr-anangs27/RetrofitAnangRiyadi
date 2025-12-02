package com.bmkg.retrofit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bmkg.retrofit.model.Location;
import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {

    private List<Location> listOriginal; // Data asli
    private List<Location> listDisplay;  // Data yang ditampilkan (hasil filter)
    private final OnItemClick listener;

    public interface OnItemClick {
        void onClick(Location location);
    }

    public LocationAdapter(List<Location> list, OnItemClick listener) {
        this.listOriginal = new ArrayList<>(list);
        this.listDisplay = new ArrayList<>(list);
        this.listener = listener;
    }

    // FUNGSI SEARCH FILTER
    public void filterList(String query) {
        listDisplay.clear();
        if (query.isEmpty()) {
            listDisplay.addAll(listOriginal);
        } else {
            String q = query.toLowerCase();
            for (Location item : listOriginal) {
                if (item.getName().toLowerCase().contains(q)) {
                    listDisplay.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Location l = listDisplay.get(position);
        holder.tvName.setText(l.getName().toUpperCase());
        holder.itemView.setOnClickListener(v -> listener.onClick(l));
    }

    @Override
    public int getItemCount() {
        return listDisplay.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}