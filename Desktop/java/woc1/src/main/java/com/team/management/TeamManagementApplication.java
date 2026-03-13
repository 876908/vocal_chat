package com.team.management;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.team.management.mapper")
public class TeamManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(TeamManagementApplication.class, args);
    }
}