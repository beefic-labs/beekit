# 贡献指南

## 构建和测试

使用 Maven 3.9.x 或 3.10.x。运行 `mvn -v` 可确认 Maven 实际使用的 JDK。修改公共 API 或运行时依赖后，分别使用 JDK 8、11、17 和 21 验证。较新的 JDK 使用 `--release 8` 约束可用的 Java API 和目标字节码。

在仓库根目录执行以下命令。

```sh
mvn clean verify
mvn -pl beekit-oss clean verify
```

第一条命令验证整个项目，第二条命令仅验证 OSS 模块。OSS 单元测试使用本地样例和 SDK 替身，覆盖签名、回调验签、公钥来源限制及删除请求转发。

推送到 `main` 或向 `main` 发起拉取请求时，[CI 工作流](.github/workflows/ci.yml) 使用 Temurin JDK 8、11、17、21 分别运行聚合构建和测试。

## 发布产物检查

`mvn -Prelease clean verify` 在本地验证发布构建。父项目和 BOM 均为 POM，OSS 模块在 `beekit-oss/target/` 生成主 JAR、源码 JAR 和 Javadoc JAR。主 JAR 包含 `META-INF/LICENSE`。

发布前，将父 POM、BOM 和 OSS 模块的版本更新为同一个版本，并在 [CHANGELOG.md](CHANGELOG.md) 写入该版本的非空小节，例如 `## 0.1.0`。在包含这些改动的提交上创建 `vMAJOR.MINOR.PATCH` 格式的标签（如 `v0.1.0`），推送标签后即触发 [Release 工作流](.github/workflows/release.yml)。

JDK 矩阵通过后，工作流使用 JDK 21 构建并上传 OSS 的三份 JAR，以及父项目、BOM 和 OSS 的三个 POM。发布说明取自对应版本的 CHANGELOG 小节。工作流校验草稿中的附件后才公开 Release。同一标签已有 Release 或草稿时会停止。上传失败留下草稿时，检查并移除该次未完成的草稿，再重跑失败的任务。

GitHub Release 提供附件下载。通过 Maven 坐标使用组件时，可检出对应标签并执行 `mvn install`，然后按 [快速开始](README.md#快速开始) 导入 BOM。Maven Central 或 GitHub Packages 的发布由各自的仓库流程管理。

## 新增独立模块

1. 新建 `beekit-<module>`，在子模块 POM 中继承根 POM，并声明独立的 `artifactId`。
2. 将模块加入根 POM 的 `modules`，并在 BOM 中登记坐标。随项目同步发版的模块使用 `${project.version}`。
3. 声明模块实际使用的依赖。框架集成放在相应的集成模块。
4. 验证聚合构建和单模块构建，再使用外部工程导入 BOM 并单独引用新组件。
5. 更新根 README 的模块入口及模块自己的用法示例。
