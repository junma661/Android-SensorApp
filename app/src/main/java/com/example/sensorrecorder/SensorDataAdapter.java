package com.example.sensorrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class SensorDataAdapter extends ArrayAdapter<DBManager.SensorRecord> {
    private LayoutInflater inflater;

    public SensorDataAdapter(Context context, List<DBManager.SensorRecord> data) {
        super(context, 0, data);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_sensor_data, parent, false);
        }
        DBManager.SensorRecord item = getItem(pos);
        TextView tvTime = convertView.findViewById(R.id.tv_time);
        TextView tvType = convertView.findViewById(R.id.tv_type);
        TextView tvVal = convertView.findViewById(R.id.tv_value);
        tvTime.setText(item.time);
        tvType.setText(item.type);
        tvVal.setText(String.valueOf(item.value));
        return convertView;
    }
}