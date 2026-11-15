package com.beefic.beekit.oss.aliyun;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.PresignOptions;
import com.aliyun.sdk.service.oss2.models.GetObjectRequest;
import com.aliyun.sdk.service.oss2.models.PresignResult;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * 阿里云 OSS 文件下载和预览签名器
 *
 * @author Vincent
 */
public class AliyunDownloadSigner {

    private final AliyunOssProperties properties;

    /**
     * 创建 GET 签名器
     *
     * @param properties OSS 配置
     */
    public AliyunDownloadSigner(AliyunOssProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    /**
     * 签发五分钟有效的原文件下载地址
     *
     * @param bucket 文件所在存储桶
     * @param key 文件对象 key
     * @param originalName 原始文件名
     * @return 签名下载地址
     */
    public String sign(String bucket, String key, String originalName) {
        AliyunOssSupport.requireArgument(bucket, "bucket");
        AliyunOssSupport.requireArgument(key, "key");
        AliyunOssSupport.requireArgument(originalName, "originalName");
        String filename;
        try {
            filename = URLEncoder.encode(originalName, StandardCharsets.UTF_8.name()).replace("+", "%20");
        } catch (UnsupportedEncodingException exception) {
            throw new AliyunOssException("Cannot encode OSS download filename", exception);
        }
        String disposition = "attachment; filename=\"download\"; filename*=UTF-8''" + filename;
        GetObjectRequest request = GetObjectRequest.newBuilder().bucket(bucket).key(key)
                .responseContentDisposition(disposition).build();
        return presign(request, properties.getEndpoint(), false);
    }

    /**
     * 使用绑定域名签发五分钟有效的预览地址
     *
     * @param bucket 文件所在存储桶
     * @param key 文件对象 key
     * @param process OSS 图片处理指令，可为 null
     * @return 签名预览地址
     */
    public String signPreview(String bucket, String key, String process) {
        AliyunOssSupport.requireArgument(bucket, "bucket");
        AliyunOssSupport.requireArgument(key, "key");
        GetObjectRequest.Builder builder = GetObjectRequest.newBuilder().bucket(bucket).key(key)
                .responseContentDisposition("inline").responseCacheControl("private, no-store");
        if (process != null) {
            builder.process(process);
        }
        GetObjectRequest request = builder.build();
        return presign(request, properties.getHost(), true);
    }

    /**
     * 使用指定域名签发短期 GET 地址
     *
     * @param request 文件读取请求
     * @param host 签名使用的域名
     * @param useCname 是否使用绑定域名
     * @return 签名地址
     */
    private String presign(GetObjectRequest request, String host, boolean useCname) {
        PresignOptions options = PresignOptions.newBuilder().expiration(Duration.ofMinutes(5)).build();
        try (OSSClient client = AliyunOssSupport.newClient(properties, host, useCname)) {
            PresignResult result = client.presign(request, options);
            return result.url();
        } catch (AliyunOssException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AliyunOssException("Cannot presign OSS GET request", exception);
        }
    }
}
