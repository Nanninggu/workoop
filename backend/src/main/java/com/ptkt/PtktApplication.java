package com.ptkt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ptkt.mapper")
public class PtktApplication {
    public static void main(String[] args) {
        SpringApplication.run(PtktApplication.class, args);
    }
}
