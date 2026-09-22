package com.rap.lyrichelper.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.rap.lyrichelper.data.model.Lyric;

import java.util.List;

/**
 * 歌词 DAO：增删改查、按标题/标签搜索。
 */
@Dao
public interface LyricDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Lyric lyric);

    @Update
    void update(Lyric lyric);

    @Delete
    void delete(Lyric lyric);

    @Query("SELECT * FROM lyrics ORDER BY updatedAt DESC")
    LiveData<List<Lyric>> getAll();

    @Query("SELECT * FROM lyrics WHERE title LIKE '%' || :keyword || '%' ORDER BY updatedAt DESC")
    LiveData<List<Lyric>> searchByTitle(String keyword);

    @Query("SELECT * FROM lyrics WHERE id = :id")
    Lyric getById(long id);

    @Query("UPDATE lyrics SET proficiency = :proficiency, updatedAt = :updatedAt WHERE id = :id")
    void updateProficiency(long id, int proficiency, long updatedAt);
}
