# Java Demo — RustFS 第三方接入

本目录是给第三方用的 **Java 接入样例**。RustFS 兼容 S3，不提供独立 Java SDK，使用官方 **AWS SDK for Java v2**。

页面是 **Spring Boot 2.7**（适配本机 JDK 8），用 `mvn spring-boot:run` 启动。不是 Spring Boot 3（那需要 JDK 17）。

演示范围只有两件事：

1. 上传文件到指定桶
2. 生成预签名 GET 地址，浏览器打开即可预览/下载

对应本机测试集群（4 节点 MNMD）：

| 项 | 值 |
|---|---|
| S3 API | `http://127.0.0.1:19000`（也可 `19001`–`19003`，同一套数据） |
| 控制台 | http://127.0.0.1:19101 （管理用，SDK 不要连这个端口） |
| AccessKey | `rustfsadmin-local` |
| SecretKey | `rustfssecret-local` |
| Region | `us-east-1` |
| 寻址 / 签名 | **path-style** + **SigV4** |
| 默认桶 | `java-demo` |

集群启动见：`D:\environment\professional\server\rustfs\部署与启动.md`

---

## 项目结构

```
java-demo/
  README.md                 本说明
  .env                      本机连接参数（可改）
  .env.example              连接参数模板
  pom.xml                   Maven 依赖（Spring Boot 2.7 + AWS SDK v2）
  samples/hello.txt         命令行演示用的示例文件
  samples/chinese.txt       UTF-8 中文示例，用来验证预览不乱码
  src/main/java/demo/rustfs/
    RustfsApplication.java  Spring Boot 入口
    RustfsConfig.java       S3Client / S3Presigner Bean
    UploadController.java   POST /api/upload
    Rustfs.java             连接、建桶、上传、预签名、Content-Type
    UploadPresignDemo.java  命令行入口（不经过 Spring）
  src/main/resources/
    application.properties  端口 18881
    static/index.html       上传页面
```

接入时优先看 `Rustfs.java` 里的 `s3()` / `presigner()` / `upload()` / `presignGet()`。

---

## 环境要求

- JDK 8+
- Maven 3.6+
- RustFS 已启动，且 `19000` 可访问

---

## 配置接入参数

复制 `.env.example` 为 `.env`，按对方环境修改：

```
RUSTFS_ENDPOINT=http://127.0.0.1:19000
RUSTFS_ACCESS_KEY=对方提供的 AccessKey
RUSTFS_SECRET_KEY=对方提供的 SecretKey
RUSTFS_REGION=us-east-1
RUSTFS_BUCKET=约定的桶名
```

也可以不建 `.env`，改用系统环境变量，名称相同。

**必做：**

- `endpoint` 填 S3 API，不要填控制台地址
- 必须 `forcePathStyle(true)`（RustFS 默认 path-style）
- 必须 SigV4；Region 用 `us-east-1` 即可
- HTTP 环境用 `http://`，不要走 SDK 默认的 AWS 公网地址

---

## 怎么启动

前提：JDK 8+、Maven 3.6+，RustFS 的 `19000` 已能访问。在 **`java-demo` 目录**执行。

### 1. 启动前端页面（常用）

入口类：`demo.rustfs.RustfsApplication`（Spring Boot，端口 `18881`）。

```powershell
cd D:\HXJ\workspace\cursor\rustfs_demo\java-demo
mvn spring-boot:run
```

也可以在 IDE 里直接运行 `RustfsApplication`。

终端出现 `Java 前端  http://127.0.0.1:18881` 后，浏览器打开该地址。这个窗口要一直开着，关掉就停服。

打包后启动：

```powershell
mvn -q -DskipTests package
java -jar target\rustfs-java-demo-1.0-SNAPSHOT.jar
```

### 2. 启动命令行 demo

入口类：`demo.rustfs.UploadPresignDemo`（不走 Spring）。上传 `samples/hello.txt`，打印 10 分钟预签名 URL，然后退出。

```powershell
cd D:\HXJ\workspace\cursor\rustfs_demo\java-demo
mvn -q compile exec:java
```

指定文件：

```powershell
mvn -q compile exec:java "-Dexec.args=D:\path\to\file.txt"
```

页面里选文件上传后，会显示对象键和预签名链接，点链接即可预览。

---

## 前端接口

`POST /api/upload`，`multipart/form-data`，字段名 `file`。

成功示例：

```json
{
  "ok": true,
  "bucket": "java-demo",
  "key": "demo/hello.txt",
  "url": "http://127.0.0.1:19000/java-demo/demo/hello.txt?response-content-type=...&X-Amz-Algorithm=..."
}
```

`url` 有效期 10 分钟，可直接给浏览器打开。密钥不会下发到前端。

---

## 接到自己项目里（核心代码）

```java
S3Client s3 = S3Client.builder()
    .endpointOverride(URI.create("http://127.0.0.1:19000"))
    .region(Region.US_EAST_1)
    .credentialsProvider(StaticCredentialsProvider.create(
        AwsBasicCredentials.create("ACCESS_KEY", "SECRET_KEY")))
    .forcePathStyle(true)
    .build();

s3.putObject(
    PutObjectRequest.builder()
        .bucket("java-demo")
        .key("demo/hello.txt")
        .contentType("text/plain; charset=utf-8")
        .build(),
    Paths.get("hello.txt"));
```

预签名：

```java
S3Presigner presigner = S3Presigner.builder()
    .endpointOverride(URI.create("http://127.0.0.1:19000"))
    .region(Region.US_EAST_1)
    .credentialsProvider(...)
    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
    .build();

String url = presigner.presignGetObject(
    GetObjectPresignRequest.builder()
        .getObjectRequest(GetObjectRequest.builder()
            .bucket("java-demo")
            .key("demo/hello.txt")
            .responseContentType("text/plain; charset=utf-8")
            .build())
        .signatureDuration(Duration.ofMinutes(10))
        .build())
    .url()
    .toString();
```

Maven 依赖：

```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
  <version>2.25.27</version>
</dependency>
```

本 demo 固定 2.25.27，可在 JDK 8 上运行。更高版本 SDK 也可以，注意仍要 path-style。

---

## 中文 txt 预览乱码

上传文本时必须带 `charset=utf-8`，预签名 GET 建议同时带 `responseContentType=text/plain; charset=utf-8`。  
否则浏览器可能按系统默认编码（中文 Windows 常为 GBK）打开 UTF-8 文件，看起来像乱码。

记事本请另存为 UTF-8 再传。

---

## 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| Connection refused | 集群未启动或端口不对 | 确认 `19000`，不要用 `19101` |
| 301 / 桶地址不对 | 没用 path-style | `forcePathStyle(true)` |
| 403 | 密钥错误，或预签名用了 HEAD | 核对 `.env`；浏览器要用 GET 打开链接 |
| 预览乱码 | 缺少 charset | 见上一节 |
| `spring-boot:run` 要 JDK 17 | 误用了 Spring Boot 3 | 本 demo 是 2.7.18，JDK 8 即可 |
| `exec:java` 没出页面 | 那是命令行入口 | 页面请用 `mvn spring-boot:run` |
