package com.example.sensorrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名、表名
    private static final String DB_NAME = "sensor_db";
    private static final String TABLE_RECORD = "record";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建数据表
        String sql = "CREATE TABLE " + TABLE_RECORD + "("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "light REAL,"
                + "temp REAL,"
                + "time LONG)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORD);
        onCreate(db);
    }
}