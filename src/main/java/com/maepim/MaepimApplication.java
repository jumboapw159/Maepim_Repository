package com.maepim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.maepim.repository")
public class MaepimApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaepimApplication.class, args);
    }

}