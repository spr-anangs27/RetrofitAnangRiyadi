package com.bmkg.retrofit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AreaAdapter extends RecyclerView.Adapter<AreaAdapter.ViewHolder> {

    // Helper class sederhana untuk menampung data tampilan
    public static class AreaItem {
        String name;
        String code; // Bisa null jika level Provinsi/Kab/Kec

        public AreaItem(String name, String code) {
            this.name = name;
            this.code = code;
        }
    }

    private final List<AreaItem> list;
    private final OnItemClick listener;

    public interface OnItemClick {
        void onClick(AreaItem item);
    }

    public AreaAdapter(List<AreaItem> list, OnItemClick listener) {
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_location, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AreaItem item = list.get(position);
        holder.tvName.setText(item.name);

        if (item.code != null && !item.code.isEmpty()) {
            holder.tvCode.setVisibility(View.VISIBLE);
            holder.tvCode.setText(item.code);
        } else {
            holder.tvCode.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCode;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvCode = itemView.findViewById(R.id.tvCode);
        }
    }
}