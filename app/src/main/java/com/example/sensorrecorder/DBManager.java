package com.example.sensorrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DBManager {
    private DBHelper helper;
    private SQLiteDatabase db;
    private SimpleDateFormat sdf;

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    // 插入一条传感器数据
    public long insertData(String sensorType, float value) {
        ContentValues values = new ContentValues();
        String timeStr = sdf.format(new Date());
        values.put(DBHelper.COL_TIME, timeStr);
        values.put(DBHelper.COL_TYPE, sensorType);
        values.put(DBHelper.COL_VALUE, value);
        return db.insert(DBHelper.TABLE_SENSOR_DATA, null, values);
    }

    // 查询全部数据
    public List<SensorData> queryAll() {
        List<SensorData> list = new ArrayList<>();
        Cursor cursor = db.query(DBHelper.TABLE_SENSOR_DATA, null, null, null, null, null, DBHelper.COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                SensorData data = new SensorData();
                data.setId(cursor.getLong(0));
                data.setTimestamp(cursor.getString(1));
                data.setSensorType(cursor.getString(2));
                data.setValue(cursor.getFloat(3));
                list.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 按传感器类型筛选
    public List<SensorData> queryByType(String type) {
        List<SensorData> list = new ArrayList<>();
        Cursor cursor = db.query(DBHelper.TABLE_SENSOR_DATA, null,
                DBHelper.COL_TYPE + "=?", new String[]{type},
                null, null, DBHelper.COL_ID + " DESC");
        if (cursor.moveToFirst()) {
            do {
                SensorData data = new SensorData();
                data.setId(cursor.getLong(0));
                data.setTimestamp(cursor.getString(1));
                data.setSensorType(cursor.getString(2));
                data.setValue(cursor.getFloat(3));
                list.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public void close() {
        if (db != null) db.close();
    }
}