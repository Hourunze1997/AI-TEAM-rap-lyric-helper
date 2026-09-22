package com.rap.lyrichelper.data.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Room 类型转换器：List<String> 与 String 互转（tags 字段）。
 */
public class Converters {

    @TypeConverter
    public static List<String> fromString(String value) {
        List<String> list = new ArrayList<>();
        if (value == null || value.isEmpty()) return list;
        String[] parts = value.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    @TypeConverter
    public static String toString(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
