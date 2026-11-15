package com.beefic.beekit.oss.aliyun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 上传、下载及预览签名的离线测试
 *
 * @author Vincent
 */
class AliyunSigningTest {

    private final AliyunOssProperties properties = properties();

    /**
     * 固定时钟校对 V4 签名，并检查上传限制
     *
     * @throws Exception 无法解析策略 JSON
     */
    @Test
    void signsUploadWithExpectedPolicyAndHmac() throws Exception {
        AliyunPostV4SignResult result = uploadSigner().sign("files/a.pdf", 123, "application/pdf");
        JsonNode policy = decodeJson(result.getPolicy());
        assertEquals("2026-10-23T09:00:00Z", policy.get("expiration").asText());
        assertEquals("OSS4-HMAC-SHA256", result.getSignatureVersion());
        assertEquals("20261023T085500Z", result.getDate());
        assertEquals("test-key/20261023/cn-hangzhou/oss/aliyun_v4_request", result.getCredential());
        assertEquals("1018fede38bc78998b7426cc4a1a1b283361995b9a278e306fcfc6218e3eebed", result.getSignature());
        assertEquals("https://test-bucket.oss-cn-hangzhou.aliyuncs.com", result.getHost());
        assertEquals("[\"eq\",\"$key\",\"files/a.pdf\"]", policy.get("conditions").get(4).toString());
        assertEquals("[\"content-length-range\",123,123]", policy.get("conditions").get(5).toString());
        assertEquals("[\"eq\",\"$content-type\",\"application/pdf\"]", policy.get("conditions").get(6).toString());
        assertEquals("true", policy.get("conditions").get(7).get("x-oss-forbid-overwrite").asText());
        assertNull(result.getCallback());
        assertTrue(result.getCallbackVar().isEmpty());
    }

    /**
     * 回调 URL、完整模板以及调用方变量进入授权策略
     *
     * @throws Exception 无法解析回调或策略 JSON
     */
    @Test
    void bindsOptionalCallbackAndVariables() throws Exception {
        properties.setCallback("https://api.example.test/oss/callback");
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("x:request_id", "req-1");
        String body = "requestId=${x:request_id}&object=${object}";
        AliyunPostV4SignResult result = uploadSigner().sign("files/a.pdf", 123, "application/pdf", variables, body);
        variables.put("x:request_id", "changed");
        JsonNode callback = decodeJson(result.getCallback());
        JsonNode conditions = decodeJson(result.getPolicy()).get("conditions");
        assertEquals(properties.getCallback(), callback.get("callbackUrl").asText());
        assertEquals(body, callback.get("callbackBody").asText());
        assertEquals("application/x-www-form-urlencoded", callback.get("callbackBodyType").asText());
        assertEquals(result.getCallback(), conditions.get(8).get("callback").asText());
        assertEquals("req-1", conditions.get(9).get("x:request_id").asText());
        assertEquals("req-1", result.getCallbackVar().get("x:request_id"));
    }

    /**
     * 验证缺少配置以及非法上传参数被拒绝
     */
    @Test
    void rejectsInvalidSigningInputs() {
        AliyunPostV4Signer signer = uploadSigner();
        assertThrows(IllegalArgumentException.class, () -> signer.sign("key", -1, "image/png"));
        assertThrows(AliyunOssException.class,
                () -> signer.sign("key", 1, "image/png", new LinkedHashMap<>(), "object=${object}"));
        AliyunDownloadSigner downloadSigner = new AliyunDownloadSigner(new AliyunOssProperties());
        assertThrows(AliyunOssException.class, () -> downloadSigner.sign("bucket", "key", "file.pdf"));
    }

    /**
     * 下载 URL 使用原始文件名，预览 URL 使用绑定域名并按需附加图片处理
     *
     * @throws Exception 无法解码 URL 查询参数
     */
    @Test
    void preservesDownloadAndPreviewUrlBehavior() throws Exception {
        AliyunDownloadSigner signer = new AliyunDownloadSigner(properties);
        URI download = URI.create(signer.sign("test-bucket", "files/文档.pdf", "最新 名称+报告.pdf"));
        Map<String, String> downloadQuery = readQuery(download);
        assertEquals("https", download.getScheme());
        assertEquals("test-bucket.oss-cn-hangzhou.aliyuncs.com", download.getHost());
        assertEquals("/files/文档.pdf", download.getPath());
        assertEquals("attachment; filename=\"download\"; filename*=UTF-8''%E6%9C%80%E6%96%B0%20"
                        + "%E5%90%8D%E7%A7%B0%2B%E6%8A%A5%E5%91%8A.pdf",
                downloadQuery.get("response-content-disposition"));
        assertFalse(downloadQuery.containsKey("x-oss-process"));
        assertV4(downloadQuery);

        String process = "image/resize,w_1080";
        URI preview = URI.create(signer.signPreview("test-bucket", "files/凭证.png", process));
        Map<String, String> previewQuery = readQuery(preview);
        assertEquals("files.example.test", preview.getHost());
        assertEquals("/files/凭证.png", preview.getPath());
        assertEquals("inline", previewQuery.get("response-content-disposition"));
        assertEquals("private, no-store", previewQuery.get("response-cache-control"));
        assertEquals(process, previewQuery.get("x-oss-process"));
        assertV4(previewQuery);
        URI plain = URI.create(signer.signPreview("test-bucket", "files/凭证.png", null));
        assertFalse(readQuery(plain).containsKey("x-oss-process"));
    }

    /**
     * @return 测试使用的固定 UTC 签名器
     */
    private AliyunPostV4Signer uploadSigner() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T08:55:00Z"), ZoneOffset.UTC);
        return new AliyunPostV4Signer(properties, clock);
    }

    /**
     * @return 离线样例配置
     */
    private AliyunOssProperties properties() {
        AliyunOssProperties config = new AliyunOssProperties();
        config.setAccessKey("test-key");
        config.setAccessSecret("test-secret");
        config.setRegion("cn-hangzhou");
        config.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        config.setHost("https://files.example.test/");
        config.setBucket("test-bucket");
        return config;
    }

    /**
     * 解码签名载荷
     *
     * @param encoded Base64 字段
     * @return JSON 数据
     * @throws Exception 字段无法解析
     */
    private JsonNode decodeJson(String encoded) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        return new ObjectMapper().readTree(bytes);
    }

    /**
     * 读取预签名查询字段
     *
     * @param uri 签名 URL
     * @return 解码后的字段
     * @throws Exception 查询字符串无法解码
     */
    private Map<String, String> readQuery(URI uri) throws Exception {
        Map<String, String> query = new LinkedHashMap<>();
        for (String item : uri.getRawQuery().split("&")) {
            String[] pair = item.split("=", 2);
            query.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
        }
        return query;
    }

    /**
     * 检查下载链接签名版本、摘要和有效期
     *
     * @param query 查询字段
     */
    private void assertV4(Map<String, String> query) {
        assertEquals("OSS4-HMAC-SHA256", query.get("x-oss-signature-version"));
        assertTrue(query.get("x-oss-signature").matches("[0-9a-f]{64}"));
        int seconds = Integer.parseInt(query.get("x-oss-expires"));
        assertTrue(seconds > 0 && seconds <= 300);
    }
}
