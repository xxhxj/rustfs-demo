package demo.rustfs.exam;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class ExamReportController {

    private final ExamReportService service;

    public ExamReportController(ExamReportService service) {
        this.service = service;
    }

    @PostMapping("/api/exam-report/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "businessKey", required = false) String businessKey) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(error("请选择文件"));
        }
        Map<String, Object> body = service.upload(files, businessKey);
        if (Boolean.TRUE.equals(body.get("ok"))) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(500).body(body);
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
