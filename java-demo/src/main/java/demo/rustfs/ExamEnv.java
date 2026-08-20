package demo.rustfs;

/**
 * 体检报告场景的独立配置，和本机 java-demo 桶解耦。
 * 连接参数见 .env 里 EXAM_* / EXAM_DM_*。
 */
public final class ExamEnv {

    private ExamEnv() {
    }

    public static String endpoint() {
        return Rustfs.env("EXAM_REPORT_ENDPOINT", Rustfs.ENDPOINT);
    }

    public static String accessKey() {
        return Rustfs.env("EXAM_REPORT_ACCESS_KEY", "ACb2x3y2t1PuowZEErYb");
    }

    public static String secretKey() {
        return Rustfs.env("EXAM_REPORT_SECRET_KEY", "mIWUaAJvNek1SwMN1R6TjPcnRw92wgF9rj8t0zeN");
    }

    public static String bucket() {
        return Rustfs.env("EXAM_REPORT_BUCKET", "inesa-bg-examreport");
    }

    public static String yydh() {
        return Rustfs.env("EXAM_REPORT_YYDH", "A");
    }

    public static String uploaderId() {
        return Rustfs.env("EXAM_REPORT_UPLOADER_ID", "demo-exam-report-uploader");
    }

    public static String businessType() {
        return "EXAM_REPORT_BUCKET";
    }

    public static String dmUrl() {
        return Rustfs.env("EXAM_DM_URL",
                "jdbc:dm://127.0.0.1:5236?schema=OP_MANAGE&compatibleMode=oracle");
    }

    public static String dmUser() {
        return Rustfs.env("EXAM_DM_USER", "SYSDBA");
    }

    public static String dmPassword() {
        return Rustfs.env("EXAM_DM_PASSWORD", "inspur123@A");
    }

    public static String dmDriver() {
        return Rustfs.env("EXAM_DM_DRIVER", "dm.jdbc.driver.DmDriver");
    }
}
