package com.beefic.beekit.oss.aliyun;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import java.util.Objects;

/**
 * 阿里云 OSS 服务端对象操作客户端
 *
 * @author Vincent
 */
public final class AliyunOssClient implements AutoCloseable {

    private final OSSClient client;

    /**
     * 创建客户端，可复用同一实例处理多个请求
     *
     * @param properties OSS 配置
     */
    public AliyunOssClient(AliyunOssProperties properties) {
        Objects.requireNonNull(properties, "properties");
        client = AliyunOssSupport.newClient(properties, properties.getEndpoint(), false);
    }

    /**
     * 使用给定的 SDK 客户端
     *
     * @param client SDK 客户端
     */
    AliyunOssClient(OSSClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    /**
     * 通过 OSS 删除指定对象，版本化存储桶按 OSS 默认删除语义处理
     *
     * @param bucket 对象所在存储桶
     * @param key 对象 key
     */
    public void deleteObject(String bucket, String key) {
        AliyunOssSupport.requireArgument(bucket, "bucket");
        AliyunOssSupport.requireArgument(key, "key");
        DeleteObjectRequest request = DeleteObjectRequest.newBuilder().bucket(bucket).key(key).build();
        try {
            client.deleteObject(request);
        } catch (RuntimeException exception) {
            throw new AliyunOssException("Cannot delete OSS object", exception);
        }
    }

    /**
     * 释放 SDK 资源
     */
    @Override
    public void close() {
        try {
            client.close();
        } catch (Exception exception) {
            throw new AliyunOssException("Cannot close OSS client", exception);
        }
    }
}
