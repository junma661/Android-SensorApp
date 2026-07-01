package com.example.sensorrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.app.AlertDialog;
import java.util.List;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private TextView tvStatus, tvLight, tvTemp;
    private Button btnStart, btnStop, btnHistory, btnSetting;

    // 传感器相关对象
    private SensorManager mSensorManager;
    private Sensor lightSensor;
    private Sensor tempSensor;

    // 数据库、采集状态、缓存传感器数值
    private DBManager dbManager;
    private boolean isCollecting = false;
    private float currentLight = 0;
    private float currentTemp = 0;

    // 传感器数据监听回调
    private final SensorEventListener sensorListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            float value = event.values[0];
            if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                currentLight = value;
                tvLight.setText("光线传感器：" + value + " lx");
            } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
                currentTemp = value;
                tvTemp.setText("温度传感器：" + value + " ℃");
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindView();
        initSensor();
        // 初始化数据库管理类
        dbManager = new DBManager(this);
    }

    // 绑定页面所有控件 + 按钮点击逻辑
    private void bindView() {
        tvStatus = findViewById(R.id.tv_status);
        tvLight = findViewById(R.id.tv_light);
        tvTemp = findViewById(R.id.tv_temp);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnHistory = findViewById(R.id.btn_history);
        btnSetting = findViewById(R.id.btn_setting);

        // 开始采集按钮
        btnStart.setOnClickListener(v -> {
            isCollecting = true;
            tvStatus.setText("采集状态：正在采集数据");
        });

        // 停止采集按钮，停止时存入数据库
        btnStop.setOnClickListener(v -> {
            if (isCollecting) {
                // 获取当前时间戳
                long nowTime = System.currentTimeMillis();
                // 构造记录对象并插入数据库
                SensorRecord record = new SensorRecord(currentLight, currentTemp, nowTime);
                dbManager.insertRecord(record);
                tvStatus.setText("采集状态：已保存本条数据");
            }
            isCollecting = false;
        });

        // 查看历史数据（后续跳转页面，先空实现）
        btnHistory.setOnClickListener(v -> {
            // 查询全部数据库记录
            List<SensorRecord> dataList = dbManager.queryAllRecord();
            StringBuilder showText = new StringBuilder();

            if (dataList.isEmpty()) {
                showText.append("暂无存储的传感器记录\n\n操作步骤：\n1. 模拟器右上角 ⋮ 打开Virtual Sensors\n2. 拖动光线、温度滑块修改数值\n3. 点【开始采集】→【停止采集】保存数据");
            } else {
                // 遍历拼接所有记录
                for (SensorRecord record : dataList) {
                    showText.append("光线：").append(record.getLight()).append(" lx\n");
                    showText.append("温度：").append(record.getTemp()).append(" ℃\n");
                    showText.append("时间戳：").append(record.getTime()).append("\n");
                    showText.append("-------------------------\n");
                }
            }

            // 弹出对话框展示
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("本地历史采集记录")
                    .setMessage(showText.toString())
                    .setPositiveButton("关闭", null)
                    .show();
        });
        btnSetting.setOnClickListener(v -> {});
    }

    // 初始化传感器管理器、光线、温度传感器
    private void initSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        tempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    // 页面可见时注册传感器监听
    @Override
    protected void onResume() {
        super.onResume();
        if (lightSensor != null) {
            mSensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (tempSensor != null) {
            mSensorManager.registerListener(sensorListener, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        tvStatus.setText("采集状态：前台实时读取中");
    }

    // 页面切后台/关闭时注销监听，省电防内存泄漏
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(sensorListener);
        tvStatus.setText("采集状态：已暂停");
    }

    // Activity销毁，关闭数据库释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.closeDB();
        }
    }
}