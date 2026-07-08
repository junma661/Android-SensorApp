package com.example.sensorrecorder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import androidx.annotation.NonNull;

public class DataListActivity extends AppCompatActivity {
    private DBManager dbManager;
    private TextView tvEmptyTip;
    private SensorDataAdapter adapter;
    private ListView lvDataGlobal;
    private static final int EXPORT_PERMISSION_CODE = 101;
    private final String[] filterItems = {"全部传感器", "光线", "温度"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        lvDataGlobal = findViewById(R.id.lv_data);
        Spinner spFilter = findViewById(R.id.sp_filter);
        tvEmptyTip = findViewById(R.id.tv_empty_tip);
        Button btnBack = findViewById(R.id.btn_back_main);
        Button btnExportCsv = findViewById(R.id.btn_export_csv);
        dbManager = new DBManager(this);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                filterItems
        );
        spFilter.setAdapter(spinnerAdapter);

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectType = filterItems[position];
                loadDataAsync(selectType);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnBack.setOnClickListener(v -> finish());
        btnExportCsv.setOnClickListener(v -> {
            if (checkExportPermission()) {
                new Thread(this::exportDataToCsv).start();
            } else {
                requestExportPermission();
            }
        });

        // 已注释，解除页面自动返回问题
         new Handler(Looper.getMainLooper()).postDelayed(this::testReadFromContentProvider, 800);
        loadDataAsync("全部传感器");
    }

    /**
     * 异步加载列表数据，带页面生命周期防护
     */
    private void loadDataAsync(String filterType) {
        if (isFinishing() || isDestroyed()) return;

        new Thread(() -> {
            List<SensorData> tempList;
            try {
                if ("全部传感器".equals(filterType)) {
                    tempList = dbManager.queryAll();
                } else {
                    tempList = dbManager.queryByType(filterType);
                }
            } catch (Exception e) {
                Log.e("DB_LOAD_ERR", "数据库查询异常", e);
                tempList = new ArrayList<>();
            }
            List<SensorData> dataList = new ArrayList<>(tempList);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (dataList.isEmpty()) {
                    tvEmptyTip.setVisibility(View.VISIBLE);
                    lvDataGlobal.setVisibility(View.GONE);
                } else {
                    tvEmptyTip.setVisibility(View.GONE);
                    lvDataGlobal.setVisibility(View.VISIBLE);
                }
                adapter = new SensorDataAdapter(DataListActivity.this, dataList);
                lvDataGlobal.setAdapter(adapter);
            });
        }).start();
    }

    /**
     * ContentProvider读取方法（代码保留，未调用，满足实验代码要求）
     */
    private void testReadFromContentProvider() {
        if (isFinishing() || isDestroyed()) return;

        new Thread(() -> {
            Cursor cursor = getContentResolver().query(
                    SensorContentProvider.SENSOR_DATA_URI,
                    null,
                    null,
                    null,
                    DBHelper.COL_ID + " DESC"
            );
            StringBuilder showText = new StringBuilder("ContentProvider读取结果：\n");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // 全部使用DBHelper常量，不会再出现列名错误
                    int idxType = cursor.getColumnIndex(DBHelper.COL_TYPE);
                    String type = idxType >= 0 ? cursor.getString(idxType) : "";

                    int idxVal = cursor.getColumnIndex(DBHelper.COL_VALUE);
                    float val = idxVal >= 0 ? cursor.getFloat(idxVal) : 0f;

                    int idxTime = cursor.getColumnIndex(DBHelper.COL_TIME);
                    String time = idxTime >= 0 ? cursor.getString(idxTime) : "";

                    showText.append(type).append(" | ").append(val).append(" | ").append(time).append("\n");
                }
                cursor.close();
            }
            runOnUiThread(() -> Toast.makeText(DataListActivity.this, showText.toString(), Toast.LENGTH_LONG).show());
        }).start();
    }

    // ---------------- 权限校验 ----------------
    private boolean checkExportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestExportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, EXPORT_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, EXPORT_PERMISSION_CODE);
        }
    }

    // ---------------- CSV导出逻辑（增加输出流空判断） ----------------
    private void exportDataToCsv() {
        List<SensorData> dataList = dbManager.queryAll();
        if (dataList.isEmpty()) {
            runOnUiThread(() -> Toast.makeText(this, "暂无数据可导出", Toast.LENGTH_SHORT).show());
            return;
        }

        StringBuilder csvContent = new StringBuilder();
        csvContent.append("采集时间,传感器类型,传感器数值\n");
        for (SensorData data : dataList) {
            String time = data.getTimestamp().replace(",", "，");
            String type = data.getSensorType().replace(",", "，");
            String value = String.valueOf(data.getValue()).replace(",", "，");
            csvContent.append(time).append(",").append(type).append(",").append(value).append("\n");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String fileName = "SensorData_Export_" + sdf.format(new Date()) + ".csv";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            android.net.Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                runOnUiThread(() -> Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show());
                return;
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvContent.toString().getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
                showExportSuccessNotification(uri, fileName);
                runOnUiThread(() -> Toast.makeText(this, "导出成功！下拉通知栏点击打开", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e("CSV_EXPORT", "写入文件异常", e);
                runOnUiThread(() -> Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        } else {
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadDir, fileName);
            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write(csvContent.toString());
                writer.flush();
                showExportSuccessNotification(android.net.Uri.fromFile(csvFile), fileName);
                runOnUiThread(() -> Toast.makeText(this, "导出成功！文件保存至下载文件夹", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e("CSV_EXPORT", "写入文件异常", e);
                runOnUiThread(() -> Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }

    // ---------------- 导出完成通知 ----------------
    private void showExportSuccessNotification(android.net.Uri fileUri, String fileName) {
        String channelId = "sensor_export_notify";
        String channelName = "数据导出通知";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        notificationManager.createNotificationChannel(channel);

        Intent openIntent = new Intent(Intent.ACTION_VIEW);
        openIntent.setDataAndType(fileUri, "text/csv");
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("传感器数据导出完成")
                .setContentText("文件：" + fileName + "，点击打开")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(2001, notification);
    }

    // ---------------- 权限回调 ----------------
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXPORT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new Thread(this::exportDataToCsv).start();
            } else {
                Toast.makeText(this, "存储/通知权限被拒绝，无法导出文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}