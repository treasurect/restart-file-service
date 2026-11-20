package com.treasure.restart.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.treasure.restart.file.mapper")
public class RestartFileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RestartFileServiceApplication.class, args);
    }

}
