package com.sharemedia.kuaichuan.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class MySQLiteHelper extends SQLiteOpenHelper {
    private static final String db_name = "kuaichuan.db";
    private static final int version = 1; // 数据库版本
    private  static Context applicationContext;
    public MySQLiteHelper(Context context) {
        super(context, db_name, null, version);
        applicationContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i("Log","没有数据库,创建数据库");
        String sqlPhoto = "create table photo (Id INTEGER primary key autoincrement,AbsolutePath TEXT,CreateTime TEXT,UploadTime TEXT, CardNo TEXT,SiteId TEXT,MD5 TEXT)";

        //执行建表语句
        db.execSQL(sqlPhoto);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //数据库的版本发生变化的时候，会自动执行。

    }

    public int insertPhoto(ContentValues values){
        SQLiteDatabase db =getWritableDatabase();
        db.insert("photo", null, values);
        db.close();
        return 1;
    }
}
