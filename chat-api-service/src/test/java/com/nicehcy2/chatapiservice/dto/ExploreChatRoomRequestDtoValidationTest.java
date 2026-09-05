package com.nicehcy2.chatapiservice.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExploreChatRoomRequestDtoValidationTest {

    static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    static Set<String> violatedFields(ExploreChatRoomRequestDto dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void 모든_조건이_없어도_위반이_없다() {
        assertTrue(violatedFields(ExploreChatRoomRequestDto.builder().build()).isEmpty());
    }

    // 검색어 상한. LIKE 풀스캔 비용과 로그 크기를 묶어두는 용도라 넉넉하게 50자
    @Test
    void 검색어가_50자를_넘으면_위반() {
        assertTrue(violatedFields(ExploreChatRoomRequestDto.builder().q("가".repeat(51)).build()).contains("q"));
        assertFalse(violatedFields(ExploreChatRoomRequestDto.builder().q("가".repeat(50)).build()).contains("q"));
    }
}
