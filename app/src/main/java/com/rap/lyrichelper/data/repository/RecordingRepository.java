package com.rap.lyrichelper.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.rap.lyrichelper.data.db.AppDatabase;
import com.rap.lyrichelper.data.db.RecordingDao;
import com.rap.lyrichelper.data.model.Recording;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 录音数据仓库，管理录音文件和记录。
 */
public class RecordingRepository {

    private final RecordingDao recordingDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RecordingRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        recordingDao = db.recordingDao();
    }

    /** 保存录音记录 */
    public void saveRecording(Recording recording, Callback<Long> callback) {
        executor.execute(() -> {
            recording.setCreatedAt(System.currentTimeMillis());
            long id = recordingDao.insert(recording);
            if (callback != null) callback.onResult(id);
        });
    }

    /** 按 lyricId 获取录音列表 */
    public LiveData<List<Recording>> getRecordings(long lyricId) {
        return recordingDao.getByLyricId(lyricId);
    }

    /** 删除录音记录 */
    public void deleteRecording(Recording recording, Callback<Void> callback) {
        executor.execute(() -> {
            recordingDao.delete(recording);
            if (callback != null) callback.onResult(null);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
