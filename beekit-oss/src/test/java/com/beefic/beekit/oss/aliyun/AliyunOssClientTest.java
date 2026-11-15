package com.beefic.beekit.oss.aliyun;

import com.aliyun.sdk.service.oss2.OSSClient;
import com.aliyun.sdk.service.oss2.models.DeleteObjectRequest;
import com.aliyun.sdk.service.oss2.models.DeleteObjectResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证删除请求和 SDK 生命周期
 *
 * @author Vincent
 */
class AliyunOssClientTest {

    /**
     * 删除使用调用方指定的存储桶与 key，关闭时传递给 SDK
     */
    @Test
    void forwardsDeleteAndClosesSdk() {
        RecordingOssClient sdk = new RecordingOssClient();
        try (AliyunOssClient client = new AliyunOssClient(sdk)) {
            client.deleteObject("test-bucket", "files/a.pdf");
            assertThrows(IllegalArgumentException.class, () -> client.deleteObject("test-bucket", ""));
        }
        assertEquals("test-bucket", sdk.request.bucket());
        assertEquals("files/a.pdf", sdk.request.key());
        assertTrue(sdk.closed);
    }

    /**
     * 记录 SDK 方法调用
     *
     * @author Vincent
     */
    private static final class RecordingOssClient implements OSSClient {
        private DeleteObjectRequest request;
        private boolean closed;

        /**
         * 记录删除对象请求
         *
         * @param request SDK 请求
         * @return 删除结果
         */
        @Override
        public DeleteObjectResult deleteObject(DeleteObjectRequest request) {
            this.request = request;
            return null;
        }

        /**
         * 标记 SDK 已关闭
         */
        @Override
        public void close() {
            closed = true;
        }
    }
}
