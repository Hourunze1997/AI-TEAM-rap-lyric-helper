package com.rap.lyrichelper.ui.editor;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.model.Recording;
import com.rap.lyrichelper.data.repository.LyricRepository;
import com.rap.lyrichelper.data.repository.RecordingRepository;

import java.util.List;

/**
 * 编辑页 ViewModel：管理当前编辑的 Lyric 对象和录音列表。
 */
public class LyricEditViewModel extends AndroidViewModel {

    private final LyricRepository lyricRepository;
    private final RecordingRepository recordingRepository;
    private final MutableLiveData<Lyric> currentLyric = new MutableLiveData<>();

    public LyricEditViewModel(@NonNull Application application) {
        super(application);
        lyricRepository = new LyricRepository(application);
        recordingRepository = new RecordingRepository(application);
    }

    /** 加载已有歌词用于编辑，id=0 表示新建 */
    public void loadLyric(long id, LyricRepository.Callback<Lyric> callback) {
        if (id == 0) {
            Lyric lyric = new Lyric();
            lyric.setProficiency(1);
            currentLyric.setValue(lyric);
            if (callback != null) callback.onResult(lyric);
        } else {
            lyricRepository.getById(id, lyric -> {
                if (lyric == null) {
                    lyric = new Lyric();
                    lyric.setProficiency(1);
                }
                currentLyric.setValue(lyric);
                if (callback != null) callback.onResult(lyric);
            });
        }
    }

    /** 保存歌词 */
    public void save(Lyric lyric, LyricRepository.Callback<Long> callback) {
        lyricRepository.saveLyric(lyric, callback);
    }

    /** 获取录音列表 */
    public LiveData<List<Recording>> getRecordings(long lyricId) {
        return recordingRepository.getRecordings(lyricId);
    }

    public LyricRepository getLyricRepository() {
        return lyricRepository;
    }

    public RecordingRepository getRecordingRepository() {
        return recordingRepository;
    }

    public LiveData<Lyric> getCurrentLyric() {
        return currentLyric;
    }
}
