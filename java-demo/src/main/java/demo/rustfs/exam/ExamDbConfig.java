package demo.rustfs.exam;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.zaxxer.hikari.HikariDataSource;
import demo.rustfs.ExamEnv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@MapperScan("demo.rustfs.exam")
public class ExamDbConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setPoolName("exam-dm");
        ds.setJdbcUrl(ExamEnv.dmUrl());
        ds.setUsername(ExamEnv.dmUser());
        ds.setPassword(ExamEnv.dmPassword());
        ds.setDriverClassName(ExamEnv.dmDriver());
        ds.setMaximumPoolSize(5);
        ds.setInitializationFailTimeout(-1);
        return ds;
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.DM));
        return interceptor;
    }
}
