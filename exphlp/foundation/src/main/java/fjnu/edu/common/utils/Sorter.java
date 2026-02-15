package fjnu.edu.common.utils;

import java.util.List;

public class Sorter {
    /**
     * value数组从小至大顺序对应的下标列表，
     * 但不修改values而是给出正序排序后下标列表
     * @param values 值数组
     * @return
     */
    public static int[] sort(double values[]) {
        int len = values.length;
        int idxs[] = new int[len];
        for (int i = 0; i < len; i++) {
            idxs[i] = i;
        }
        for (int j = 0; j < len; j++) {
            double minVal = values[idxs[j]];
            for (int k = j + 1; k < len; k++) {
                double curVal = values[idxs[k]];
                if (Double.compare(curVal, minVal) < 0) {
                    minVal = curVal;
                    //交换
                    int tempIdx = idxs[k];
                    idxs[k] = idxs[j];
                    idxs[j] = tempIdx;
                }
            }
        }
        return idxs;
    }

    /**
     * value列表元素值从小至大顺序对应的下标列表，
     * 但不修改values而是给出正序排序后下标列表
     * @param values 值列表
     * @return
     */
    public static int[] sort(List<Double> values) {
        int len = values.size();
        int idxs[] = new int[len];
        for (int i = 0; i < len; i++) {
            idxs[i] = i;
        }
        for (int j = 0; j < len; j++) {

            double minVal = values.get(idxs[j]);
            for (int k = j + 1; k < len; k++) {
                double curVal = values.get(idxs[k]);
                if (Double.compare(curVal, minVal) < 0) {
                    minVal = curVal;
                    //交换
                    int tempIdx = idxs[k];
                    idxs[k] = idxs[j];
                    idxs[j] = tempIdx;
                }
            }
        }
        return idxs;
    }
}
