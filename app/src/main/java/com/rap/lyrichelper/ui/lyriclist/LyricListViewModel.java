package com.rap.lyrichelper.ui.lyriclist;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.repository.LyricRepository;

import java.util.List;

/**
 * 列表页 ViewModel，LiveData 暴露歌词列表和搜索结果。
 */
public class LyricListViewModel extends AndroidViewModel {

    private final LyricRepository repository;
    private final MediatorLiveData<List<Lyric>> lyrics = new MediatorLiveData<>();
    private LiveData<List<Lyric>> allLyricsSource;
    private LiveData<List<Lyric>> searchSource;

    public LyricListViewModel(@NonNull Application application) {
        super(application);
        repository = new LyricRepository(application);
        loadAll();
    }

    /** 加载全部歌词 */
    private void loadAll() {
        if (allLyricsSource != null) {
            lyrics.removeSource(allLyricsSource);
        }
        allLyricsSource = repository.getLyrics();
        lyrics.addSource(allLyricsSource, lyrics::setValue);
    }

    /** 按关键词搜索 */
    public void search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            loadAll();
            return;
        }
        if (searchSource != null) {
            lyrics.removeSource(searchSource);
        }
        searchSource = repository.search(keyword.trim());
        lyrics.addSource(searchSource, lyrics::setValue);
    }

    public LiveData<List<Lyric>> getLyrics() {
        return lyrics;
    }

    public LyricRepository getRepository() {
        return repository;
    }
}
