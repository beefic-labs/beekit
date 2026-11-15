# BeeKit OSS

`beekit-oss` 提供阿里云 OSS 表单直传授权、下载与预览签名、回调验签和对象删除。运行时依赖阿里云 OSS Java SDK V2 和 Jackson，可在普通 Java 应用中直接使用。

## Maven 依赖

按照 [快速开始](../README.md#快速开始) 安装开发快照并导入 BeeKit BOM，再在应用的 `dependencies` 中声明模块。

```xml
<dependency>
    <groupId>com.beefic</groupId>
    <artifactId>beekit-oss</artifactId>
</dependency>
```

## 初始化与配置

完整初始化见 [最小示例](../README.md#快速开始)。下文的 OSS 类型位于 `com.beefic.beekit.oss.aliyun` 包，示例沿用其中的 `properties` 和 `signer`。

| 配置 | 用途 |
| --- | --- |
| `accessKey`、`accessSecret` | 签名与 SDK 客户端使用的凭据，从环境变量或密钥服务读取 |
| `region` | OSS 区域，例如 `cn-hangzhou` |
| `endpoint` | 区域端点，用于直传、下载和删除 |
| `bucket` | 直传签名使用的存储桶。下载、预览和删除由调用方传入存储桶 |
| `host` | 预览使用的 OSS 绑定域名 |
| `callback` | 启用上传回调时使用的服务端 URL |

## 表单直传

```java
AliyunPostV4SignResult upload = signer.sign("documents/sample.pdf", 123, "application/pdf");
```

第二个参数是文件的实际字节数，第三个参数是 MIME 类型。授权五分钟有效，绑定对象 key、精确字节数和 MIME 类型，并禁止覆盖已有对象。

客户端以 `multipart/form-data` 向 `upload.getHost()` 发起 POST，按下表填写表单，并将 `file` 放在最后。

| 表单字段 | 值 |
| --- | --- |
| `key` | `getKey()` |
| `policy` | `getPolicy()` |
| `x-oss-signature` | `getSignature()` |
| `x-oss-signature-version` | `getSignatureVersion()` |
| `x-oss-credential` | `getCredential()` |
| `x-oss-date` | `getDate()` |
| `x-oss-forbid-overwrite` | `getForbidOverwrite()` |
| `content-type` | `getContentType()` |
| `file` | 文件内容 |

## 上传回调

设置回调 URL，并向签名方法传入完整的回调请求体模板。自定义变量名以 `x:` 开头。

```java
properties.setCallback("https://api.example.com/oss/callback");
AliyunPostV4SignResult uploadWithCallback = signer.sign(
        "documents/sample.pdf", 123, "application/pdf",
        java.util.Collections.singletonMap("x:request_id", "req-123"),
        "requestId=${x:request_id}&bucket=${bucket}&object=${object}&size=${size}");
```

表单额外提交 `callback`（`getCallback()`），并将 `getCallbackVar()` 中的每个条目作为独立的表单字段。

服务端复用一个 `AliyunOssCallbackVerifier` 实例以缓存公钥。将配置的回调 URL、`Authorization` 请求头、`x-oss-pub-key-url` 请求头和**原始请求体字节**传入 `verify(callbackUrl, authorization, publicKeyUrlHeader, body)`，验签成功后再解析请求体。公钥地址须为 Base64 编码后的 `https://gosspublic.alicdn.com` URL，验签器拒绝公钥下载重定向，失败时抛出 `AliyunOssException`。

## 下载与预览

```java
properties.setHost("files.example.com");
AliyunDownloadSigner downloader = new AliyunDownloadSigner(properties);
String downloadUrl = downloader.sign(properties.getBucket(), "documents/sample.pdf", "示例.pdf");
String previewUrl = downloader.signPreview(properties.getBucket(), "images/sample.png", "image/resize,w_1080");
```

两个 URL 均为五分钟有效。下载使用区域端点和附件文件名。预览使用绑定域名和内联响应，图片处理指令可传 `null`。

## 删除对象

客户端可复用，通过 `close()` 释放 SDK 资源。版本化存储桶遵循 OSS 默认删除语义。

```java
try (AliyunOssClient client = new AliyunOssClient(properties)) {
    client.deleteObject(properties.getBucket(), "documents/sample.pdf");
}
```
