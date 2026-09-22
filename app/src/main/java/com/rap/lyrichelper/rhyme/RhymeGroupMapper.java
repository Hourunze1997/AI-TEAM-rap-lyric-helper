package com.rap.lyrichelper.rhyme;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.HashMap;
import java.util.Map;

/**
 * 韵组映射核心：维护韵母→韵组ID的静态 Map。
 * ang/iang/uang→ANG, eng/ing→ENG, an/ian/uan/üan→AN, en/in/un/ün→EN,
 * ao/iao→AO, ou/iu→OU, ai/uai→AI, ei/ui→EI,
 * a/ia/ua→A, o/uo→O, e/ie/üe→E, i→I, u→U, ü→V
 */
public class RhymeGroupMapper {

    private static final Map<String, String> FINAL_TO_GROUP = new HashMap<>();

    static {
        // ang 韵组（ang/iang/uang 互换）
        FINAL_TO_GROUP.put("ang", "ANG");
        FINAL_TO_GROUP.put("iang", "ANG");
        FINAL_TO_GROUP.put("uang", "ANG");

        // eng 韵组
        FINAL_TO_GROUP.put("eng", "ENG");
        FINAL_TO_GROUP.put("ing", "ENG");

        // an 韵组
        FINAL_TO_GROUP.put("an", "AN");
        FINAL_TO_GROUP.put("ian", "AN");
        FINAL_TO_GROUP.put("uan", "AN");
        FINAL_TO_GROUP.put("üan", "AN");

        // en 韵组
        FINAL_TO_GROUP.put("en", "EN");
        FINAL_TO_GROUP.put("in", "EN");
        FINAL_TO_GROUP.put("un", "EN");
        FINAL_TO_GROUP.put("ün", "EN");

        // ao 韵组
        FINAL_TO_GROUP.put("ao", "AO");
        FINAL_TO_GROUP.put("iao", "AO");

        // ou 韵组
        FINAL_TO_GROUP.put("ou", "OU");
        FINAL_TO_GROUP.put("iu", "OU");

        // ai 韵组
        FINAL_TO_GROUP.put("ai", "AI");
        FINAL_TO_GROUP.put("uai", "AI");

        // ei 韵组
        FINAL_TO_GROUP.put("ei", "EI");
        FINAL_TO_GROUP.put("ui", "EI");

        // a 韵组
        FINAL_TO_GROUP.put("a", "A");
        FINAL_TO_GROUP.put("ia", "A");
        FINAL_TO_GROUP.put("ua", "A");

        // o 韵组
        FINAL_TO_GROUP.put("o", "O");
        FINAL_TO_GROUP.put("uo", "O");

        // e 韵组
        FINAL_TO_GROUP.put("e", "E");
        FINAL_TO_GROUP.put("ie", "E");
        FINAL_TO_GROUP.put("üe", "E");

        // i 韵组
        FINAL_TO_GROUP.put("i", "I");

        // u 韵组
        FINAL_TO_GROUP.put("u", "U");

        // ü 韵组
        FINAL_TO_GROUP.put("ü", "V");
    }

    private static final HanyuPinyinOutputFormat PINYIN_FORMAT;

    static {
        PINYIN_FORMAT = new HanyuPinyinOutputFormat();
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        PINYIN_FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    /**
     * 根据拼音韵母返回韵组 ID（如 ang→ANG, iang→ANG, eng→ENG）。
     */
    public static String getRhymeGroup(String pinyinFinal) {
        if (pinyinFinal == null || pinyinFinal.isEmpty()) return "";
        String group = FINAL_TO_GROUP.get(pinyinFinal);
        return group != null ? group : "";
    }

    /**
     * 从完整拼音中提取韵母部分。
     * 如 "meng"→"eng", "xiang"→"iang", "an"→"an"。
     */
    public static String getFinal(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) return "";

        String s = pinyin.trim().toLowerCase();

        // 去除声调数字
        s = s.replaceAll("[1-5]", "");

        // 去除声调符号，转为基本字母
        s = removeToneMarks(s);

        // u: → ü, v → ü
        s = s.replace("u:", "ü").replace("v", "ü");

        // 去除声母
        if (s.startsWith("zh") || s.startsWith("ch") || s.startsWith("sh")) {
            s = s.substring(2);
        } else if (s.length() > 1 && "bpmfdtnlgkhjqxrzcsyw".indexOf(s.charAt(0)) >= 0) {
            s = s.substring(1);
        }

        return s;
    }

    /**
     * 取词末两字韵组，用 '-' 拼接成韵组对，如 '梦想'→'ENG-ANG'。
     * ang/iang/uang 已归并同组，所以可互换匹配。
     */
    public static String getRhymeGroupPair(String word) {
        if (word == null || word.isEmpty()) return "";

        int len = word.length();
        if (len == 1) {
            return getRhymeGroupOfChar(word.charAt(0));
        }

        // 取末两字
        char c1 = word.charAt(len - 2);
        char c2 = word.charAt(len - 1);

        String g1 = getRhymeGroupOfChar(c1);
        String g2 = getRhymeGroupOfChar(c2);

        if (g1.isEmpty() || g2.isEmpty()) return "";
        return g1 + "-" + g2;
    }

    /** 获取单个汉字的韵组（内部使用 Pinyin4j） */
    private static String getRhymeGroupOfChar(char c) {
        String pinyin = getPinyinOfChar(c);
        if (pinyin == null || pinyin.isEmpty()) return "";
        String finalStr = getFinal(pinyin);
        return getRhymeGroup(finalStr);
    }

    /** 用 Pinyin4j 获取汉字拼音（无声调） */
    private static String getPinyinOfChar(char c) {
        try {
            String[] pinyinArray = net.sourceforge.pinyin4j.PinyinHelper
                    .toHanyuPinyinStringArray(c, PINYIN_FORMAT);
            if (pinyinArray != null && pinyinArray.length > 0) {
                return pinyinArray[0];
            }
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            // 忽略格式错误
        }
        return "";
    }

    /** 去除拼音声调符号 */
    private static String removeToneMarks(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case 'ā': case 'á': case 'ǎ': case 'à': sb.append('a'); break;
                case 'ē': case 'é': case 'ě': case 'è': sb.append('e'); break;
                case 'ī': case 'í': case 'ǐ': case 'ì': sb.append('i'); break;
                case 'ō': case 'ó': case 'ǒ': case 'ò': sb.append('o'); break;
                case 'ū': case 'ú': case 'ǔ': case 'ù': sb.append('u'); break;
                case 'ǖ': case 'ǘ': case 'ǚ': case 'ǜ': sb.append('ü'); break;
                case 'ń': case 'ň': case 'ǹ': sb.append('n'); break;
                case 'ḿ': sb.append('m'); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }
}
