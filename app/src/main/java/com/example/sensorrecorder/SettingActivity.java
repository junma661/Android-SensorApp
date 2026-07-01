package com.example.sensorrecorder;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {
    private EditText etInterval, etThreshold;
    private CheckBox cbSaveDb;
    private SensorCollectService service;
    private boolean isBind = false;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            SensorCollectService.MyBinder b = (SensorCollectService.MyBinder) binder;
            service = b.getService();
            isBind = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        etInterval = findViewById(R.id.et_interval);
        etThreshold = findViewById(R.id.et_threshold);
        cbSaveDb = findViewById(R.id.cb_save);
        Button btnSave = findViewById(R.id.btn_save);
        Button btnBack = findViewById(R.id.btn_back);

        bindService(new Intent(this, SensorCollectService.class), conn, BIND_AUTO_CREATE);

        btnSave.setOnClickListener(v -> {
            String intervalText = etInterval.getText().toString().trim();
            String thresholdText = etThreshold.getText().toString().trim();
            boolean allowSave = cbSaveDb.isChecked();
            long interval;
            int threshold;
            try {
                interval = Long.parseLong(intervalText);
                threshold = Integer.parseInt(thresholdText);
                if (interval < 200) {
                    Toast.makeText(SettingActivity.this, "采样间隔不能小于200毫秒", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (threshold <= 0) {
                    Toast.makeText(SettingActivity.this, "阈值必须大于0", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (isBind) {
                    service.setConfig(interval, threshold, allowSave);
                    Toast.makeText(SettingActivity.this, "设置保存成功", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                Toast.makeText(SettingActivity.this, "请输入合法数字", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBind) unbindService(conn);
    }
}