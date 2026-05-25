package com.teams_tracking_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TeamsTrackingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamsTrackingApiApplication.class, args);
	}

}
