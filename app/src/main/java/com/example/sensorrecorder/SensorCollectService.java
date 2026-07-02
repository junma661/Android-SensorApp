package com.example.sensorrecorder;

// 补齐缺失核心导入包（解决全部找不到符号报错）
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class SensorCollectService extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor lightSensor, tempSensor;
    private DBManager dbManager;
    private Handler handler = new Handler();
    private long period = 1000;
    private float curLight = 0, curTemp = 0;
    private boolean isRunning = false;

    // 光线告警阈值
    private int lightThreshold = 1000;
    // 防抖标记，避免持续超标重复发通知
    private boolean hasAlertTriggered = false;

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
        loadConfig();
        registerSensor();
        // 服务启动直接自动开始采集（由设置开关控制服务启停）
        startCollect();
    }

    private void loadConfig() {
        SharedPreferences sp = getSharedPreferences(SettingActivity.SP_NAME, Context.MODE_PRIVATE);
        period = sp.getLong(SettingActivity.KEY_PERIOD, 1000);
        lightThreshold = sp.getInt(SettingActivity.KEY_LIGHT_THRESHOLD, 1000);
    }

    private void registerSensor() {
        if (lightSensor != null)
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (tempSensor != null)
            mSensorManager.registerListener(this, tempSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void startCollect() {
        if (!isRunning) {
            isRunning = true;
            loadConfig();
            handler.postDelayed(saveTask, 0);
        }
    }

    public void stopCollect() {
        isRunning = false;
        handler.removeCallbacks(saveTask);
        // 停止采集后重置告警标记，下次开启可重新告警
        hasAlertTriggered = false;
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
            // 光线超过阈值且未触发告警
            if (curLight > lightThreshold && !hasAlertTriggered) {
                hasAlertTriggered = true;
                sendLightAlertNotification();
            }
            // 数值回落，重置标记，下次超标再次提醒
            if (curLight <= lightThreshold) {
                hasAlertTriggered = false;
            }
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

    /**
     * 发送光线超限系统通知
     */
    private void sendLightAlertNotification() {
        String channelId = "sensor_light_alert";
        String channelName = "光线异常告警";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android 8.0+ 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("光线数值超出设定阈值时推送告警消息");
            notificationManager.createNotificationChannel(channel);
        }

        // 点击通知跳转主页
        Intent jumpIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                jumpIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        Notification notify = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("光线传感器超限告警")
                .setContentText("当前光线：" + curLight + " lx，超过阈值 " + lightThreshold + " lx")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1001, notify);
    }
}