"""体检报告批量上传：独立 S3 凭证 + 达梦附件表，不改原 /api/upload。"""

from __future__ import annotations

import os
import uuid
from datetime import datetime
from typing import Any

import boto3
from botocore.client import Config
from werkzeug.datastructures import FileStorage

from exam_db import insert_attachment
from rustfs_client import presign_get, upload_fileobj

BUSINESS_TYPE = "EXAM_REPORT_BUCKET"


def _env(name: str, default: str) -> str:
    value = os.environ.get(name, default)
    return default if value is None or value == "" else value


def _clip(value: str, max_len: int) -> str:
    return value if len(value) <= max_len else value[:max_len]


def _original_name(raw: str | None) -> str:
    if not raw or not raw.strip():
        return "unnamed"
    name = raw.replace("\\", "/").rsplit("/", 1)[-1].strip().replace("~", "_")
    return name or "unnamed"


def _exam_s3():
    return boto3.client(
        "s3",
        endpoint_url=_env("EXAM_REPORT_ENDPOINT", _env("RUSTFS_ENDPOINT", "http://127.0.0.1:9000")),
        aws_access_key_id=_env("EXAM_REPORT_ACCESS_KEY", "ACb2x3y2t1PuowZEErYb"),
        aws_secret_access_key=_env("EXAM_REPORT_SECRET_KEY", "mIWUaAJvNek1SwMN1R6TjPcnRw92wgF9rj8t0zeN"),
        region_name=_env("RUSTFS_REGION", "us-east-1"),
        config=Config(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
        ),
    )


def upload_exam_reports(files: list[FileStorage], business_key: str | None) -> dict[str, Any]:
    bucket = _env("EXAM_REPORT_BUCKET", "inesa-bg-examreport")
    org = _env("EXAM_REPORT_YYDH", "A")
    uploader_id = _clip(_env("EXAM_REPORT_UPLOADER_ID", "demo-exam-report-uploader"), 36)
    key = (business_key or "").strip()[:50]
    s3 = _exam_s3()
    items: list[dict[str, Any]] = []
    errors: list[dict[str, str]] = []
    for uploaded in files:
        original = _original_name(uploaded.filename)
        try:
            items.append(_upload_one(s3, bucket, org, uploader_id, uploaded, original, key))
        except Exception as exc:
            errors.append({"fileName": original, "error": str(exc)})
    return {
        "ok": bool(items),
        "bucket": bucket,
        "businessType": BUSINESS_TYPE,
        "items": items,
        "errors": errors,
    }


def _upload_one(
    s3,
    bucket: str,
    org: str,
    uploader_id: str,
    uploaded: FileStorage,
    original: str,
    business_key: str,
) -> dict[str, Any]:
    file_id = uuid.uuid4().hex
    now = datetime.now()
    day = now.strftime("%Y%m%d")
    ts = now.strftime("%Y-%m-%d %H:%M:%S.%f")[:23]
    stored_name = _clip(f"{org}_{day}_{original}", 255)
    file_path = _clip(f"{org}/{day}/{file_id}~{original}", 500)
    uploaded.stream.seek(0)
    uploaded.stream.seek(0, 2)
    size = uploaded.stream.tell()
    uploaded.stream.seek(0)
    mime = uploaded.mimetype or "application/octet-stream"
    upload_fileobj(s3, bucket, file_path, uploaded.stream, mime)
    insert_attachment(
        {
            "id": file_id,
            "file_name": stored_name,
            "file_path": file_path,
            "file_size": size,
            "uploader_id": uploader_id,
            "upload_time": ts,
            "business_type": BUSINESS_TYPE,
            "business_key": business_key,
            "mime_type": _clip(mime, 100),
            "vtag": 0,
            "created_time": ts,
            "updated_time": ts,
            "yydh": _clip(org, 36),
            "actual_file_name": _clip(original, 128),
        }
    )
    return {
        "id": file_id,
        "fileName": stored_name,
        "filePath": file_path,
        "fileSize": size,
        "businessKey": business_key,
        "url": presign_get(s3, bucket, file_path),
    }
