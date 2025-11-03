package com.nicehcy2.gatewayserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class ApiGatewayServerApplication {

	public static void main(String[] args) {

		Hooks.enableAutomaticContextPropagation();
		SpringApplication.run(ApiGatewayServerApplication.class, args);
	}

}
