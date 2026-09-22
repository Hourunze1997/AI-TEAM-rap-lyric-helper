package com.rap.lyrichelper.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.model.Recording;

/**
 * RoomDatabase 单例，注册 Lyric/Recording Entity 和 DAO。
 */
@Database(
    entities = {Lyric.class, Recording.class},
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract LyricDao lyricDao();
    public abstract RecordingDao recordingDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "lyrichelper.db"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
