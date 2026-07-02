package com.example.sensorrecorder;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class SensorCollectService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor lightSensor, tempSensor;
    private DBManager dbManager;
    private Handler handler = new Handler();
    private long period = 1000;
    private float curLight = 0, curTemp = 0;
    private boolean isRunning = false;

    // Binder 用于Activity绑定获取实时数据
    private final MyBinder binder = new MyBinder();
    public class MyBinder extends Binder {
        public SensorCollectService getService() {
            return SensorCollectService.this;
        }
    }

    // 定时保存任务
    private Runnable saveTask = new Runnable() {
        @Override
        public void run() {
            if (isRunning) {
                dbManager.insertData("光线", curLight);
                dbManager.insertData("温度", curTemp);
                handler.postDelayed(this, period);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        tempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        dbManager = new DBManager(this);
        loadPeriod();
        registerSensor();
    }

    // 读取设置的采样周期
    private void loadPeriod() {
        SharedPreferences sp = getSharedPreferences("sensor_setting", MODE_PRIVATE);
        period = sp.getLong("sample_period", 1000);
    }

    private void registerSensor() {
        if (lightSensor != null)
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (tempSensor != null)
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // 启动后台采集
    public void startCollect() {
        if (!isRunning) {
            isRunning = true;
            loadPeriod();
            handler.postDelayed(saveTask, 0);
        }
    }

    // 停止采集
    public void stopCollect() {
        isRunning = false;
        handler.removeCallbacks(saveTask);
    }

    // 获取当前传感器数值，供界面刷新
    public float getCurLight() { return curLight; }
    public float getCurTemp() { return curTemp; }
    public boolean isCollecting() { return isRunning; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            curLight = event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            curTemp = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCollect();
        mSensorManager.unregisterListener(this);
        dbManager.close();
    }
}