"""达梦附件表：仅体检报告上传使用，和 Flask/S3 主流程解耦。

JayDeBeApi/JPype 需要 Java 9+。本机 Spring Boot demo 仍用 JDK 8，
这里单独切到 JDK 17，不影响 Java demo。
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Any

import jaydebeapi

PROJECT_ROOT = Path(__file__).resolve().parent
JDBC_JAR = PROJECT_ROOT / "lib" / "DmJdbcDriver18.jar"

_INSERT_SQL = """
INSERT INTO OP_MANAGE.TB_ATTACHMENT (
  ID, FILE_NAME, FILE_PATH, FILE_SIZE, UPLOADER_ID, UPLOAD_TIME,
  BUSINESS_TYPE, BUSINESS_KEY, MIME_TYPE, VTAG, CREATED_TIME, UPDATED_TIME,
  YYDH, ACTUAL_FILE_NAME
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
"""


def _env(name: str, default: str) -> str:
    value = os.environ.get(name, default)
    return default if value is None or value == "" else value


def _prepare_jvm() -> None:
    configured = os.environ.get("EXAM_JAVA_HOME", "").strip()
    if configured:
        os.environ["JAVA_HOME"] = configured
        return
    home = os.environ.get("JAVA_HOME", "")
    java_exe = Path(home) / "bin" / "java.exe"
    if home and java_exe.is_file() and _java_major(home) >= 9:
        return
    for candidate in (
        Path(r"D:\Program Files\Java\jdk-17"),
        Path(r"C:\Program Files\Java\jdk-17"),
        Path(r"C:\Program Files\Eclipse Adoptium\jdk-17"),
    ):
        if (candidate / "bin" / "java.exe").is_file():
            os.environ["JAVA_HOME"] = str(candidate)
            return


def _java_major(java_home: str) -> int:
    release = Path(java_home) / "release"
    if not release.is_file():
        return 0
    text = release.read_text(encoding="utf-8", errors="ignore")
    for line in text.splitlines():
        if line.startswith("JAVA_VERSION="):
            version = line.split("=", 1)[1].strip().strip('"')
            if version.startswith("1."):
                return int(version.split(".")[1])
            return int(version.split(".")[0])
    return 0


def insert_attachment(row: dict[str, Any]) -> None:
    _prepare_jvm()
    conn = jaydebeapi.connect(
        _env("EXAM_DM_DRIVER", "dm.jdbc.driver.DmDriver"),
        _env("EXAM_DM_URL", "jdbc:dm://127.0.0.1:5236?schema=OP_MANAGE&compatibleMode=oracle"),
        [_env("EXAM_DM_USER", "SYSDBA"), _env("EXAM_DM_PASSWORD", "inspur123@A")],
        str(JDBC_JAR),
    )
    try:
        cur = conn.cursor()
        try:
            cur.execute(
                _INSERT_SQL,
                [
                    row["id"],
                    row["file_name"],
                    row["file_path"],
                    int(row["file_size"]),
                    row["uploader_id"],
                    row["upload_time"],
                    row["business_type"],
                    row["business_key"],
                    row["mime_type"],
                    int(row["vtag"]),
                    row["created_time"],
                    row["updated_time"],
                    row["yydh"],
                    row["actual_file_name"],
                ],
            )
            conn.commit()
        finally:
            cur.close()
    finally:
        conn.close()
