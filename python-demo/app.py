"""最简前端：上传文件，返回预签名预览地址。"""

from __future__ import annotations

from flask import Flask, jsonify, request, send_from_directory

from rustfs_client import (
    PROJECT_ROOT,
    create_s3_client,
    ensure_bucket,
    env,
    load_env,
    presign_get,
    upload_fileobj,
)

load_env()

WEB_DIR = PROJECT_ROOT / "web"
app = Flask(__name__)
app.config["MAX_CONTENT_LENGTH"] = 64 * 1024 * 1024


def fail(message: str, status: int = 400):
    return jsonify({"ok": False, "error": message}), status


@app.get("/")
def index():
    return send_from_directory(WEB_DIR, "index.html")


@app.post("/api/upload")
def api_upload():
    uploaded = request.files.get("file")
    if uploaded is None or not uploaded.filename:
        return fail("请选择文件")
    filename = uploaded.filename.replace("\\", "/").rsplit("/", 1)[-1]
    key = f"demo/{filename}"
    bucket = env("RUSTFS_BUCKET", "python-demo")
    try:
        s3 = create_s3_client()
        ensure_bucket(s3, bucket)
        upload_fileobj(s3, bucket, key, uploaded, uploaded.mimetype)
        url = presign_get(s3, bucket, key)
        return jsonify({"ok": True, "bucket": bucket, "key": key, "url": url})
    except Exception as exc:
        return fail(str(exc), 500)


def main() -> None:
    print("Python 前端  http://127.0.0.1:18880")
    app.run(host="127.0.0.1", port=18880, debug=False)


if __name__ == "__main__":
    main()
