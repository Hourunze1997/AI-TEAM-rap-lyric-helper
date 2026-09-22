package com.rap.lyrichelper.rhyme;

import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 封装 Pinyin4j，提取汉字韵母并交给 RhymeGroupMapper 转换韵组。
 * 不保留 normalizeRhyme 方法，避免与韵组逻辑冲突。
 */
public class PinyinHelper {

    private static final HanyuPinyinOutputFormat FORMAT;

    static {
        FORMAT = new HanyuPinyinOutputFormat();
        FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        FORMAT.setVCharType(HanyuPinyinVCharType.WITH_U_UNICODE);
    }

    /**
     * 用 Pinyin4j 获取汉字韵母，无拼音返回空串。
     */
    public static String getFinalOfChar(char c) {
        String pinyin = getPinyin(c);
        if (pinyin == null || pinyin.isEmpty()) return "";
        return RhymeGroupMapper.getFinal(pinyin);
    }

    /**
     * 取词末字韵组 ID。
     */
    public static String getRhymeGroupOfWord(String word) {
        if (word == null || word.isEmpty()) return "";
        char lastChar = word.charAt(word.length() - 1);
        String finalStr = getFinalOfChar(lastChar);
        return RhymeGroupMapper.getRhymeGroup(finalStr);
    }

    /**
     * 取词末两字韵组对（双押用）。
     */
    public static String getRhymeGroupPairOfWord(String word) {
        return RhymeGroupMapper.getRhymeGroupPair(word);
    }

    /** 用 Pinyin4j 获取汉字拼音 */
    private static String getPinyin(char c) {
        try {
            String[] arr = net.sourceforge.pinyin4j.PinyinHelper
                    .toHanyuPinyinStringArray(c, FORMAT);
            if (arr != null && arr.length > 0) return arr[0];
        } catch (BadHanyuPinyinOutputFormatCombination e) {
            // 忽略格式错误
        }
        return "";
    }
}
