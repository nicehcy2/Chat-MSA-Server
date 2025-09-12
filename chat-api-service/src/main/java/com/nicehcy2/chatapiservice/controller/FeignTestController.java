package com.nicehcy2.chatapiservice.controller;

import com.nicehcy2.chatapiservice.service.TestFeignClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FeignTestController {
    private final TestFeignClient testFeignClient;

    @GetMapping("/feign-test")
    public String getFeign() {
        return testFeignClient.getFeign(); // ← Feign으로 home-service 호출
    }
}

