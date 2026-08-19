# RustFS 接入 Demo

给第三方看的两套独立样例，都只演示 **上传** 和 **预签名预览**。RustFS 兼容 S3，Java 用 AWS SDK v2，Python 用 boto3。

| 目录 | 说明 | 页面 |
|---|---|---|
| [java-demo](java-demo/README.md) | Java 8 + Spring Boot 2.7 | http://127.0.0.1:18881 |
| [python-demo](python-demo/README.md) | Python 3 + Flask | http://127.0.0.1:18880 |

启动 Java 页面：

```powershell
cd java-demo
mvn spring-boot:run
```

启动 Python 页面：

```powershell
cd python-demo
python app.py
```

两套的连接方式和接口形态一致。改对方环境时，编辑各自目录下的 `.env` 即可。

本机测试集群：S3 `http://127.0.0.1:19000`，账号 `rustfsadmin-local` / `rustfssecret-local`。启动方式见 `D:\environment\professional\server\rustfs\部署与启动.md`。
