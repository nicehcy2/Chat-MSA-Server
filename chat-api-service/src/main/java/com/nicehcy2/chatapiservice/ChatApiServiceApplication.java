package com.nicehcy2.chatapiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ChatApiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatApiServiceApplication.class, args);
	}

}
