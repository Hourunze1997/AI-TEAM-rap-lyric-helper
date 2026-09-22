package com.rap.lyrichelper.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rap.lyrichelper.data.db.AppDatabase;
import com.rap.lyrichelper.data.db.LyricDao;
import com.rap.lyrichelper.data.model.Lyric;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 歌词数据仓库，封装 DAO 调用，运行在后台线程。
 */
public class LyricRepository {

    private final LyricDao lyricDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public LyricRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        lyricDao = db.lyricDao();
    }

    /** 保存歌词（新建或更新），回调返回 id */
    public void saveLyric(Lyric lyric, Callback<Long> callback) {
        executor.execute(() -> {
            long id;
            if (lyric.getId() == 0) {
                lyric.setCreatedAt(System.currentTimeMillis());
                lyric.setUpdatedAt(System.currentTimeMillis());
                id = lyricDao.insert(lyric);
            } else {
                lyric.setUpdatedAt(System.currentTimeMillis());
                lyricDao.update(lyric);
                id = lyric.getId();
            }
            if (callback != null) callback.onResult(id);
        });
    }

    /** 删除歌词 */
    public void deleteLyric(Lyric lyric, Callback<Void> callback) {
        executor.execute(() -> {
            lyricDao.delete(lyric);
            if (callback != null) callback.onResult(null);
        });
    }

    /** 获取全部歌词（按更新时间降序） */
    public LiveData<List<Lyric>> getLyrics() {
        return lyricDao.getAll();
    }

    /** 按标题搜索 */
    public LiveData<List<Lyric>> search(String keyword) {
        return lyricDao.searchByTitle(keyword);
    }

    /** 更新熟练度 */
    public void updateProficiency(long id, int proficiency, Callback<Void> callback) {
        executor.execute(() -> {
            lyricDao.updateProficiency(id, proficiency, System.currentTimeMillis());
            if (callback != null) callback.onResult(null);
        });
    }

    /** 按 id 查询单条 */
    public void getById(long id, Callback<Lyric> callback) {
        executor.execute(() -> {
            Lyric lyric = lyricDao.getById(id);
            if (callback != null) callback.onResult(lyric);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
