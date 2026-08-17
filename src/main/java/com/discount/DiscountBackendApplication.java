package com.discount;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.discount", "Controller", "function", "Repo", "Request"})
@EntityScan(basePackages = {"function", "Repo"})
@EnableJpaRepositories(basePackages = "Repo")
public class DiscountBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(DiscountBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner logActiveDatasource(Environment env) {
        return args -> {
            System.out.println("=================================================");
            System.out.println("Active DB URL: " + env.getProperty("spring.datasource.url"));
            System.out.println("Active DB user: " + env.getProperty("spring.datasource.username"));
            System.out.println("=================================================");
        };
    }
}