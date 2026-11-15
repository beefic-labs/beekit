package com.beefic.beekit.oss.aliyun;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.util.Base64;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OSS 回调签名与公钥来源测试
 *
 * @author Vincent
 */
class AliyunOssCallbackVerifierTest {

    /**
     * 原始请求体、解码路径和未解码查询参数都参与签名
     *
     * @throws Exception 密钥或签名算法不可用
     */
    @Test
    void verifiesRawCallbackAndRejectsChanges() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String callbackUrl = "https://api.example.test/%E5%9B%9E%E8%B0%83+doc?source=foo%2Fbar";
        byte[] body = "object=files%2Fa.pdf".getBytes(StandardCharsets.UTF_8);
        byte[] prefix = "/回调+doc?source=foo%2Fbar\n".getBytes(StandardCharsets.UTF_8);
        Signature signer = Signature.getInstance("MD5withRSA");
        signer.initSign(keyPair.getPrivate());
        signer.update(prefix);
        signer.update(body);
        String authorization = Base64.getEncoder().encodeToString(signer.sign());

        AliyunOssCallbackVerifier verifier = new AliyunOssCallbackVerifier();
        assertTrue(verifier.verifySignature(keyPair.getPublic(), callbackUrl, authorization, body));
        byte[] changed = "object=changed.pdf".getBytes(StandardCharsets.UTF_8);
        assertFalse(verifier.verifySignature(keyPair.getPublic(), callbackUrl, authorization, changed));
    }

    /**
     * 公钥下载只允许预期的 HTTPS 域名
     */
    @Test
    void rejectsUntrustedPublicKeyUrlsBeforeConnecting() {
        String allowed = "https://gosspublic.alicdn.com/callback.pem";
        assertEquals(allowed, AliyunOssCallbackVerifier.trustedPublicKeyUri(allowed).toString());
        AliyunOssCallbackVerifier verifier = new AliyunOssCallbackVerifier();
        String[] rejected = {
                "http://gosspublic.alicdn.com/callback.pem",
                "https://gosspublic.alicdn.com.example.test/callback.pem",
                "https://name@gosspublic.alicdn.com/callback.pem",
                "https://gosspublic.alicdn.com:8443/callback.pem"
        };
        for (String url : rejected) {
            String header = Base64.getEncoder().encodeToString(url.getBytes(StandardCharsets.UTF_8));
            assertThrows(AliyunOssException.class,
                    () -> verifier.verify("https://api.example.test/callback", "unused", header, new byte[0]));
        }
    }
}
