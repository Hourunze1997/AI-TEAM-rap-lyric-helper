package com.rap.lyrichelper.data.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 歌词行数据类（POJO），对应 content JSON 中的一行
 */
public class LyricLine {

    private String text;
    private String section;   // verse / chorus / bridge / hook
    private String rhymeMark; // 押韵标记 A / B / C ...

    public LyricLine() {
        this.text = "";
        this.section = "verse";
        this.rhymeMark = "";
    }

    public LyricLine(String text, String section, String rhymeMark) {
        this.text = text;
        this.section = section;
        this.rhymeMark = rhymeMark;
    }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRhymeMark() { return rhymeMark; }
    public void setRhymeMark(String rhymeMark) { this.rhymeMark = rhymeMark; }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("text", text != null ? text : "");
        obj.put("section", section != null ? section : "verse");
        obj.put("rhymeMark", rhymeMark != null ? rhymeMark : "");
        return obj;
    }

    public static LyricLine fromJson(JSONObject json) {
        LyricLine line = new LyricLine();
        line.setText(json.optString("text", ""));
        line.setSection(json.optString("section", "verse"));
        line.setRhymeMark(json.optString("rhymeMark", ""));
        return line;
    }

    /** 将 List<LyricLine> 序列化为 JSON 字符串 */
    public static String toJsonString(List<LyricLine> lines) {
        try {
            JSONArray arr = new JSONArray();
            for (LyricLine line : lines) {
                arr.put(line.toJson());
            }
            return arr.toString();
        } catch (JSONException e) {
            return "[]";
        }
    }

    /** 从 JSON 字符串反序列化为 List<LyricLine> */
    public static List<LyricLine> fromJsonString(String json) {
        List<LyricLine> lines = new ArrayList<>();
        if (json == null || json.isEmpty()) return lines;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                lines.add(LyricLine.fromJson(arr.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // 解析失败返回空列表
        }
        return lines;
    }
}
