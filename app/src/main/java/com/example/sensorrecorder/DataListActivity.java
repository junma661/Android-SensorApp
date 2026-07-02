package com.example.sensorrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import java.util.List;

public class DataListActivity extends AppCompatActivity {
    private ListView lvData;
    private Spinner spFilter;
    private DBManager dbManager;
    private SensorDataAdapter adapter;

    // 下拉选项
    private final String[] filterItems = {"全部传感器", "光线", "温度"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);
        lvData = findViewById(R.id.lv_data);
        spFilter = findViewById(R.id.sp_filter);
        Button btnBack = findViewById(R.id.btn_back_main);
        dbManager = new DBManager(this);

        // 下拉适配器
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                filterItems
        );
        spFilter.setAdapter(spinnerAdapter);

        // 默认加载全部
        refreshData(filterItems[0]);

        spFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectText = filterItems[position];
                refreshData(selectText);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnBack.setOnClickListener(v -> {
            finish();
        });
    }

    private void refreshData(String selectText) {
        List<SensorData> dataList;
        // 修复判断条件：匹配完整"全部传感器"文本
        if ("全部传感器".equals(selectText)) {
            dataList = dbManager.queryAll();
        } else {
            // 光线/温度直接按类型筛选
            dataList = dbManager.queryByType(selectText);
        }
        // 刷新适配器
        adapter = new SensorDataAdapter(this, dataList);
        lvData.setAdapter(adapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}