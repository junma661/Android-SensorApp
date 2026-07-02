package com.example.sensorrecorder;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingActivity extends AppCompatActivity {
    // 全部常量设为 public static，SensorCollectService 可正常读取
    public static final String SP_NAME = "sensor_config";
    public static final String KEY_PERIOD = "sample_period";
    public static final String KEY_BG_ENABLE = "bg_collect_enable";
    public static final String KEY_LIGHT_THRESHOLD = "light_threshold";

    private EditText etPeriod;
    private EditText etLightThreshold;
    private Switch swBgCollect;
    private LinearLayout layoutSwitchBg;
    private Button btnSave;
    private Button btnBack;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // 绑定控件
        etPeriod = findViewById(R.id.et_period);
        etLightThreshold = findViewById(R.id.et_light_threshold);
        swBgCollect = findViewById(R.id.sw_background_collect);
        layoutSwitchBg = findViewById(R.id.layout_switch_bg);
        btnSave = findViewById(R.id.btn_save_setting);
        btnBack = findViewById(R.id.btn_back_main);

        sp = getSharedPreferences(SP_NAME, MODE_PRIVATE);
        // 读取本地配置回填输入框
        long period = sp.getLong(KEY_PERIOD, 1000);
        int lightTh = sp.getInt(KEY_LIGHT_THRESHOLD, 1000);
        boolean bgEnable = sp.getBoolean(KEY_BG_ENABLE, true);

        etPeriod.setText(String.valueOf(period));
        etLightThreshold.setText(String.valueOf(lightTh));
        swBgCollect.setChecked(bgEnable);

        // 点击整行文字区域切换开关，解决触摸区域过小报错
        layoutSwitchBg.setOnClickListener(v -> {
            boolean nowChecked = swBgCollect.isChecked();
            swBgCollect.setChecked(!nowChecked);
        });

        // 保存按钮
        btnSave.setOnClickListener(v -> saveAllConfig());
        // 返回采集主页
        btnBack.setOnClickListener(v -> finish());
    }

    // 保存全部三项配置：采样周期、后台采集开关、光线告警阈值
    private void saveAllConfig() {
        String periodStr = etPeriod.getText().toString().trim();
        String thresholdStr = etLightThreshold.getText().toString().trim();

        // 非空校验
        if (periodStr.isEmpty() || thresholdStr.isEmpty()) {
            Toast.makeText(this, "采样周期、光线阈值不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        long samplePeriod;
        int lightThreshold;
        try {
            samplePeriod = Long.parseLong(periodStr);
            lightThreshold = Integer.parseInt(thresholdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入合法数字", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean bgCollectSwitch = swBgCollect.isChecked();

        // 写入SharedPreferences持久化
        sp.edit()
                .putLong(KEY_PERIOD, samplePeriod)
                .putInt(KEY_LIGHT_THRESHOLD, lightThreshold)
                .putBoolean(KEY_BG_ENABLE, bgCollectSwitch)
                .apply();

        Toast.makeText(this, "设置保存成功", Toast.LENGTH_SHORT).show();
    }
}