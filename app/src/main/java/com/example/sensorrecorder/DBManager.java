package com.example.sensorrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    private DBHelper helper;
    private SQLiteDatabase db;

    public DBManager(Context context) {
        helper = new DBHelper(context);
        db = helper.getWritableDatabase();
    }

    // 插入一条传感器记录
    public long insertRecord(SensorRecord record) {
        ContentValues values = new ContentValues();
        values.put("light", record.getLight());
        values.put("temp", record.getTemp());
        values.put("time", record.getTime());
        return db.insert("record", null, values);
    }

    // 查询所有历史数据
    public List<SensorRecord> queryAllRecord() {
        List<SensorRecord> list = new ArrayList<>();
        Cursor cursor = db.query("record", null, null, null, null, null, "time DESC");
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(0);
                float light = cursor.getFloat(1);
                float temp = cursor.getFloat(2);
                long time = cursor.getLong(3);
                list.add(new SensorRecord(id, light, temp, time));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    // 关闭数据库
    public void closeDB() {
        if (db != null) db.close();
    }
}