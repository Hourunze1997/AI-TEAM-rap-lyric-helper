package com.rap.lyrichelper.ui.memorize;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.rap.lyrichelper.data.model.Lyric;
import com.rap.lyrichelper.data.repository.LyricRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * 背诵 ViewModel：管理行索引、熟练度、显示状态。
 */
public class MemorizeViewModel extends AndroidViewModel {

    private final LyricRepository repository;
    private final MutableLiveData<List<Lyric.Line>> lines = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Integer> currentIndex = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> revealed = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> proficiency = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> totalLines = new MutableLiveData<>(0);
    private long currentLyricId = 0;

    public MemorizeViewModel(@NonNull Application application) {
        super(application);
        repository = new LyricRepository(application);
    }

    /** 加载歌词 */
    public void loadLyric(long lyricId) {
        this.currentLyricId = lyricId;
        repository.getById(lyricId, lyric -> {
            if (lyric != null) {
                List<Lyric.Line> parsedLines = Lyric.jsonToLines(lyric.getContent());
                lines.postValue(parsedLines);
                totalLines.postValue(parsedLines.size());
                currentIndex.postValue(0);
                revealed.postValue(false);
                proficiency.postValue(lyric.getProficiency());
            }
        });
    }

    public LiveData<List<Lyric.Line>> getLines() {
        return lines;
    }

    public LiveData<Integer> getCurrentIndex() {
        return currentIndex;
    }

    public LiveData<Boolean> getRevealed() {
        return revealed;
    }

    public LiveData<Integer> getProficiency() {
        return proficiency;
    }

    public LiveData<Integer> getTotalLines() {
        return totalLines;
    }

    /** 下一句 */
    public void next() {
        Integer current = currentIndex.getValue();
        Integer total = totalLines.getValue();
        if (current != null && total != null && current < total - 1) {
            currentIndex.setValue(current + 1);
            revealed.setValue(false);
        }
    }

    /** 上一句 */
    public void prev() {
        Integer current = currentIndex.getValue();
        if (current != null && current > 0) {
            currentIndex.setValue(current - 1);
            revealed.setValue(false);
        }
    }

    /** 显示/隐藏切换 */
    public void toggleReveal() {
        Boolean isRevealed = revealed.getValue();
        revealed.setValue(isRevealed == null || !isRevealed);
    }

    /** 设置熟练度 */
    public void setProficiency(int level) {
        if (level >= 1 && level <= 5) {
            proficiency.setValue(level);
            if (currentLyricId > 0) {
                repository.updateProficiency(currentLyricId, level, null);
            }
        }
    }

    public void setLyricId(long lyricId) {
        this.currentLyricId = lyricId;
    }
}
