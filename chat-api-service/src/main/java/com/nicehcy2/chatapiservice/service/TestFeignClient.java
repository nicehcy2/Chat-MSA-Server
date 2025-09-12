package com.nicehcy2.chatapiservice.service;

import com.nicehcy2.chatapiservice.config.OAuthFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "home-service",
        configuration = OAuthFeignConfig.class
)
public interface TestFeignClient {

    @GetMapping("/api/feign")
    String getFeign();
}
