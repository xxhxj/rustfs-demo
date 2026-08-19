"""RustFS S3 客户端：连接、建桶、上传、预签名。"""

from __future__ import annotations

import os
from pathlib import Path

import boto3
from botocore.client import Config
from botocore.exceptions import ClientError

PROJECT_ROOT = Path(__file__).resolve().parent


def load_env(path: Path | None = None) -> None:
    env_path = path or PROJECT_ROOT / ".env"
    if not env_path.is_file():
        return
    for raw in env_path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip().strip('"').strip("'"))


def env(name: str, default: str | None = None) -> str:
    value = os.environ.get(name, default)
    if value is None or value == "":
        raise RuntimeError(f"缺少环境变量 {name}，请检查 .env")
    return value


def content_type_for(key: str, provided: str | None = None) -> str:
    """文本对象补 charset=utf-8，避免浏览器按 GBK 预览乱码。"""
    type_ = (provided or "").strip()
    name = (key or "").lower()
    if not type_ or type_.lower() == "application/octet-stream":
        if name.endswith((".html", ".htm")):
            type_ = "text/html"
        elif name.endswith(".json"):
            type_ = "application/json"
        elif name.endswith((".txt", ".log", ".md", ".csv")):
            type_ = "text/plain"
        else:
            type_ = "application/octet-stream"
    lower = type_.lower()
    if "charset=" not in lower and (
        lower.startswith("text/") or any(token in lower for token in ("json", "xml", "javascript"))
    ):
        return f"{type_}; charset=utf-8"
    return type_


def create_s3_client():
    return boto3.client(
        "s3",
        endpoint_url=env("RUSTFS_ENDPOINT"),
        aws_access_key_id=env("RUSTFS_ACCESS_KEY"),
        aws_secret_access_key=env("RUSTFS_SECRET_KEY"),
        region_name=env("RUSTFS_REGION", "us-east-1"),
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
        ),
    )


def ensure_bucket(s3, bucket: str) -> None:
    try:
        s3.head_bucket(Bucket=bucket)
        return
    except ClientError as exc:
        code = str(exc.response.get("Error", {}).get("Code", ""))
        if code not in {"404", "NoSuchBucket", "NotFound"}:
            raise
    try:
        s3.create_bucket(Bucket=bucket)
    except ClientError as exc:
        code = str(exc.response.get("Error", {}).get("Code", ""))
        if code not in {"BucketAlreadyOwnedByYou", "BucketAlreadyExists"}:
            raise


def upload_fileobj(s3, bucket: str, key: str, fileobj, content_type: str | None = None) -> None:
    s3.upload_fileobj(
        fileobj,
        bucket,
        key,
        ExtraArgs={"ContentType": content_type_for(key, content_type)},
    )


def presign_get(s3, bucket: str, key: str, expires: int = 600) -> str:
    return s3.generate_presigned_url(
        ClientMethod="get_object",
        Params={
            "Bucket": bucket,
            "Key": key,
            "ResponseContentType": content_type_for(key),
        },
        ExpiresIn=expires,
    )
