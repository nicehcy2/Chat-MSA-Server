package com.nicehcy.chatservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;

@SpringBootApplication
@RefreshScope
public class ChatServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}
