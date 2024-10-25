package com.example.suncloud;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class SensorAdapter extends RecyclerView.Adapter<SensorAdapter.SensorViewHolder> {

    private ArrayList<String> sensorList;
    private ArrayList<String> sensorValues;
    private ArrayList<String> sensorUnits;

    public SensorAdapter(ArrayList<String> sensorList) {
        this.sensorList = sensorList;
        // Initialiser les valeurs des capteurs avec des valeurs par défaut
        sensorValues = new ArrayList<>();
        sensorUnits = new ArrayList<>();
        for (String sensor : sensorList) {
            sensorValues.add(""); // Valeur vide au départ
            sensorUnits.add(getUnitForSensor(sensor));
        }
    }

    private String getUnitForSensor(String sensor) {
        switch (sensor.toLowerCase()) {
            case "température":
                return "°C";
            case "humidité":
                return "%";
            case "pression":
                return "hPa";
            case "luminosité":
                return "lx";
            case "uv":
                return "uva";
            case "infrarouge":
                return "mm";
            default:
                return "";
        }
    }

    @Override
    public SensorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_item_layout, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SensorViewHolder holder, int position) {
        String sensor = sensorList.get(position);
        String value = sensorValues.get(position);
        String unit = sensorUnits.get(position);
        holder.sensorName.setText(sensor);
        holder.sensorValue.setText(value + " " + unit);
    }

    @Override
    public int getItemCount() {
        return sensorList.size();
    }

    public void updateSensorValues(ArrayList<String> newValues) {
        this.sensorValues = newValues;
        notifyDataSetChanged();
    }

    public ArrayList<String> getSensorList() {
        return sensorList;
    }

    public ArrayList<String> getSensorValues() {
        return sensorValues;
    }

    public ArrayList<String> getSensorUnits() {
        return sensorUnits;
    }

    public class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView sensorName;
        TextView sensorValue;

        public SensorViewHolder(View itemView) {
            super(itemView);
            sensorName = itemView.findViewById(R.id.sensorName);
            sensorValue = itemView.findViewById(R.id.sensorValue);
        }
    }
}
