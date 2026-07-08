package com.example.sensorrecorder;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    // 数据库名称、版本
    public static final String DB_NAME = "sensor_db";
    public static final int DB_VERSION = 2;

    // 数据表名
    public static final String TABLE_SENSOR_DATA = "sensor_data";

    // 字段常量（统一给ContentProvider复用，杜绝拼写错误）
    public static final String COL_ID = "_id";
    public static final String COL_TIME = "timestamp";
    public static final String COL_TYPE = "sensor_type";
    public static final String COL_VALUE = "value";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 创建传感器数据表
        String createSql = "CREATE TABLE " + TABLE_SENSOR_DATA + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TIME + " TEXT,"
                + COL_TYPE + " TEXT,"
                + COL_VALUE + " REAL)";
        db.execSQL(createSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 版本升级删除旧表重建
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_DATA);
        onCreate(db);
    }
}