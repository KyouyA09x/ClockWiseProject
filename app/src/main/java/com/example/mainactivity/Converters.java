package com.example.mainactivity;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static String fromBooleanArray(boolean[] booleans) {
        if (booleans == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (boolean b : booleans) {
            sb.append(b ? "1" : "0");
        }
        return sb.toString();
    }

    @TypeConverter
    public static boolean[] toBooleanArray(String booleans) {
        if (booleans == null) {
            return null;
        }
        boolean[] result = new boolean[booleans.length()];
        for (int i = 0; i < booleans.length(); i++) {
            result[i] = booleans.charAt(i) == '1';
        }
        return result;
    }
}
