package com.example.sensorrecorder;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SensorContentProvider extends ContentProvider {
    // 和AndroidManifest完全一致
    public static final String AUTHORITY = "com.example.sensorrecorder.provider";
    public static final Uri SENSOR_DATA_URI = Uri.parse("content://" + AUTHORITY + "/sensor");

    // Uri匹配器
    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    private static final int CODE_SENSOR_ALL = 1;

    static {
        uriMatcher.addURI(AUTHORITY, "sensor", CODE_SENSOR_ALL);
    }

    // 查询返回列（和DBHelper字段完全对应）
    private static final String[] PROJECTION = {
            DBHelper.COL_ID,
            DBHelper.COL_TIME,
            DBHelper.COL_TYPE,
            DBHelper.COL_VALUE
    };

    private DBHelper dbHelper;

    @Override
    public boolean onCreate() {
        if (getContext() == null) return false;
        dbHelper = new DBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int match = uriMatcher.match(uri);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        if (match == CODE_SENSOR_ALL) {
            // 默认按_id倒序
            String sort = sortOrder == null ? DBHelper.COL_ID + " DESC" : sortOrder;
            cursor = db.query(
                    DBHelper.TABLE_SENSOR_DATA,
                    projection == null ? PROJECTION : projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sort
            );
        }
        if (cursor != null && getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int match = uriMatcher.match(uri);
        if (match != CODE_SENSOR_ALL) return null;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long id = db.insert(DBHelper.TABLE_SENSOR_DATA, null, values);
        if (id > 0 && getContext() != null) {
            Uri newUri = Uri.parse(SENSOR_DATA_URI + "/" + id);
            getContext().getContentResolver().notifyChange(uri, null);
            return newUri;
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        if (match != CODE_SENSOR_ALL) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.delete(DBHelper.TABLE_SENSOR_DATA, selection, selectionArgs);
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        int match = uriMatcher.match(uri);
        if (match != CODE_SENSOR_ALL) return 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = db.update(DBHelper.TABLE_SENSOR_DATA, values, selection, selectionArgs);
        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }
}