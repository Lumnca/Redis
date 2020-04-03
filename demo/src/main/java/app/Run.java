package app;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;

@EnableCaching
@SpringBootApplication
@EnableHystrix
@MapperScan("app")
public class Run {
    public static void main(String[] args){
        SpringApplication.run(Run.class,args);
    }
}
