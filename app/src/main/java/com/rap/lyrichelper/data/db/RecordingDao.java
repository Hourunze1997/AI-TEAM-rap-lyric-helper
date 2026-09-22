package com.rap.lyrichelper.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.rap.lyrichelper.data.model.Recording;

import java.util.List;

/**
 * 录音 DAO：按 lyric_id 查询、插入、删除。
 */
@Dao
public interface RecordingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Recording recording);

    @Delete
    void delete(Recording recording);

    @Query("SELECT * FROM recordings WHERE lyricId = :lyricId ORDER BY createdAt DESC")
    LiveData<List<Recording>> getByLyricId(long lyricId);

    @Query("SELECT * FROM recordings WHERE lyricId = :lyricId ORDER BY createdAt DESC")
    List<Recording> getByLyricIdSync(long lyricId);

    @Query("DELETE FROM recordings WHERE id = :id")
    void deleteById(long id);
}
