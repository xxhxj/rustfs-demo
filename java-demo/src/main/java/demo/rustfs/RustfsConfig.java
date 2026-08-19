package demo.rustfs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class RustfsConfig {

    @Bean(destroyMethod = "close")
    public S3Client s3Client() {
        S3Client s3 = Rustfs.s3();
        Rustfs.ensureBucket(s3);
        return s3;
    }

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner() {
        return Rustfs.presigner();
    }
}
