package com.rap.lyrichelper.data.model;

import org.json.JSONObject;

/**
 * 押韵词数据类（非 Entity），从 raw/rhyme_dict.json 加载。
 */
public class RhymeWord {

    public String word;
    /** 单押韵组 ID，如 "ANG"；双押韵组对，如 "ENG-ANG" */
    public String rhymeGroupPair;
    public int frequency;

    public RhymeWord() {}

    public RhymeWord(String word, String rhymeGroupPair, int frequency) {
        this.word = word;
        this.rhymeGroupPair = rhymeGroupPair;
        this.frequency = frequency;
    }

    /** 从 JSON 对象构建 */
    public static RhymeWord from(JSONObject obj) {
        RhymeWord rw = new RhymeWord();
        rw.word = obj.optString("word", "");
        rw.frequency = obj.optInt("frequency", 0);
        return rw;
    }

    public String getWord() { return word; }
    public String getRhymeGroupPair() { return rhymeGroupPair; }
    public int getFrequency() { return frequency; }
}
