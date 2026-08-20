package demo.rustfs.exam;

import demo.rustfs.ExamEnv;
import demo.rustfs.Rustfs;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

final class ExamS3 implements AutoCloseable {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    ExamS3() {
        this.bucket = ExamEnv.bucket();
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ExamEnv.accessKey(), ExamEnv.secretKey()));
        URI endpoint = URI.create(ExamEnv.endpoint());
        this.s3 = S3Client.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .forcePathStyle(true)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(Region.US_EAST_1)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    String bucket() {
        return bucket;
    }

    void upload(String key, InputStream body, long length, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(Rustfs.contentTypeFor(key, contentType))
                        .build(),
                RequestBody.fromInputStream(body, length));
    }

    String presignGet(String key, Duration ttl) {
        return presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .getObjectRequest(GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .responseContentType(Rustfs.contentTypeFor(key, null))
                                .build())
                        .signatureDuration(ttl)
                        .build())
                .url()
                .toString();
    }

    @Override
    public void close() {
        s3.close();
        presigner.close();
    }
}
