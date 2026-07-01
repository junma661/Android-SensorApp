package com.example.sensorrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvLight, tvTemp;
    private Button btnStart, btnStop, btnHistory, btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
    }

    // 绑定页面所有控件
    private void bindView() {
        tvStatus = findViewById(R.id.tv_status);
        tvLight = findViewById(R.id.tv_light);
        tvTemp = findViewById(R.id.tv_temp);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history);
        btnSetting = findViewById(R.id.btn_setting);
    }
}