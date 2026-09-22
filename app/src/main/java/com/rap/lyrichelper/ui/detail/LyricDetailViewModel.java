package com.rap.lyrichelper.ui.detail;

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
 * 详情页 ViewModel：加载歌词和录音列表。
 */
public class LyricDetailViewModel extends AndroidViewModel {

    private final LyricRepository lyricRepository;
    private final RecordingRepository recordingRepository;
    private final MutableLiveData<Lyric> lyric = new MutableLiveData<>();

    public LyricDetailViewModel(@NonNull Application application) {
        super(application);
        lyricRepository = new LyricRepository(application);
        recordingRepository = new RecordingRepository(application);
    }

    /** 加载歌词 */
    public void loadLyric(long id) {
        lyricRepository.getById(id, lyric -> {
            this.lyric.postValue(lyric);
        });
    }

    public LiveData<Lyric> getLyric() {
        return lyric;
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
}
