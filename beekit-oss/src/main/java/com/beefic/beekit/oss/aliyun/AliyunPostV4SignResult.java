package com.beefic.beekit.oss.aliyun;

import java.util.Map;

/**
 * POST V4 上传授权结果
 *
 * @author Vincent
 */
public final class AliyunPostV4SignResult {

    private final String key;
    private final String host;
    private final String accessId;
    private final String policy;
    private final String signature;
    private final String signatureVersion;
    private final String credential;
    private final String date;
    private final String expire;
    private final String callback;
    private final Map<String, String> callbackVar;
    private final String contentType;

    /**
     * 保存上传表单授权字段
     *
     * @param key 对象 key
     * @param host 上传地址
     * @param accessId 访问密钥 ID
     * @param policy Base64 编码的上传策略
     * @param signature 签名
     * @param signatureVersion 签名版本
     * @param credential 凭证范围
     * @param date 签名时间
     * @param expire 到期时间的 Unix 秒数
     * @param callback Base64 编码的回调配置，未启用时为 null
     * @param callbackVar 回调变量
     * @param contentType 文件类型
     */
    AliyunPostV4SignResult(String key, String host, String accessId, String policy, String signature,
                           String signatureVersion, String credential, String date, String expire,
                           String callback, Map<String, String> callbackVar, String contentType) {
        this.key = key;
        this.host = host;
        this.accessId = accessId;
        this.policy = policy;
        this.signature = signature;
        this.signatureVersion = signatureVersion;
        this.credential = credential;
        this.date = date;
        this.expire = expire;
        this.callback = callback;
        this.callbackVar = callbackVar;
        this.contentType = contentType;
    }

    /**
     * @return 对象 key
     */
    public String getKey() {
        return key;
    }

    /**
     * @return 上传地址
     */
    public String getHost() {
        return host;
    }

    /**
     * @return 访问密钥 ID
     */
    public String getAccessId() {
        return accessId;
    }

    /**
     * @return Base64 编码的上传策略
     */
    public String getPolicy() {
        return policy;
    }

    /**
     * @return 签名
     */
    public String getSignature() {
        return signature;
    }

    /**
     * @return 签名版本
     */
    public String getSignatureVersion() {
        return signatureVersion;
    }

    /**
     * @return 凭证范围
     */
    public String getCredential() {
        return credential;
    }

    /**
     * @return 签名时间
     */
    public String getDate() {
        return date;
    }

    /**
     * @return 到期时间的 Unix 秒数
     */
    public String getExpire() {
        return expire;
    }

    /**
     * @return Base64 编码的回调配置，未启用时为 null
     */
    public String getCallback() {
        return callback;
    }

    /**
     * @return 回调变量
     */
    public Map<String, String> getCallbackVar() {
        return callbackVar;
    }

    /**
     * @return 文件类型
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return 禁止覆盖的表单字段值
     */
    public String getForbidOverwrite() {
        return "true";
    }
}
