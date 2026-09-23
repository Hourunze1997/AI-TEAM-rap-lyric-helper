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
        /** 末字声调：1-4 四声、5 轻声、0 未知 */
        final int tone;

        DictEntry(String word, int frequency, String singleGroup, String doubleGroup, int tone) {
            this.word = word;
            this.frequency = frequency;
            this.singleGroup = singleGroup;
            this.doubleGroup = doubleGroup;
            this.tone = tone;
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

                // 预计算单押韵组（末字）与声调（同调优先排序用）
                String singleGroup = PinyinHelper.getRhymeGroupOfWord(word);
                // 预计算双押韵组对（末两字）
                String doubleGroup = RhymeGroupMapper.getRhymeGroupPair(word);
                int tone = PinyinHelper.getToneOfWord(word);

                DictEntry entry = new DictEntry(word, frequency, singleGroup, doubleGroup, tone);
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
     * 单押：取 inputWord 末字韵组，返回同韵组词——同声调优先，其余按词频。
     * RhymeWord 的第二行文案带声调说明（"同韵·同调"/"同韵·二声"）。
     */
    public List<RhymeWord> findSingleRhymes(String inputWord) {
        if (!loaded || inputWord == null || inputWord.isEmpty()) return new ArrayList<>();

        String group = PinyinHelper.getRhymeGroupOfWord(inputWord);
        if (group.isEmpty()) return new ArrayList<>();

        List<RhymeWord> result = new ArrayList<>();
        List<DictEntry> candidates = singleIndex.get(group);
        if (candidates != null) {
            final int inTone = PinyinHelper.getToneOfWord(inputWord);
            List<DictEntry> sorted = new ArrayList<>(candidates);
            Collections.sort(sorted, (a, b) -> {
                boolean ta = inTone != 0 && a.tone == inTone;
                boolean tb = inTone != 0 && b.tone == inTone;
                if (ta != tb) return ta ? -1 : 1;
                return b.frequency - a.frequency;
            });
            for (DictEntry entry : sorted) {
                if (!entry.word.equals(inputWord)) {
                    result.add(new RhymeWord(entry.word,
                            toneNote(inTone, entry.tone), entry.frequency));
                }
            }
        }
        return result;
    }

    /**
     * 双押：取 inputWord 末两字韵组对，返回韵组对相等的词，
     * 同声调优先，其余按词频。
     */
    public List<RhymeWord> findDoubleRhymes(String inputWord) {
        if (!loaded || inputWord == null || inputWord.isEmpty()) return new ArrayList<>();

        String pair = RhymeGroupMapper.getRhymeGroupPair(inputWord);
        if (pair.isEmpty() || !pair.contains("-")) return new ArrayList<>();

        List<RhymeWord> result = new ArrayList<>();
        List<DictEntry> candidates = doubleIndex.get(pair);
        if (candidates != null) {
            final int inTone = PinyinHelper.getToneOfWord(inputWord);
            List<DictEntry> sorted = new ArrayList<>(candidates);
            Collections.sort(sorted, (a, b) -> {
                boolean ta = inTone != 0 && a.tone == inTone;
                boolean tb = inTone != 0 && b.tone == inTone;
                if (ta != tb) return ta ? -1 : 1;
                return b.frequency - a.frequency;
            });
            for (DictEntry entry : sorted) {
                if (!entry.word.equals(inputWord)) {
                    result.add(new RhymeWord(entry.word,
                            "双押·" + toneNote(inTone, entry.tone), entry.frequency));
                }
            }
        }
        return result;
    }

    /** 第二行文案：同调标"同调"，否则标对方声调 */
    private static String toneNote(int inTone, int entryTone) {
        if (inTone != 0 && entryTone == inTone) return "同韵·同调";
        String name;
        switch (entryTone) {
            case 1: name = "一声"; break;
            case 2: name = "二声"; break;
            case 3: name = "三声"; break;
            case 4: name = "四声"; break;
            case 5: name = "轻声"; break;
            default: name = ""; break;
        }
        return name.isEmpty() ? "同韵" : "同韵·" + name;
    }
}
