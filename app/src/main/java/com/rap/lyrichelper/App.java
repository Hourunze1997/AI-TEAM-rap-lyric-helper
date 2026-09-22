package com.rap.lyrichelper;

import android.app.Application;

import com.rap.lyrichelper.data.db.AppDatabase;

/**
 * Application 入口，初始化 Room 数据库单例。
 */
public class App extends Application {

    private static AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();
        database = AppDatabase.getInstance(this);
    }

    public static AppDatabase getDatabase() {
        return database;
    }
}
