package com.beefic.beekit.oss.aliyun;

/**
 * OSS 操作或回调验证失败
 *
 * @author Vincent
 */
public class AliyunOssException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 创建 OSS 异常
     *
     * @param message 错误描述
     */
    public AliyunOssException(String message) {
        super(message);
    }

    /**
     * 创建带原始原因的 OSS 异常
     *
     * @param message 错误描述
     * @param cause 错误原因
     */
    public AliyunOssException(String message, Throwable cause) {
        super(message, cause);
    }
}
