package com.example.sensorrecorder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class SensorDataProvider extends ContentProvider {
    private DBHelper dbHelper;
    private SQLiteDatabase db;

    // 授权标识，和AndroidManifest一致
    public static final String AUTHORITY = "com.example.sensorrecorder.provider";
    // Uri路径
    public static final Uri URI_ALL_DATA = Uri.parse("content://" + AUTHORITY + "/sensor_data");
    // 匹配码
    private static final int CODE_ALL = 1;
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "sensor_data", CODE_ALL);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DBHelper(getContext());
        db = dbHelper.getWritableDatabase();
        return true;
    }

    // 对外查询接口（作业要求至少支持查询）
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        switch (uriMatcher.match(uri)) {
            case CODE_ALL:
                cursor = db.query(DBHelper.TABLE_SENSOR_DATA, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("未知Uri: " + uri);
        }
        // 监听数据变化
        if (cursor != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    // 下面增删改本次作业仅要求查询，空实现即可
    @Override
    public String getType(Uri uri) { return null; }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}