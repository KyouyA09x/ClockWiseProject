package com.example.mainactivity;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static String fromBooleanArray(boolean[] array) {
        if (array == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i] ? "1" : "0");
            if (i < array.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    @TypeConverter
    public static boolean[] toBooleanArray(String data) {
        if (data == null) return new boolean[7];
        String[] parts = data.split(",");
        boolean[] array = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            array[i] = parts[i].equals("1");
        }
        return array;
    }
}

