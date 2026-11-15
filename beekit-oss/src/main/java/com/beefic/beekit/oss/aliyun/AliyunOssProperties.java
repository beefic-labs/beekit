package com.beefic.beekit.oss.aliyun;

/**
 * OSS 配置，可由应用组装或绑定
 *
 * @author Vincent
 */
public class AliyunOssProperties {

    private String accessKey;
    private String accessSecret;
    private String endpoint;
    private String host;
    private String region;
    private String bucket;
    private String callback;

    /**
     * @return 访问密钥 ID
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * 设置访问密钥 ID
     *
     * @param accessKey 访问密钥 ID
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * @return 访问密钥 Secret
     */
    public String getAccessSecret() {
        return accessSecret;
    }

    /**
     * 设置访问密钥 Secret
     *
     * @param accessSecret 访问密钥 Secret
     */
    public void setAccessSecret(String accessSecret) {
        this.accessSecret = accessSecret;
    }

    /**
     * @return OSS 服务端点
     */
    public String getEndpoint() {
        return endpoint;
    }

    /**
     * 设置OSS 服务端点
     *
     * @param endpoint OSS 服务端点
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * @return 可选的预览绑定域名
     */
    public String getHost() {
        return host;
    }

    /**
     * 设置可选的预览绑定域名
     *
     * @param host 可选的预览绑定域名
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return 存储区域
     */
    public String getRegion() {
        return region;
    }

    /**
     * 设置存储区域
     *
     * @param region 存储区域
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return 默认存储桶
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 设置默认存储桶
     *
     * @param bucket 默认存储桶
     */
    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    /**
     * @return 可选的上传回调地址
     */
    public String getCallback() {
        return callback;
    }

    /**
     * 设置可选的上传回调地址
     *
     * @param callback 可选的上传回调地址
     */
    public void setCallback(String callback) {
        this.callback = callback;
    }
}
