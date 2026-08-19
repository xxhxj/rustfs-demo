package demo.rustfs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RustfsApplication {

    public static void main(String[] args) {
        SpringApplication.run(RustfsApplication.class, args);
        System.out.println("Java 前端  http://127.0.0.1:18881");
        System.out.println("桶 " + Rustfs.BUCKET + "  endpoint " + Rustfs.ENDPOINT);
    }
}
