package com.example.sensorrecorder;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import java.util.List;

public class DataListActivity extends AppCompatActivity {
    private ListView lvData;
    private DBManager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_list);
        lvData = findViewById(R.id.lv_data);
        Button btnBack = findViewById(R.id.btn_back_main);
        dbManager = new DBManager(this);

        List<SensorData> allData = dbManager.queryAll();
        SensorDataAdapter adapter = new SensorDataAdapter(this, allData);
        lvData.setAdapter(adapter);

        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(DataListActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}