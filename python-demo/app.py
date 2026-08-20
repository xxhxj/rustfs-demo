"""最简前端：上传文件，返回预签名预览地址。"""

from __future__ import annotations

from flask import Flask, jsonify, request, send_from_directory

from exam_report import upload_exam_reports
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


@app.get("/exam-report.html")
def exam_report_page():
    return send_from_directory(WEB_DIR, "exam-report.html")


@app.post("/api/exam-report/upload")
def api_exam_report_upload():
    files = [item for item in request.files.getlist("files") if item and item.filename]
    if not files:
        return fail("请选择文件")
    try:
        payload = upload_exam_reports(files, request.form.get("businessKey"))
        status = 200 if payload.get("ok") else 500
        return jsonify(payload), status
    except Exception as exc:
        return fail(str(exc), 500)


def main() -> None:
    print("Python 前端  http://127.0.0.1:18880")
    print("体检报告  http://127.0.0.1:18880/exam-report.html")
    app.run(host="127.0.0.1", port=18880, debug=False)


if __name__ == "__main__":
    main()
