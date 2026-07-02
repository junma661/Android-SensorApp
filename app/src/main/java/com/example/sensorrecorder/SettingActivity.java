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
        boolean bgEnable = sp.getBoolean(KEY_BG_ENABLE, false);

        etPeriod.setText(String.valueOf(period));
        etLightThreshold.setText(String.valueOf(lightTh));
        swBgCollect.setChecked(bgEnable);

        // 点击整行文字区域切换开关
        layoutSwitchBg.setOnClickListener(v -> {
            boolean nowChecked = swBgCollect.isChecked();
            swBgCollect.setChecked(!nowChecked);
        });

        // ========= 新增：开关实时切换，直接控制后台采集 =========
        swBgCollect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Intent serviceIntent = new Intent(SettingActivity.this, SensorCollectService.class);
            if (isChecked) {
                // 打开开关：启动采集服务
                startService(serviceIntent);
                Toast.makeText(this, "已开启后台采集", Toast.LENGTH_SHORT).show();
            } else {
                // 关闭开关：停止采集服务
                stopService(serviceIntent);
                Toast.makeText(this, "已关闭后台采集", Toast.LENGTH_SHORT).show();
            }
            // 同步保存开关状态到SP
            sp.edit().putBoolean(KEY_BG_ENABLE, isChecked).apply();
        });

        // 保存按钮：仅保存周期、阈值，开关状态实时保存无需等保存按钮
        btnSave.setOnClickListener(v -> saveAllConfig());
        // 返回采集主页
        btnBack.setOnClickListener(v -> finish());
    }

    // 保存采样周期、光线阈值（开关实时保存，不走这里）
    private void saveAllConfig() {
        String periodStr = etPeriod.getText().toString().trim();
        String thresholdStr = etLightThreshold.getText().toString().trim();

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

        // 仅更新周期、阈值，开关状态已实时同步
        sp.edit()
                .putLong(KEY_PERIOD, samplePeriod)
                .putInt(KEY_LIGHT_THRESHOLD, lightThreshold)
                .apply();

        Toast.makeText(this, "采样与阈值保存成功", Toast.LENGTH_SHORT).show();
    }
}