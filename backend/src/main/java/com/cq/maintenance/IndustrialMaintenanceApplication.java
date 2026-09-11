package com.cq.maintenance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class IndustrialMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndustrialMaintenanceApplication.class, args);
    }
}

