package com.example.sensorrecorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private TextView tvState, tvLight, tvTemp;
    private SensorCollectService service;
    private boolean isBind = false;
    private Handler uiHandler;
    private static final long UI_REFRESH_DELAY = 300; // 300ms主动拉取刷新
    private final Runnable uiRefreshTask = new Runnable() {
        @Override
        public void run() {
            if (isBind && service.isCollecting()) {
                float light = service.getCurLight();
                float temp = service.getCurTemp();
                tvLight.setText("光线传感器：" + light + " lx");
                tvTemp.setText("温度传感器：" + temp + " ℃");
            }
            // 循环300ms刷新
            uiHandler.postDelayed(this, UI_REFRESH_DELAY);
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            SensorCollectService.MyBinder b = (SensorCollectService.MyBinder) binder;
            service = b.getService();
            service.setMainActivity(MainActivity.this);
            isBind = true;
            // 绑定成功后启动定时刷新
            uiHandler.post(uiRefreshTask);
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
        uiHandler = new Handler();
        tvState = findViewById(R.id.tv_state);
        tvLight = findViewById(R.id.tv_light);
        tvTemp = findViewById(R.id.tv_temp);

        Button btnStart = findViewById(R.id.btn_start);
        Button btnStop = findViewById(R.id.btn_stop);
        Button btnHistory = findViewById(R.id.btn_history);
        Button btnSetting = findViewById(R.id.btn_setting);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }

        bindService(new Intent(this, SensorCollectService.class), conn, BIND_AUTO_CREATE);

        btnStart.setOnClickListener(v -> {
            if (isBind) {
                service.startCollect();
                tvState.setText("采集状态：已开启");
            }
        });
        btnStop.setOnClickListener(v -> {
            if (isBind) {
                service.stopCollect();
                tvState.setText("采集状态：已停止");
            }
        });
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DataListActivity.class)));
        btnSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 页面暂停停止刷新，节省性能
        uiHandler.removeCallbacks(uiRefreshTask);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isBind) {
            uiHandler.post(uiRefreshTask);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHandler.removeCallbacks(uiRefreshTask);
        if (isBind) unbindService(conn);
    }
}