# BeeKit

BeeKit 是模块化的 Java 工具与基础组件库。每个组件可按需引入，版本由 BeeKit BOM 管理。

支持 Java 8 及以上版本，已在 JDK 8、11、17 和 21 上验证。

## 可用模块

| 模块 | 用途 |
| --- | --- |
| [`beekit-oss`](beekit-oss/README.md) | 阿里云 OSS 表单直传授权、下载与预览签名、回调验签和对象删除 |

`beekit-bom` 管理组件版本。版本内容见 [发布记录](CHANGELOG.md)。

## 快速开始

使用 Maven 3.9.x 或 3.10.x 在 BeeKit 根目录执行 `mvn install`，将 `0.1.0` 安装到本地 Maven 仓库。随后在使用方的 `pom.xml` 中导入 BOM，并声明所需组件。

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.beefic</groupId>
            <artifactId>beekit-bom</artifactId>
            <version>0.1.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.beefic</groupId>
        <artifactId>beekit-oss</artifactId>
    </dependency>
</dependencies>
```

设置环境变量 `OSS_ACCESS_KEY_ID` 和 `OSS_ACCESS_KEY_SECRET`，并将区域、端点和存储桶改为实际值。以下程序在本地生成直传授权，打印客户端应提交表单的地址。

示例授权绑定 5 字节的 `hello.txt`，例如内容为 `hello` 且末尾无换行的文件。

```java
import com.beefic.beekit.oss.aliyun.AliyunOssProperties;
import com.beefic.beekit.oss.aliyun.AliyunPostV4Signer;

public class OssQuickStart {
    public static void main(String[] args) {
        AliyunOssProperties properties = new AliyunOssProperties();
        properties.setAccessKey(System.getenv("OSS_ACCESS_KEY_ID"));
        properties.setAccessSecret(System.getenv("OSS_ACCESS_KEY_SECRET"));
        properties.setRegion("cn-hangzhou");
        properties.setEndpoint("oss-cn-hangzhou.aliyuncs.com");
        properties.setBucket("your-bucket");

        AliyunPostV4Signer signer = new AliyunPostV4Signer(properties);
        System.out.println(signer.sign("hello.txt", 5, "text/plain").getHost());
    }
}
```

表单字段及其他用法见 [OSS 使用指南](beekit-oss/README.md)。构建、测试和新增模块见 [贡献指南](CONTRIBUTING.md)。

## 许可证

[Apache License 2.0](LICENSE)
