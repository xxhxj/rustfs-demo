# Python Demo — RustFS 第三方接入

本目录是给第三方用的 **Python 接入样例**。RustFS 兼容 S3，不提供独立 Python SDK，使用官方 **boto3**。

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
| 默认桶 | `python-demo` |

集群启动见：`D:\environment\professional\server\rustfs\部署与启动.md`

---

## 项目结构

```
python-demo/
  README.md            本说明
  .env                 本机连接参数（可改）
  .env.example         连接参数模板
  requirements.txt     boto3、flask
  rustfs_client.py     连接、建桶、上传、预签名、Content-Type
  main.py              命令行入口
  app.py               前端入口（http://127.0.0.1:18880）
  web/index.html       上传页面
  samples/hello.txt    命令行演示用的示例文件
  samples/chinese.txt  UTF-8 中文示例，用来验证预览不乱码
```

接入时优先看 `rustfs_client.py` 里的 `create_s3_client()` / `upload_fileobj()` / `presign_get()`。

---

## 环境要求

- Python 3.8+
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

**必做：**

- `endpoint_url` 填 S3 API，不要填控制台地址
- `addressing_style='path'`（RustFS 默认 path-style）
- `signature_version='s3v4'`
- Region 用 `us-east-1`

---

## 怎么跑

```powershell
cd python-demo
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

命令行（上传 `samples/hello.txt` 并打印预签名 URL）：

```powershell
python main.py
```

指定文件：

```powershell
python main.py D:\path\to\file.txt
```

前端页面：

```powershell
python app.py
```

浏览器打开 http://127.0.0.1:18880 ，选文件上传，页面会展示对象键和预签名链接。点链接即可预览。

---

## 前端接口

`POST /api/upload`，`multipart/form-data`，字段名 `file`。

成功示例：

```json
{
  "ok": true,
  "bucket": "python-demo",
  "key": "demo/hello.txt",
  "url": "http://127.0.0.1:19000/python-demo/demo/hello.txt?response-content-type=...&X-Amz-Algorithm=..."
}
```

`url` 有效期 10 分钟，可直接给浏览器打开。密钥不会下发到前端。该接口形态与 `java-demo` 一致。

---

## 接到自己项目里（核心代码）

```python
import boto3
from botocore.client import Config

s3 = boto3.client(
    "s3",
    endpoint_url="http://127.0.0.1:19000",
    aws_access_key_id="ACCESS_KEY",
    aws_secret_access_key="SECRET_KEY",
    region_name="us-east-1",
    config=Config(
        signature_version="s3v4",
        s3={"addressing_style": "path"},
    ),
)

s3.upload_file(
    "hello.txt",
    "python-demo",
    "demo/hello.txt",
    ExtraArgs={"ContentType": "text/plain; charset=utf-8"},
)

url = s3.generate_presigned_url(
    ClientMethod="get_object",
    Params={
        "Bucket": "python-demo",
        "Key": "demo/hello.txt",
        "ResponseContentType": "text/plain; charset=utf-8",
    },
    ExpiresIn=600,
)
print(url)
```

---

## 中文 txt 预览乱码

上传文本时必须带 `charset=utf-8`，预签名 GET 建议同时带 `ResponseContentType=text/plain; charset=utf-8`。  
否则浏览器可能按系统默认编码（中文 Windows 常为 GBK）打开 UTF-8 文件，看起来像乱码。

记事本请另存为 UTF-8 再传。

---

## 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| EndpointConnectionError | 集群未启动或端口不对 | 确认 `19000`，不要用 `19101` |
| PermanentRedirect / 桶 URL 错误 | 没用 path-style | `addressing_style='path'` |
| SignatureDoesNotMatch | 没用 v4 签名 | `signature_version='s3v4'` |
| 403 AccessDenied | 密钥错误 | 核对 `.env` |
| 预览乱码 | 缺少 charset | 见上一节 |
