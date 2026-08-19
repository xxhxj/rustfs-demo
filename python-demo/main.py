"""命令行：上传示例文件并打印预签名 GET 地址。"""

from __future__ import annotations

import sys
from pathlib import Path

from rustfs_client import (
    PROJECT_ROOT,
    create_s3_client,
    ensure_bucket,
    env,
    load_env,
    presign_get,
    upload_fileobj,
)

OBJECT_KEY = "demo/hello.txt"


def main() -> int:
    load_env()
    sample = Path(sys.argv[1]) if len(sys.argv) > 1 else PROJECT_ROOT / "samples" / "hello.txt"
    if not sample.is_file():
        print(f"找不到待上传文件: {sample}", file=sys.stderr)
        return 1

    bucket = env("RUSTFS_BUCKET", "python-demo")
    s3 = create_s3_client()
    ensure_bucket(s3, bucket)
    with sample.open("rb") as fh:
        upload_fileobj(s3, bucket, OBJECT_KEY, fh, "text/plain")
    url = presign_get(s3, bucket, OBJECT_KEY)
    print(f"已上传: {sample} -> s3://{bucket}/{OBJECT_KEY}")
    print("预签名 GET（10 分钟）:")
    print(url)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
