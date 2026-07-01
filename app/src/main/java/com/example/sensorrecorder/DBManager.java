package com.example.sensorrecorder;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

public class DBManager {
    public class SensorRecord {
        String time;
        String type;
        float value;

        public SensorRecord(String t, String ty, float v) {
            time = t;
            type = ty;
            value = v;
        }
    }

    private static final String DB_NAME = "sensor_db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_DATA = "sensor_data";
    private static final String COL_ID = "id";
    private static final String COL_TIME = "record_time";
    private static final String COL_TYPE = "sensor_type";
    private static final String COL_VAL = "sensor_value";

    private DBHelper helper;
    private SQLiteDatabase db;

    private class DBHelper extends SQLiteOpenHelper {
        public DBHelper(Context ctx) {
            super(ctx, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            String createSql = "CREATE TABLE " + TABLE_DATA + " (" +
                    COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COL_TIME + " TEXT," +
                    COL_TYPE + " TEXT," +
                    COL_VAL + " REAL)";
            db.execSQL(createSql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldV, int newV) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_DATA);
            onCreate(db);
        }
    }

    public DBManager(Context ctx) {
        helper = new DBHelper(ctx);
    }

    public void insertData(String time, String type, float val) {
        db = helper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TIME, time);
        cv.put(COL_TYPE, type);
        cv.put(COL_VAL, val);
        db.insert(TABLE_DATA, null, cv);
        db.close();
    }

    public List<SensorRecord> queryAll() {
        List<SensorRecord> list = new ArrayList<>();
        db = helper.getReadableDatabase();
        Cursor cursor = db.query(TABLE_DATA, null, null, null, null, null, COL_ID + " DESC");
        int idxTime = cursor.getColumnIndex(COL_TIME);
        int idxType = cursor.getColumnIndex(COL_TYPE);
        int idxVal = cursor.getColumnIndex(COL_VAL);
        while (cursor.moveToNext()) {
            String t = cursor.getString(idxTime);
            String ty = cursor.getString(idxType);
            float v = cursor.getFloat(idxVal);
            list.add(new SensorRecord(t, ty, v));
        }
        cursor.close();
        db.close();
        return list;
    }

    public void clearAll() {
        db = helper.getWritableDatabase();
        db.delete(TABLE_DATA, null, null);
        db.close();
    }
}