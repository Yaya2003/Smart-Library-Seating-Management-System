package com.example.common.util;

public class ArrayUtil {
    public static int[][] adjustArraySize(int[][] original, int newRows, int newCols) {
        int[][] newArray = new int[newRows][newCols];

        // 遍历原数组，将数据复制到新数组，同时填充多余的部分为 -1
        for (int i = 0; i < newRows; i++) {
            for (int j = 0; j < newCols; j++) {
                if (i < original.length && j < original[i].length) {
                    newArray[i][j] = original[i][j]; // 复制原数组的值
                } else {
                    newArray[i][j] = -1; // 填充多余的部分
                }
            }
        }

        return newArray;
    }

    public static void copy2DArray(int[][] source, int[][] target) {
        for (int i = 0; i < Integer.min(source.length, target.length); i++) {
            System.arraycopy(source[i], 0, target[i], 0, source[i].length);
        }
    }
}
