package com.example.sensorrecorder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.content.ContentValues;
import android.provider.MediaStore; // 修正正确包路径
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class DataListActivity extends AppCompatActivity {
    private DBManager dbManager;
    private TextView tvEmptyTip;
    private SensorDataAdapter adapter;
    // 导出权限请求码
    private static final int EXPORT_PERMISSION_CODE = 101;
    // 筛选选项：全部、光线、温度
    private final String[] filterItems = {"全部传感器", "光线", "温度"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);

        // 控件改为局部变量，消除黄色警告
        ListView lvData = findViewById(R.id.lv_data);
        Spinner spFilter = findViewById(R.id.sp_filter);
        tvEmptyTip = findViewById(R.id.tv_empty_tip);
        Button btnBack = findViewById(R.id.btn_back_main);
        Button btnExportCsv = findViewById(R.id.btn_export_csv);
        dbManager = new DBManager(this);

        // 初始化下拉筛选器
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                filterItems
        );
        spFilter.setAdapter(spinnerAdapter);

        // 默认加载全部数据
        refreshData("全部传感器", lvData);

        // 下拉切换筛选 - 完整实现两个抽象方法，修复匿名类报错
        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectType = filterItems[position];
                refreshData(selectType, lvData);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 空实现，必须保留
            }
        });

        // 返回主页按钮
        btnBack.setOnClickListener(v -> finish());

        // 导出CSV按钮点击事件
        btnExportCsv.setOnClickListener(v -> {
            if (checkExportPermission()) {
                exportDataToCsv();
            } else {
                requestExportPermission();
            }
        });
    }

    /** 根据筛选类型刷新列表数据 */
    private void refreshData(String filterType, ListView lvData) {
        List<SensorData> dataList;
        if ("全部传感器".equals(filterType)) {
            dataList = dbManager.queryAll();
        } else {
            dataList = dbManager.queryByType(filterType);
        }
        // 空数据提示显隐控制
        if(dataList.isEmpty()){
            tvEmptyTip.setVisibility(View.VISIBLE);
            lvData.setVisibility(View.GONE);
        }else{
            tvEmptyTip.setVisibility(View.GONE);
            lvData.setVisibility(View.VISIBLE);
        }
        adapter = new SensorDataAdapter(this, dataList);
        lvData.setAdapter(adapter);
    }

    // -------------------------- CSV导出核心逻辑 --------------------------
    /** 检查导出所需权限 */
    private boolean checkExportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 仅通知权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10~12 存储权限
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        } else {
            // 低版本读写存储
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /** 申请导出权限 */
    private void requestExportPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    EXPORT_PERMISSION_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXPORT_PERMISSION_CODE);
        }
    }

    /** 核心：导出全部数据到CSV文件 */
    private void exportDataToCsv() {
        List<SensorData> dataList = dbManager.queryAll();
        if (dataList.isEmpty()) {
            Toast.makeText(this, "暂无数据可导出", Toast.LENGTH_SHORT).show();
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

        // API29及以上用MediaStore，低版本走旧File方式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "文件创建失败", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                outputStream.write(csvContent.toString().getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                showExportSuccessNotification(uri, fileName);
                Toast.makeText(this, "导出成功！下拉通知栏点击打开", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // 安卓9及以下旧方案
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File csvFile = new File(downloadDir, fileName);
            try (FileWriter writer = new FileWriter(csvFile)) {
                writer.write(csvContent.toString());
                writer.flush();
                showExportSuccessNotification(Uri.fromFile(csvFile), fileName);
                Toast.makeText(this, "导出成功！文件保存至下载文件夹", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    /** 导出成功推送系统通知 */
// 修改为两个入参
    private void showExportSuccessNotification(Uri fileUri, String fileName) {
        String channelId = "sensor_export_notify";
        String channelName = "数据导出通知";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

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

    // 权限申请回调 - 正确重写父类方法，无Override报错
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == EXPORT_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDataToCsv();
            } else {
                Toast.makeText(this, "存储/通知权限被拒绝，无法导出文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}