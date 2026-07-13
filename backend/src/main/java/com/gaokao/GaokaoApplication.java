package com.gaokao;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gaokao.mapper")
public class GaokaoApplication {
    public static void main(String[] args) {
        SpringApplication.run(GaokaoApplication.class, args);
    }
}