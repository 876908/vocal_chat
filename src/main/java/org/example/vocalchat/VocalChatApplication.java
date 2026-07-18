package org.example.vocalchat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.vocalchat.mapper")
public class VocalChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(VocalChatApplication.class, args);
    }

}
