package com.example.demo.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 人脸特征匹配工具类（用于 face-api.js 的 128 维 descriptor）
 *
 * - descriptorJson: 数据库存的 JSON 数组字符串，例如 "[0.0123, -0.0456, ...]"
 * - inputDescriptor: 前端上传的 descriptor（double[] / List<Double>）
 *
 * 说明：
 * face-api.js 的距离通常用欧氏距离（euclidean distance），越小越相似。
 */
public final class FaceMatchUtils {

    private FaceMatchUtils() {}

    /** face-api.js 常用阈值范围大约 0.4~0.6，项目里可以根据误识别/漏识别调参 */
    public static final double DEFAULT_THRESHOLD = 0.55;

    /** face-api.js descriptor 维度 */
    public static final int DESCRIPTOR_DIM = 128;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 将数据库中的 JSON 数组字符串解析为 double[]。
     * 例："[0.01, -0.02, ...]"
     */
    public static double[] parseDescriptorJson(String descriptorJson) {
        if (descriptorJson == null || descriptorJson.trim().isEmpty()) {
            return null;
        }
        try {
            List<Double> list = MAPPER.readValue(descriptorJson, new TypeReference<List<Double>>() {});
            return toDoubleArray(list);
        } catch (Exception e) {
            // 解析失败就返回 null，避免业务层直接炸
            return null;
        }
    }

    /**
     * 将 List<Double> 转成 double[]，并做基础校验。
     */
    public static double[] toDoubleArray(List<Double> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            Double v = list.get(i);
            arr[i] = (v == null ? 0.0 : v);
        }
        // 维度不对也允许返回，但建议业务层做严格校验
        return arr;
    }

    /**
     * 对输入 descriptor 做维度校验（建议在 controller/service 入口处调用）
     */
    public static boolean isValidDescriptor(double[] descriptor) {
        return descriptor != null && descriptor.length == DESCRIPTOR_DIM;
    }

    /**
     * 计算欧氏距离：sqrt(sum((a[i]-b[i])^2))
     * 距离越小越相似。
     *
     * @return 如果输入不合法返回 Double.POSITIVE_INFINITY
     */
    public static double euclideanDistance(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }

    /**
     * 判断是否匹配（用默认阈值）
     */
    public static boolean isMatch(double[] inputDescriptor, double[] storedDescriptor) {
        return isMatch(inputDescriptor, storedDescriptor, DEFAULT_THRESHOLD);
    }

    /**
     * 判断是否匹配（自定义阈值）
     */
    public static boolean isMatch(double[] inputDescriptor, double[] storedDescriptor, double threshold) {
        double dist = euclideanDistance(inputDescriptor, storedDescriptor);
        return dist < threshold;
    }

    /**
     * 返回距离（输入 vs 数据库存 JSON）
     * @return 解析失败/不合法返回 POSITIVE_INFINITY
     */
    public static double distanceToStoredJson(double[] inputDescriptor, String storedDescriptorJson) {
        double[] stored = parseDescriptorJson(storedDescriptorJson);
        if (stored == null) return Double.POSITIVE_INFINITY;
        return euclideanDistance(inputDescriptor, stored);
    }

    /**
     * 给一堆 descriptorJson（比如数据库里取出来的字符串），找最小距离及索引。
     * 你在 controller/service 里遍历用户时可以用这个返回最优匹配值。
     */
    public static BestMatchResult findBestMatch(double[] inputDescriptor, List<String> storedDescriptorJsonList) {
        if (inputDescriptor == null || storedDescriptorJsonList == null || storedDescriptorJsonList.isEmpty()) {
            return BestMatchResult.notFound();
        }
        int bestIndex = -1;
        double bestDistance = Double.POSITIVE_INFINITY;

        for (int i = 0; i < storedDescriptorJsonList.size(); i++) {
            String json = storedDescriptorJsonList.get(i);
            double dist = distanceToStoredJson(inputDescriptor, json);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestIndex = i;
            }
        }

        if (bestIndex == -1) {
            return BestMatchResult.notFound();
        }
        return new BestMatchResult(true, bestIndex, bestDistance);
    }

    /**
     * 最优匹配结果结构
     */
    public static class BestMatchResult {
        private final boolean found;
        private final int bestIndex;
        private final double bestDistance;

        public BestMatchResult(boolean found, int bestIndex, double bestDistance) {
            this.found = found;
            this.bestIndex = bestIndex;
            this.bestDistance = bestDistance;
        }

        public static BestMatchResult notFound() {
            return new BestMatchResult(false, -1, Double.POSITIVE_INFINITY);
        }

        public boolean isFound() {
            return found;
        }

        public int getBestIndex() {
            return bestIndex;
        }

        public double getBestDistance() {
            return bestDistance;
        }

        @Override
        public String toString() {
            return "BestMatchResult{" +
                    "found=" + found +
                    ", bestIndex=" + bestIndex +
                    ", bestDistance=" + bestDistance +
                    '}';
        }
    }
}
