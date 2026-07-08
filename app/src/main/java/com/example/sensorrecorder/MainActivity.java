package com.example.sensorrecorder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.sensorrecorder.DataListActivity;


public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvLight, tvTemp;
    private Button btnStart, btnStop, btnHistory, btnSetting;
    private DBManager dbManager;
    private SensorCollectService collectService;
    private boolean isBind = false;
    private BatteryReceiver batteryReceiver;
    private Thread refreshThread;
    // 页面前台存活标记，控制刷新线程
    private boolean isActive = false;

    // 服务连接回调
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SensorCollectService.MyBinder binder = (SensorCollectService.MyBinder) service;
            collectService = binder.getService();
            isBind = true;
            // 绑定成功且页面活跃，启动实时刷新
            if (isActive) startRefresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
            collectService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件、数据库、绑定服务
        bindView();
        dbManager = new DBManager(this);
        bindService(new Intent(this, SensorCollectService.class), conn, Context.BIND_AUTO_CREATE);

        // 动态注册电量广播接收器
        batteryReceiver = new BatteryReceiver();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(batteryReceiver, filter);

        // Android13+ 申请通知权限（用于光线超限告警、导出通知）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    100);
        }

        // 读取配置，自动恢复后台采集状态
        SharedPreferences sp = getSharedPreferences(SettingActivity.SP_NAME, MODE_PRIVATE);
        boolean autoStartCollect = sp.getBoolean(SettingActivity.KEY_BG_ENABLE, false);
        if (autoStartCollect) {
            Intent autoServiceIntent = new Intent(this, SensorCollectService.class);
            startService(autoServiceIntent);
        }
    }

    /** 启动实时刷新线程，更新传感器数值 */
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

    /** 绑定控件与点击事件 */
    private void bindView() {
        tvStatus = findViewById(R.id.tv_status);
        tvLight = findViewById(R.id.tv_light);
        tvTemp = findViewById(R.id.tv_temp);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history);
        btnSetting = findViewById(R.id.btn_setting);

        // 开始采集
        btnStart.setOnClickListener(v -> {
            if (isBind && collectService != null) {
                collectService.startCollect();
                tvStatus.setText("采集状态：后台服务正在采集");
            }
        });

        // 停止采集
        btnStop.setOnClickListener(v -> {
            if (isBind && collectService != null) {
                collectService.stopCollect();
                tvStatus.setText("采集状态：未开启");
            }
        });

        // 跳转历史数据页面
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, DataListActivity.class)));

        // 跳转设置页面
        btnSetting.setOnClickListener(v -> startActivity(new Intent(this, SettingActivity.class)));
    }

    // 权限申请回调，补全通知权限处理
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 通知权限申请成功
            }
        }
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
        // 页面退后台，中断刷新线程
        if (refreshThread != null) {
            refreshThread.interrupt();
            refreshThread = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActive = false;
        // 销毁时释放资源
        if (refreshThread != null) refreshThread.interrupt();
        if (isBind) unbindService(conn);
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        if (dbManager != null) dbManager.close();
    }
}