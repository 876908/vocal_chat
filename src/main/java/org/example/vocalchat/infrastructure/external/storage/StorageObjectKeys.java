package org.example.vocalchat.infrastructure.external.storage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
// 私有构造器，禁止实例化，所有方法均为静态工具方法
public final class StorageObjectKeys {
    //20260703 生成日期目录，按天分区管理
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private StorageObjectKeys() {
    }
    //清洗并规范化对象键
    public static String normalize(String objectKey) {
        if (!hasText(objectKey)) { 
            throw new IllegalArgumentException("objectKey must not be blank");
        }
        //去除收尾空白，将反斜杠替换为正斜杠，兼容Unix系统
        String normalized = objectKey.trim().replace('\\', '/');
        // 移除开头的/，防止产生绝对路径形式,对象存储所有key都是相对路径
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        // 检查是否非空且不为\0，防止注入攻击
        if (!hasText(normalized) || normalized.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("objectKey is invalid");
        }

        return normalized;
    }
    // 将多个路径片段拼接成一个完整的对象键
    public static String join(String... parts) {
        String joined = Arrays.stream(parts)
                .filter(StorageObjectKeys::hasText)
                .map(StorageObjectKeys::normalize)
                .collect(Collectors.joining("/"));
        return normalize(joined);
    }
    //生成一个带随机UUID的对象键  uploads/20260723/550e8400-e29b-41d4-a716-446655440000.jpg
    public static String randomKey(String prefix, String originalFilename) {
        String safeFilename = safeFilename(originalFilename);
        String extension = extensionOf(safeFilename);
        return join(prefix, LocalDate.now().format(DATE_FORMATTER), UUID.randomUUID() + extension);
    }
    //生成对象键的完整公共访问url https://cdn.example.com/uploads/20260723/xxx.jpg
    public static String publicUrl(String publicEndpoint, String objectKey) {
        if (!hasText(publicEndpoint)) {
            return null;
        }
        return trimTrailingSlash(publicEndpoint.trim()) + "/" + normalize(objectKey);
    }

    private static String safeFilename(String originalFilename) {
        if (!hasText(originalFilename)) {
            return "file";
        }

        String normalized = originalFilename.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/'); //去掉最后一个/之前的部分
        String filename = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
        String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");  //只保留字母，数字，点，下划线，其余替换为_
        return hasText(safe) ? safe : "file"; //结果为空返回file
    }
    // 提取文件扩展名
    private static String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index <= 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index);
    }
    // 循环移除字符串末尾所有的/
    private static String trimTrailingSlash(String value) {
        String trimmed = value;
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
    // 去空格后非空
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
