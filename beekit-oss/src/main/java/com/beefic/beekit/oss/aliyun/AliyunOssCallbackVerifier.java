package com.beefic.beekit.oss.aliyun;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.HttpsURLConnection;

/**
 * 阿里云 OSS 上传回调验签
 *
 * @author Vincent
 */
public class AliyunOssCallbackVerifier {

    private static final String PUBLIC_KEY_HOST = "gosspublic.alicdn.com";
    private static final int REQUEST_TIMEOUT_MILLIS = 5000;
    private static final int MAX_PUBLIC_KEY_BYTES = 16384;
    private final Map<URI, PublicKey> publicKeys = new ConcurrentHashMap<>();

    /**
     * 下载 OSS 公钥并校验回调请求签名
     *
     * @param callbackUrl 配置的回调地址，包含签名所需的路径和查询字符串
     * @param authorization Base64 编码的 OSS 回调签名
     * @param publicKeyUrlHeader Base64 编码的 x-oss-pub-key-url 请求头
     * @param body 原始请求体字节
     * @throws AliyunOssException 公钥下载或验签失败
     */
    public void verify(String callbackUrl, String authorization, String publicKeyUrlHeader, byte[] body) {
        try {
            String publicKeyUrl = new String(Base64.getDecoder().decode(publicKeyUrlHeader), StandardCharsets.UTF_8);
            URI uri = trustedPublicKeyUri(publicKeyUrl);
            PublicKey publicKey = publicKeys.get(uri);
            if (publicKey == null) {
                publicKey = loadPublicKey(uri);
                if (publicKeys.size() < 32) {
                    publicKeys.putIfAbsent(uri, publicKey);
                }
            }
            if (!verifySignature(publicKey, callbackUrl, authorization, body)) {
                throw new AliyunOssException("Invalid OSS callback signature");
            }
        } catch (GeneralSecurityException | IOException | IllegalArgumentException | NullPointerException exception) {
            throw new AliyunOssException("Cannot verify OSS callback", exception);
        }
    }

    /**
     * 校验公钥下载地址
     *
     * @param publicKeyUrl 公钥地址
     * @return 已验证的 HTTPS URI
     */
    static URI trustedPublicKeyUri(String publicKeyUrl) {
        URI uri = URI.create(publicKeyUrl);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !PUBLIC_KEY_HOST.equalsIgnoreCase(uri.getHost())
                || uri.getRawUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 443)
                || uri.getRawFragment() != null) {
            throw new AliyunOssException("Invalid OSS callback public key URL");
        }
        return uri;
    }

    /**
     * 使用指定公钥校验路径、查询字符串和请求体
     *
     * @param publicKey RSA 公钥
     * @param callbackUrl 配置的回调地址
     * @param authorization Base64 编码的签名
     * @param body 原始请求体字节
     * @return 是否通过校验
     * @throws GeneralSecurityException 签名算法或密钥不可用
     */
    boolean verifySignature(PublicKey publicKey, String callbackUrl, String authorization, byte[] body)
            throws GeneralSecurityException {
        URI callbackUri = URI.create(callbackUrl);
        String resource = callbackUri.getPath();
        if (resource == null) {
            throw new IllegalArgumentException("Invalid OSS callback URL");
        }
        if (resource.isEmpty()) {
            resource = "/";
        }
        if (callbackUri.getRawQuery() != null) {
            resource += "?" + callbackUri.getRawQuery();
        }
        byte[] resourceBytes = (resource + "\n").getBytes(StandardCharsets.UTF_8);
        Signature verifier = Signature.getInstance("MD5withRSA");
        verifier.initVerify(publicKey);
        verifier.update(resourceBytes);
        verifier.update(body);
        byte[] signature = Base64.getDecoder().decode(authorization);
        return verifier.verify(signature);
    }

    /**
     * 从阿里云 HTTPS 域名下载并解析公钥
     *
     * @param uri 已验证的公钥 URI
     * @return RSA 公钥
     * @throws IOException 公钥请求失败
     * @throws GeneralSecurityException 公钥无法解析
     */
    private PublicKey loadPublicKey(URI uri) throws IOException, GeneralSecurityException {
        HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(REQUEST_TIMEOUT_MILLIS);
        connection.setReadTimeout(REQUEST_TIMEOUT_MILLIS);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                throw new IOException("OSS callback public key request failed");
            }
            String pem;
            try (InputStream stream = connection.getInputStream()) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int count;
                while ((count = stream.read(buffer)) != -1) {
                    if (bytes.size() + count > MAX_PUBLIC_KEY_BYTES) {
                        throw new IOException("OSS callback public key response exceeds maximum size");
                    }
                    bytes.write(buffer, 0, count);
                }
                pem = new String(bytes.toByteArray(), StandardCharsets.US_ASCII);
            }
            String value = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(value);
            X509EncodedKeySpec key = new X509EncodedKeySpec(encoded);
            return KeyFactory.getInstance("RSA").generatePublic(key);
        } finally {
            connection.disconnect();
        }
    }
}
