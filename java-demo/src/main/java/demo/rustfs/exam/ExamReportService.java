package demo.rustfs.exam;

import demo.rustfs.ExamEnv;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExamReportService {

    private final TbAttachmentMapper mapper;
    private final ExamS3 examS3 = new ExamS3();

    public ExamReportService(TbAttachmentMapper mapper) {
        this.mapper = mapper;
    }

    public Map<String, Object> upload(List<MultipartFile> files, String businessKey) {
        List<Map<String, Object>> items = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> errors = new ArrayList<Map<String, Object>>();
        String key = businessKey == null ? "" : businessKey.trim();
        if (key.length() > 50) {
            key = key.substring(0, 50);
        }
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String original = originalName(file.getOriginalFilename());
            try {
                items.add(uploadOne(file, original, key));
            } catch (Exception ex) {
                Map<String, Object> err = new LinkedHashMap<String, Object>();
                err.put("fileName", original);
                err.put("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
                errors.add(err);
            }
        }
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("ok", !items.isEmpty());
        body.put("bucket", examS3.bucket());
        body.put("businessType", ExamEnv.businessType());
        body.put("items", items);
        body.put("errors", errors);
        return body;
    }

    private Map<String, Object> uploadOne(MultipartFile file, String original, String businessKey) throws Exception {
        String id = UUID.randomUUID().toString().replace("-", "");
        Date now = new Date();
        String day = new SimpleDateFormat("yyyyMMdd").format(now);
        String org = ExamEnv.yydh();
        String storedName = clip(org + "_" + day + "_" + original, 255);
        String filePath = org + "/" + day + "/" + id + "~" + original;
        if (filePath.length() > 500) {
            filePath = filePath.substring(0, 500);
        }
        Timestamp ts = new Timestamp(now.getTime());
        String mime = file.getContentType();

        InputStream in = file.getInputStream();
        try {
            examS3.upload(filePath, in, file.getSize(), mime);
        } finally {
            in.close();
        }

        TbAttachment row = new TbAttachment();
        row.setId(id);
        row.setFileName(storedName);
        row.setFilePath(filePath);
        row.setFileSize(Long.valueOf(file.getSize()));
        row.setUploaderId(clip(ExamEnv.uploaderId(), 36));
        row.setUploadTime(ts);
        row.setBusinessType(ExamEnv.businessType());
        row.setBusinessKey(businessKey);
        row.setMimeType(clip(mime == null || mime.isEmpty() ? "application/octet-stream" : mime, 100));
        row.setVtag(Integer.valueOf(0));
        row.setCreatedTime(ts);
        row.setUpdatedTime(ts);
        row.setYydh(clip(org, 36));
        row.setActualFileName(clip(original, 128));
        mapper.insert(row);

        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("id", id);
        item.put("fileName", storedName);
        item.put("filePath", filePath);
        item.put("fileSize", Long.valueOf(file.getSize()));
        item.put("businessKey", businessKey);
        item.put("url", examS3.presignGet(filePath, Duration.ofMinutes(10)));
        return item;
    }

    private static String originalName(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "unnamed";
        }
        String name = raw.replace("\\", "/");
        name = name.substring(name.lastIndexOf('/') + 1).trim();
        name = name.replace("~", "_");
        return name.isEmpty() ? "unnamed" : name;
    }

    private static String clip(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
