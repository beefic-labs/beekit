package com.beefic.beekit.oss.aliyun;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.credentials.StaticCredentialsProvider;

/**
 * OSS 配置校验与客户端构造
 *
 * @author Vincent
 */
final class AliyunOssSupport {

    /**
     * 内部工具无需实例化
     */
    private AliyunOssSupport() {
    }

    /**
     * 检查必需的配置字段
     *
     * @param value 配置值
     * @param name 配置名
     */
    static void requireConfiguration(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new AliyunOssException("Missing OSS configuration: " + name);
        }
    }

    /**
     * 检查必需的调用参数
     *
     * @param value 参数值
     * @param name 参数名
     */
    static void requireArgument(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing OSS argument: " + name);
        }
    }

    /**
     * 校验签名和客户端公用配置
     *
     * @param properties OSS 配置
     */
    static void requireCredentials(AliyunOssProperties properties) {
        requireConfiguration(properties.getAccessKey(), "accessKey");
        requireConfiguration(properties.getAccessSecret(), "accessSecret");
        requireConfiguration(properties.getRegion(), "region");
    }

    /**
     * 提取 OSS 域名
     *
     * @param host OSS 服务端点或自定义域名
     * @return 不含协议和末尾斜杠的域名
     */
    static String normalizeHost(String host) {
        return host.trim().replaceFirst("(?i)^https?://", "").replaceAll("/+$", "");
    }

    /**
     * 创建指定端点的阿里云 SDK 客户端
     *
     * @param properties OSS 配置
     * @param host OSS 服务端点或自定义域名
     * @param useCname 是否使用绑定域名
     * @return OSS 客户端
     */
    static OSSClient newClient(AliyunOssProperties properties, String host, boolean useCname) {
        requireCredentials(properties);
        requireConfiguration(host, useCname ? "host" : "endpoint");
        StaticCredentialsProvider credentials = new StaticCredentialsProvider(properties.getAccessKey(),
                properties.getAccessSecret());
        String endpoint = "https://" + normalizeHost(host);
        return OSSClient.newBuilder().region(properties.getRegion()).endpoint(endpoint)
                .useCName(useCname).credentialsProvider(credentials).build();
    }
}
