package com.example.sensorrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "sensor_db";
    public static final int DB_VERSION = 2;
    // 作业标准表
    public static final String TABLE_SENSOR_DATA = "sensor_data";
    // 字段
    public static final String COL_ID = "_id";
    public static final String COL_TIME = "timestamp";
    public static final String COL_TYPE = "sensor_type";
    public static final String COL_VALUE = "value";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createSql = "CREATE TABLE " + TABLE_SENSOR_DATA + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TIME + " TEXT,"
                + COL_TYPE + " TEXT,"
                + COL_VALUE + " REAL)";
        db.execSQL(createSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_DATA);
        onCreate(db);
    }
}