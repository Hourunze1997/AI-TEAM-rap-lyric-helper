package com.rap.lyrichelper.data.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * 录音实体，对应 recordings 表，通过 lyric_id 关联歌词。
 */
@Entity(
    tableName = "recordings",
    foreignKeys = @ForeignKey(
        entity = Lyric.class,
        parentColumns = "id",
        childColumns = "lyricId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("lyricId")}
)
public class Recording {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long lyricId;

    /** 录音文件路径 */
    public String filePath;

    /** 录音时长（毫秒） */
    public long duration;

    public long createdAt;

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getLyricId() { return lyricId; }
    public void setLyricId(long lyricId) { this.lyricId = lyricId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
