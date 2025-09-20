package com.back_hexiang_studio;


import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.back_hexiang_studio.mapper")
@EnableScheduling
@EnableAsync
public class  BackHexiangStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackHexiangStudioApplication.class, args);
    }

}
