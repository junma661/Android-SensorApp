package com.example.sensorrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {
    private EditText etPeriod;
    private SharedPreferences sp;
    private static final String SP_NAME = "sensor_setting";
    private static final String KEY_PERIOD = "sample_period";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        etPeriod = findViewById(R.id.et_period);
        Button btnSave = findViewById(R.id.btn_save_setting);
        Button btnBack = findViewById(R.id.btn_back_main);

        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        // 读取上次保存的周期，默认1000毫秒
        long defaultPeriod = sp.getLong(KEY_PERIOD, 1000);
        etPeriod.setText(String.valueOf(defaultPeriod));

        // 保存配置
        btnSave.setOnClickListener(v -> {
            String text = etPeriod.getText().toString();
            long period;
            try {
                period = Long.parseLong(text);
            } catch (Exception e) {
                Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit()
                    .putLong(KEY_PERIOD, period)
                    .apply();
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
        });

        // 返回主页面
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(SettingActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}