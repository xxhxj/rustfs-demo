package demo.rustfs;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * 本机 RustFS 最简 Java demo：上传文件 + 生成预签名 GET 地址。
 * 连接参数见同目录 .env / README.md。
 */
public class UploadPresignDemo {

    private static final String OBJECT_KEY = "demo/hello.txt";

    public static void main(String[] args) {
        Path localFile = resolveSample(args);
        if (!Files.isRegularFile(localFile)) {
            System.err.println("找不到待上传文件: " + localFile.toAbsolutePath());
            System.exit(1);
        }

        try (S3Client s3 = Rustfs.s3(); S3Presigner presigner = Rustfs.presigner()) {
            Rustfs.ensureBucket(s3);
            Rustfs.upload(s3, OBJECT_KEY, localFile);
            System.out.println("已上传: " + localFile.toAbsolutePath()
                    + " -> s3://" + Rustfs.BUCKET + "/" + OBJECT_KEY);
            System.out.println("预签名 GET（10 分钟）:");
            System.out.println(Rustfs.presignGet(presigner, OBJECT_KEY, Duration.ofMinutes(10)));
        }
    }

    private static Path resolveSample(String[] args) {
        if (args.length > 0) {
            return Paths.get(args[0]);
        }
        Path fromModule = Paths.get("samples", "hello.txt");
        if (Files.isRegularFile(fromModule)) {
            return fromModule;
        }
        return Paths.get("java-demo", "samples", "hello.txt");
    }
}
