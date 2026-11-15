package com.beefic.beekit.oss.aliyun;

import com.aliyun.sdk.service.oss2.models.CallbackHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 阿里云 OSS POST V4 表单上传签名器
 *
 * @author Vincent
 */
public class AliyunPostV4Signer {

    private static final String SIGNATURE_VERSION = "OSS4-HMAC-SHA256";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'", Locale.ROOT)
            .withZone(ZoneOffset.UTC);
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AliyunOssProperties properties;
    private final Clock clock;

    /**
     * 创建上传签名器
     *
     * @param properties OSS 配置
     */
    public AliyunPostV4Signer(AliyunOssProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * 使用指定时钟创建上传签名器
     *
     * @param properties OSS 配置
     * @param clock 签名时钟
     */
    AliyunPostV4Signer(AliyunOssProperties properties, Clock clock) {
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = clock;
    }

    /**
     * 签发五分钟有效、禁止覆盖的上传授权
     *
     * @param key 完整对象 key
     * @param size 文件大小（字节）
     * @param mimeType 文件 MIME 类型
     * @return 上传表单签名字段
     */
    public AliyunPostV4SignResult sign(String key, long size, String mimeType) {
        return sign(key, size, mimeType, Collections.emptyMap(), null);
    }

    /**
     * 签发上传授权并绑定调用方指定的 OSS 表单回调
     *
     * @param key 完整对象 key
     * @param size 文件大小（字节）
     * @param mimeType 文件 MIME 类型
     * @param callbackVariables 自定义回调变量，变量名使用 x: 前缀
     * @param callbackBody 完整的回调请求体模板，无回调时传 null
     * @return 上传表单签名字段
     */
    public AliyunPostV4SignResult sign(String key, long size, String mimeType,
                                     Map<String, String> callbackVariables, String callbackBody) {
        AliyunOssSupport.requireCredentials(properties);
        AliyunOssSupport.requireConfiguration(properties.getEndpoint(), "endpoint");
        AliyunOssSupport.requireConfiguration(properties.getBucket(), "bucket");
        AliyunOssSupport.requireArgument(key, "key");
        AliyunOssSupport.requireArgument(mimeType, "mimeType");
        if (size < 0) {
            throw new IllegalArgumentException("OSS upload size must not be negative");
        }

        Map<String, String> variables = new LinkedHashMap<>(
                Objects.requireNonNull(callbackVariables, "callbackVariables"));
        variables.forEach((name, value) -> {
            if (name == null || !name.startsWith("x:") || name.length() == 2 || value == null) {
                throw new IllegalArgumentException("Invalid OSS callback variable");
            }
        });
        String callback = encodeCallback(callbackBody, variables);
        Instant now = clock.instant();
        Instant expiration = now.plusSeconds(300);
        String date = DATE_FORMAT.format(now);
        String timestamp = TIME_FORMAT.format(now);
        String credential = properties.getAccessKey() + "/" + date + "/" + properties.getRegion()
                + "/oss/aliyun_v4_request";

        List<Object> conditions = new ArrayList<>();
        conditions.add(Collections.singletonMap("bucket", properties.getBucket()));
        conditions.add(Collections.singletonMap("x-oss-signature-version", SIGNATURE_VERSION));
        conditions.add(Collections.singletonMap("x-oss-credential", credential));
        conditions.add(Collections.singletonMap("x-oss-date", timestamp));
        conditions.add(Arrays.asList("eq", "$key", key));
        conditions.add(Arrays.asList("content-length-range", size, size));
        conditions.add(Arrays.asList("eq", "$content-type", mimeType));
        conditions.add(Collections.singletonMap("x-oss-forbid-overwrite", "true"));
        if (callback != null) {
            conditions.add(Collections.singletonMap("callback", callback));
            variables.forEach((name, value) -> conditions.add(Collections.singletonMap(name, value)));
        }

        Map<String, Object> policyDocument = new LinkedHashMap<>();
        policyDocument.put("expiration", DateTimeFormatter.ISO_INSTANT.format(expiration));
        policyDocument.put("conditions", conditions);
        String policy;
        try {
            byte[] json = OBJECT_MAPPER.writeValueAsBytes(policyDocument);
            policy = Base64.getEncoder().encodeToString(json);
        } catch (JsonProcessingException exception) {
            throw new AliyunOssException("Cannot encode OSS upload policy", exception);
        }

        byte[] signingKey = hmac(("aliyun_v4" + properties.getAccessSecret()).getBytes(StandardCharsets.UTF_8), date);
        signingKey = hmac(signingKey, properties.getRegion());
        signingKey = hmac(signingKey, "oss");
        signingKey = hmac(signingKey, "aliyun_v4_request");
        byte[] digest = hmac(signingKey, policy);
        char[] signature = new char[digest.length * 2];
        for (int i = 0; i < digest.length; i++) {
            int value = digest[i] & 0xff;
            signature[2 * i] = HEX_DIGITS[value >>> 4];
            signature[2 * i + 1] = HEX_DIGITS[value & 0xf];
        }

        String endpoint = AliyunOssSupport.normalizeHost(properties.getEndpoint());
        String host = "https://" + properties.getBucket() + "." + endpoint;
        return new AliyunPostV4SignResult(key, host, properties.getAccessKey(), policy, new String(signature),
                SIGNATURE_VERSION, credential, timestamp, Long.toString(expiration.getEpochSecond()),
                callback, Collections.unmodifiableMap(variables), mimeType);
    }

    /**
     * 生成 SDK 规定的回调表单字段
     *
     * @param body 回调请求体模板
     * @param variables 回调变量
     * @return Base64 回调字段，未启用回调时为 null
     */
    private String encodeCallback(String body, Map<String, String> variables) {
        if (body == null) {
            if (!variables.isEmpty()) {
                throw new IllegalArgumentException("OSS callback variables require a callback body");
            }
            return null;
        }
        AliyunOssSupport.requireArgument(body, "callbackBody");
        AliyunOssSupport.requireConfiguration(properties.getCallback(), "callback");
        CallbackHelper callback = CallbackHelper.newBuilder().callbackUrl(properties.getCallback())
                .callbackBody(body).callbackBodyType(CallbackHelper.CallbackBodyType.URL_ENCODED).build();
        String encoded = callback.toCallbackParameter();
        if (encoded == null) {
            throw new AliyunOssException("Cannot encode OSS callback");
        }
        return encoded;
    }

    /**
     * 计算 HMAC-SHA256
     *
     * @param key 密钥
     * @param content 签名内容
     * @return 原始摘要字节
     */
    private byte[] hmac(byte[] key, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new AliyunOssException("Cannot sign OSS upload policy", exception);
        }
    }
}
