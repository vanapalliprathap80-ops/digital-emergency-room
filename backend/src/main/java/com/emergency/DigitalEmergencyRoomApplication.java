package com.emergency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class DigitalEmergencyRoomApplication {
    public static void main(String[] args) {
        SpringApplication.run(DigitalEmergencyRoomApplication.class, args);
    }
}
