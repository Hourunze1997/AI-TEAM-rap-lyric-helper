package com.rap.lyrichelper.data.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.rap.lyrichelper.data.db.Converters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌词实体，对应 lyrics 表。
 * content 存储分行 JSON：[{"text":"...","section":"verse","rhymeMark":"A"}]
 */
@Entity(tableName = "lyrics")
public class Lyric {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    /** 分行 JSON 数据 */
    public String content;

    @TypeConverters(Converters.class)
    public List<String> tags;

    /** 熟练度 1-5 */
    public int proficiency;

    public long createdAt;

    public long updatedAt;

    // ---- getters / setters ----

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public int getProficiency() { return proficiency; }
    public void setProficiency(int proficiency) { this.proficiency = proficiency; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // ---- 分行 JSON 解析工具 ----

    /** 一行歌词的数据结构 */
    public static class Line {
        public String text;
        public String section; // verse / chorus / bridge / hook
        public String rhymeMark; // 押韵标记 A/B/C...

        public Line() {}

        public Line(String text, String section, String rhymeMark) {
            this.text = text;
            this.section = section;
            this.rhymeMark = rhymeMark;
        }

        public JSONObject toJson() throws Exception {
            JSONObject obj = new JSONObject();
            obj.put("text", text != null ? text : "");
            obj.put("section", section != null ? section : "verse");
            obj.put("rhymeMark", rhymeMark != null ? rhymeMark : "");
            return obj;
        }

        public static Line fromJson(JSONObject obj) {
            Line line = new Line();
            line.text = obj.optString("text", "");
            line.section = obj.optString("section", "verse");
            line.rhymeMark = obj.optString("rhymeMark", "");
            return line;
        }
    }

    /** 将 List<Line> 序列化为 content JSON 字符串 */
    public static String linesToJson(List<Line> lines) {
        try {
            JSONArray arr = new JSONArray();
            for (Line line : lines) {
                arr.put(line.toJson());
            }
            return arr.toString();
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 将 content JSON 字符串反序列化为 List<Line> */
    public static List<Line> jsonToLines(String json) {
        List<Line> lines = new ArrayList<>();
        if (json == null || json.isEmpty()) return lines;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lines.add(Line.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception e) {
            // 解析失败返回空列表
        }
        return lines;
    }
}
