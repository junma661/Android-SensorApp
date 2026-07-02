package com.example.sensorrecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvLight, tvTemp;
    private Button btnStart, btnStop, btnHistory, btnSetting;
    private DBManager dbManager;
    private SensorCollectService collectService;
    private boolean isBind = false;
    private BatteryReceiver batteryReceiver;
    private Thread refreshThread;
    // 新增标记：页面前台存活
    private boolean isActive = false;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorCollectService.MyBinder binder = (SensorCollectService.MyBinder) service;
            collectService = binder.getService();
            isBind = true;
            // 绑定成功立刻启动刷新
            if(isActive) startRefresh();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        dbManager = new DBManager(this);
        bindService(new Intent(this, SensorCollectService.class), conn, Context.BIND_AUTO_CREATE);

        // 动态注册电量广播接收器
        batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // ========== 新增：申请通知权限（Android13+ 用于光线超限告警通知） ==========
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100);
        }
    }

    private void startRefresh() {
        if (refreshThread != null && refreshThread.isAlive()) return;
        refreshThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted() && isBind && collectService != null && isActive) {
                runOnUiThread(() -> {
                    float light = collectService.getCurLight();
                    float temp = collectService.getCurTemp();
                    tvLight.setText("光线传感器：" + light + " lx");
                    tvTemp.setText("温度传感器：" + temp + " ℃");
                });
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.start();
    }

    private void bindView() {
        tvStatus = findViewById(R.id.tv_status);
        tvLight = findViewById(R.id.tv_light);
        tvTemp = findViewById(R.id.tv_temp);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history);
        btnSetting = findViewById(R.id.btn_setting);

        btnStart.setOnClickListener(v -> {
            if (isBind && collectService != null) {
                collectService.startCollect();
                tvStatus.setText("采集状态：后台服务正在采集");
            }
        });
        btnStop.setOnClickListener(v -> {
            if (isBind && collectService != null) {
                collectService.stopCollect();
                tvStatus.setText("采集状态：未开启");
            }
        });
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DataListActivity.class)));
        btnSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActive = true;
        startRefresh();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActive = false;
        if (refreshThread != null) {
            refreshThread.interrupt();
            refreshThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
        if (refreshThread != null) refreshThread.interrupt();
        if (isBind) unbindService(conn);
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        dbManager.close();
    }
}