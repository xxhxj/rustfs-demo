package demo.rustfs;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class UploadController {

    private final S3Client s3;
    private final S3Presigner presigner;

    public UploadController(S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        this.presigner = presigner;
    }

    @PostMapping("/api/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null) {
            return ResponseEntity.badRequest().body(error("请选择文件"));
        }
        String filename = file.getOriginalFilename().replace("\\", "/");
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        String key = "demo/" + filename;
        java.io.InputStream in = file.getInputStream();
        try {
            Rustfs.upload(s3, key, in, file.getSize(), file.getContentType());
        } finally {
            in.close();
        }
        String url = Rustfs.presignGet(presigner, key, Duration.ofMinutes(10));

        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("ok", true);
        body.put("bucket", Rustfs.BUCKET);
        body.put("key", key);
        body.put("url", url);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(500).body(error(message));
    }

    private static Map<String, Object> error(String message) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("ok", false);
        body.put("error", message);
        return body;
    }
}
