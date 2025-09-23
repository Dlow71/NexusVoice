package com.nexusvoice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nexusvoice.infrastructure.database.mapper")
public class NexusVoiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexusVoiceApplication.class, args);
    }
}

