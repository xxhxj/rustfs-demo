package demo.rustfs;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class Rustfs {

    private static final Map<String, String> FILE_ENV = readDotEnv();
    static final String ENDPOINT = env("RUSTFS_ENDPOINT", "http://127.0.0.1:19000");
    static final String ACCESS_KEY = env("RUSTFS_ACCESS_KEY", "rustfsadmin-local");
    static final String SECRET_KEY = env("RUSTFS_SECRET_KEY", "rustfssecret-local");
    static final String BUCKET = env("RUSTFS_BUCKET", "java-demo");

    private Rustfs() {
    }

    static String env(String name, String fallback) {
        String value = FILE_ENV.get(name);
        if (value == null || value.isEmpty()) {
            value = System.getenv(name);
        }
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static Map<String, String> readDotEnv() {
        Map<String, String> map = new LinkedHashMap<String, String>();
        Path[] candidates = new Path[] {
                Paths.get(".env"),
                Paths.get("java-demo", ".env"),
        };
        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                        continue;
                    }
                    int i = line.indexOf('=');
                    String key = line.substring(0, i).trim();
                    String val = line.substring(i + 1).trim();
                    if (val.length() >= 2 && ((val.startsWith("\"") && val.endsWith("\""))
                            || (val.startsWith("'") && val.endsWith("'")))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    map.put(key, val);
                }
                break;
            } catch (Exception ignored) {
                // 没有 .env 时使用代码里的默认值
            }
        }
        return Collections.unmodifiableMap(map);
    }

    static S3Client s3() {
        return S3Client.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials())
                .forcePathStyle(true)
                .build();
    }

    static S3Presigner presigner() {
        return S3Presigner.builder()
                .endpointOverride(URI.create(ENDPOINT))
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    static void ensureBucket(S3Client s3) {
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ignored) {
            // 桶已存在即可继续用
        }
    }

    static void upload(S3Client s3, String key, Path file) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType(contentTypeFor(key, null))
                        .build(),
                file);
    }

    static void upload(S3Client s3, String key, InputStream body, long length, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .contentType(contentTypeFor(key, contentType))
                        .build(),
                RequestBody.fromInputStream(body, length));
    }

    static String presignGet(S3Presigner presigner, String key, Duration ttl) {
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(BUCKET)
                                .key(key)
                                .responseContentType(contentTypeFor(key, null))
                                .build())
                        .signatureDuration(ttl)
                        .build())
                .url()
                .toString();
    }

    /** 文本类对象补上 charset=utf-8，否则浏览器预览会按系统默认编码（中文 Windows 常是 GBK）乱码。 */
    static String contentTypeFor(String key, String provided) {
        String type = provided == null ? "" : provided.trim();
        if (type.isEmpty() || "application/octet-stream".equalsIgnoreCase(type)) {
            type = guessContentType(key);
        }
        String lower = type.toLowerCase(Locale.ROOT);
        if (!lower.contains("charset=") && needsUtf8Charset(lower)) {
            return type + "; charset=utf-8";
        }
        return type;
    }

    private static boolean needsUtf8Charset(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("json")
                || contentType.contains("xml")
                || contentType.contains("javascript")
                || contentType.contains("svg");
    }

    private static String guessContentType(String key) {
        String name = key == null ? "" : key.toLowerCase(Locale.ROOT);
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return "text/html";
        }
        if (name.endsWith(".json")) {
            return "application/json";
        }
        if (name.endsWith(".xml")) {
            return "application/xml";
        }
        if (name.endsWith(".css")) {
            return "text/css";
        }
        if (name.endsWith(".js")) {
            return "text/javascript";
        }
        if (name.endsWith(".csv")) {
            return "text/csv";
        }
        if (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".md")) {
            return "text/plain";
        }
        if (name.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private static StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));
    }
}
