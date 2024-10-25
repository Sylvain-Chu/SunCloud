package com.example.suncloud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {

    private ArrayList<String> sensorList;

    public SensorAdapter(ArrayList<String> sensorList) {
        this.sensorList = sensorList;
    }

    @Override
    public SensorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_item_layout, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SensorViewHolder holder, int position) {
        String sensor = sensorList.get(position);
        holder.sensorName.setText(sensor);
    }

    @Override
    public int getItemCount() {
        return sensorList.size();
    }

    public class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView sensorName;

        public SensorViewHolder(View itemView) {
            super(itemView);
            sensorName = itemView.findViewById(R.id.sensorName);
        }
    }
}
