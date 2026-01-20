package com.mfon.rmhi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RmhiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RmhiApplication.class, args);
	}

}
