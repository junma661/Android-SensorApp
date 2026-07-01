package com.example.sensorrecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {
    private static final int LOW_BATTERY = 20;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // 记录上一次电量，用来判断电量是上升还是下降
    private int lastBatteryPercent = 100;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) return;

        int level = intent.getIntExtra("level", 0);
        int scale = intent.getIntExtra("scale", 100);
        int currentPercent = level * 100 / scale;

        // 两个条件同时满足才弹窗：
        // 1. 当前电量≤20%
        // 2. 当前电量 < 上一次电量 = 电量在下降
        if (currentPercent <= LOW_BATTERY && currentPercent < lastBatteryPercent) {
            mainHandler.post(() -> Toast.makeText(context, "警告：电量过低，建议停止传感器采集", Toast.LENGTH_SHORT).show());
        }

        // 更新上次电量记录
        lastBatteryPercent = currentPercent;
    }
}