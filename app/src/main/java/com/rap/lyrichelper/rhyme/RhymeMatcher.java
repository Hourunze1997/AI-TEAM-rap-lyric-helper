package com.rap.lyrichelper.rhyme;

import android.content.Context;

import com.rap.lyrichelper.data.model.RhymeWord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 押韵匹配引擎。
 * 加载 rhyme_dict.json 预计算所有词的韵组/韵组对，
 * 按输入词韵组检索候选；单押比末字韵组相等，双押比韵组对相等。
 * 返回按 frequency 排序的列表。
 */
public class RhymeMatcher {

    /** 词典条目内部结构 */
    private static class DictEntry {
        final String word;
        final int frequency;
        final String singleGroup;
        final String doubleGroup;

        DictEntry(String word, int frequency, String singleGroup, String doubleGroup) {
            this.word = word;
            this.frequency = frequency;
            this.singleGroup = singleGroup;
            this.doubleGroup = doubleGroup;
        }
    }

    private final List<DictEntry> entries = new ArrayList<>();
    /** 单押韵组 → 词列表 */
    private final Map<String, List<DictEntry>> singleIndex = new HashMap<>();
    /** 双押韵组对 → 词列表 */
    private final Map<String, List<DictEntry>> doubleIndex = new HashMap<>();

    private boolean loaded = false;

    /** 词典是否加载完成（2.5 万词在后台线程加载，UI 据此提示"加载中"） */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * 从 raw/rhyme_dict.json 加载词典，预计算每个词的单押韵组和双押韵组对。
     */
    public void loadDict(Context ctx) {
        if (loaded) return;
        entries.clear();
        singleIndex.clear();
        doubleIndex.clear();

        try {
            int resId = ctx.getResources().getIdentifier(
                    "rhyme_dict", "raw", ctx.getPackageName());
            InputStream is = ctx.getResources().openRawResource(resId);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String json = new String(buffer, "UTF-8");
            JSONArray arr = new JSONArray(json);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String word = obj.optString("word", "");
                int frequency = obj.optInt("frequency", 0);
                if (word.isEmpty()) continue;

                // 预计算单押韵组（末字）
                String singleGroup = PinyinHelper.getRhymeGroupOfWord(word);
                // 预计算双押韵组对（末两字）
                String doubleGroup = RhymeGroupMapper.getRhymeGroupPair(word);

                DictEntry entry = new DictEntry(word, frequency, singleGroup, doubleGroup);
                entries.add(entry);

                if (!singleGroup.isEmpty()) {
                    singleIndex.computeIfAbsent(singleGroup, k -> new ArrayList<>()).add(entry);
                }
                if (!doubleGroup.isEmpty()) {
                    doubleIndex.computeIfAbsent(doubleGroup, k -> new ArrayList<>()).add(entry);
                }
            }

            loaded = true;
        } catch (Exception e) {
            // 加载失败，保持空词典
            loaded = true;
        }
    }

    /**
     * 单押：取 inputWord 末字韵组，返回同韵组词按 frequency 降序。
     */
    public List<RhymeWord> findSingleRhymes(String inputWord) {
        if (!loaded || inputWord == null || inputWord.isEmpty()) return new ArrayList<>();

        String group = PinyinHelper.getRhymeGroupOfWord(inputWord);
        if (group.isEmpty()) return new ArrayList<>();

        List<RhymeWord> result = new ArrayList<>();
        List<DictEntry> candidates = singleIndex.get(group);
        if (candidates != null) {
            for (DictEntry entry : candidates) {
                if (!entry.word.equals(inputWord)) {
                    result.add(new RhymeWord(entry.word, group, entry.frequency));
                }
            }
        }

        Collections.sort(result, (a, b) -> b.frequency - a.frequency);
        return result;
    }

    /**
     * 双押：取 inputWord 末两字韵组对，返回韵组对相等的词，
     * 按 frequency 降序。
     */
    public List<RhymeWord> findDoubleRhymes(String inputWord) {
        if (!loaded || inputWord == null || inputWord.isEmpty()) return new ArrayList<>();

        String pair = RhymeGroupMapper.getRhymeGroupPair(inputWord);
        if (pair.isEmpty() || !pair.contains("-")) return new ArrayList<>();

        List<RhymeWord> result = new ArrayList<>();
        List<DictEntry> candidates = doubleIndex.get(pair);
        if (candidates != null) {
            for (DictEntry entry : candidates) {
                if (!entry.word.equals(inputWord)) {
                    result.add(new RhymeWord(entry.word, pair, entry.frequency));
                }
            }
        }

        Collections.sort(result, (a, b) -> b.frequency - a.frequency);
        return result;
    }
}
