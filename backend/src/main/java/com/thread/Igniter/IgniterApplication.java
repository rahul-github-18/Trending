package com.thread.Igniter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class IgniterApplication {

	public static void main(String[] args) {
		SpringApplication.run(IgniterApplication.class, args);
	}

}
