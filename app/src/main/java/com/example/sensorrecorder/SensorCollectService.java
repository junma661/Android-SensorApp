package com.example.sensorrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.lang.ref.WeakReference;

public class SensorCollectService extends Service implements SensorEventListener {
    private final IBinder binder = new MyBinder();
    private SensorManager sensorManager;
    private Sensor lightSensor;
    private Sensor tempSensor;
    private DBManager dbManager;
    private NotificationManager notifyMgr;
    private static final String CHANNEL_ID = "sensor_collect_channel";
    private static final int NOTIFY_ID_FG = 1001;

    private float curLight = 0f;
    private float curTemp = 0f;
    private boolean isCollecting = false;
    private long sampleInterval = 1000;
    private Thread saveThread;
    private boolean isWriting = false;
    private WeakReference<MainActivity> mainActRef;

    public class MyBinder extends Binder {
        public SensorCollectService getService() {
            return SensorCollectService.this;
        }
    }

    public void setMainActivity(MainActivity activity) {
        mainActRef = new WeakReference<>(activity);
    }

    // 仅保留修改采样间隔方法
    public void setSampleInterval(long interval) {
        sampleInterval = interval;
    }

    public float getCurLight() {
        return curLight;
    }

    public float getCurTemp() {
        return curTemp;
    }

    public boolean isCollecting() {
        return isCollecting;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dbManager = new DBManager(getApplicationContext());
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        notifyMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        tempSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "传感器采集服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            notifyMgr.createNotificationChannel(channel);
        }
    }

    public void startCollect() {
        if (isCollecting) return;
        isCollecting = true;
        if (lightSensor != null)
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        if (tempSensor != null)
            sensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_UI);

        Notification notify = buildForegroundNotify();
        // 纯数字2，消除找不到常量符号报错，适配低compileSdk
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFY_ID_FG, notify, 2);
        } else {
            startForeground(NOTIFY_ID_FG, notify);
        }
        startSaveLoop();
    }

    public void stopCollect() {
        isCollecting = false;
        sensorManager.unregisterListener(this);
        stopForeground(true);
    }

    private Notification buildForegroundNotify() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("传感器采集中")
                .setContentText("光线、温度实时采集")
                .build();
    }

    private void startSaveLoop() {
        saveThread = new Thread(this::saveDataLoop);
        saveThread.start();
    }

    private void saveDataLoop() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        while (isCollecting) {
            if (!isWriting) {
                isWriting = true;
                String time = sdf.format(new Date());
                new Thread(() -> {
                    dbManager.insertData(time, "光线", curLight);
                    dbManager.insertData(time, "温度", curTemp);
                    isWriting = false;
                }).start();
            }
            try {
                Thread.sleep(sampleInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            curLight = event.values[0];
        }
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            curTemp = event.values[0];
        }
        if (mainActRef.get() != null) {
            MainActivity act = mainActRef.get();
            act.runOnUiThread(() -> act.refreshUI(curLight, curTemp));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopCollect();
        if (saveThread != null) saveThread.interrupt();
    }
}