package com.example.sensorrecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

public class SensorDataAdapter extends ArrayAdapter<SensorData> {
    private List<SensorData> dataList;
    private Context context;

    public SensorDataAdapter(Context context, List<SensorData> list) {
        super(context, 0, list);
        this.context = context;
        this.dataList = list;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_sensor_data, parent, false);
            holder = new ViewHolder();
            holder.tvTime = convertView.findViewById(R.id.tv_time);
            holder.tvType = convertView.findViewById(R.id.tv_type);
            holder.tvValue = convertView.findViewById(R.id.tv_value);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        SensorData data = dataList.get(position);
        holder.tvTime.setText(data.getTimestamp());
        holder.tvType.setText(data.getSensorType() + "：");
        holder.tvValue.setText(String.valueOf(data.getValue()));
        return convertView;
    }

    static class ViewHolder {
        TextView tvTime, tvType, tvValue;
    }
}