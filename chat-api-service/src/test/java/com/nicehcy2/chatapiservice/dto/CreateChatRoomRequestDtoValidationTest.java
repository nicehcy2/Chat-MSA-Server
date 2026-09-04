package com.nicehcy2.chatapiservice.dto;

import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 단일 필드 규칙(@Valid 어노테이션)만 검증한다. 스프링 컨텍스트 없이 Validator를 직접 돌린다.
 * "비공개면 비밀번호 필수" 같은 필드 간 규칙은 서비스 테스트(ChatRoomServiceTest)에서 다룬다.
 */
class CreateChatRoomRequestDtoValidationTest {

    static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    static CreateChatRoomRequestDto.CreateChatRoomRequestDtoBuilder valid() {
        return CreateChatRoomRequestDto.builder()
                .title("무지출 챌린지")
                .description("하루 만원으로 살기")
                .maxParticipants(10)
                .isPrivate(false)
                .ageGroups(List.of(AgeGroup.TWENTIES))
                .jobGroups(List.of(JobGroup.EMPLOYEE))
                .dailyLimit(10_000);
    }

    static Set<String> violatedFields(CreateChatRoomRequestDto dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    @Test
    void 정상_요청은_위반이_없다() {
        assertTrue(validator.validate(valid().build()).isEmpty());
    }

    @Test
    void description은_비어있어도_된다() {
        assertFalse(violatedFields(valid().description("").build()).contains("description"));
        assertFalse(violatedFields(valid().description(null).build()).contains("description"));
    }

    @Test
    void imageUrl은_없어도_된다() {
        assertFalse(violatedFields(valid().imageUrl(null).build()).contains("imageUrl"));
    }

    // ----- imageUrl: 선택, 최대 255자 (컬럼 기본 길이. 넘기면 DB 에러 → 500) -----

    @Test
    void imageUrl이_255자를_넘으면_위반() {
        String base = "https://cdn.example.com/";
        assertTrue(violatedFields(valid().imageUrl(base + "a".repeat(256 - base.length())).build()).contains("imageUrl"));
        assertFalse(violatedFields(valid().imageUrl(base + "a".repeat(255 - base.length())).build()).contains("imageUrl"));
    }

    // ----- title: 필수, 1~18자 (엔티티 length=18) -----

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void title이_비어있거나_공백만이면_위반(String title) {
        assertTrue(violatedFields(valid().title(title).build()).contains("title"));
    }

    @Test
    void title이_null이면_위반() {
        assertTrue(violatedFields(valid().title(null).build()).contains("title"));
    }

    @Test
    void title이_18자를_넘으면_위반() {
        assertTrue(violatedFields(valid().title("가".repeat(19)).build()).contains("title"));
        assertFalse(violatedFields(valid().title("가".repeat(18)).build()).contains("title"));
    }

    // ----- description: 선택, 최대 200자 -----

    @Test
    void description이_200자를_넘으면_위반() {
        assertTrue(violatedFields(valid().description("가".repeat(201)).build()).contains("description"));
        assertFalse(violatedFields(valid().description("가".repeat(200)).build()).contains("description"));
    }

    // ----- maxParticipants: 1~100 -----

    @Test
    void maxParticipants가_null이면_위반() {
        assertTrue(violatedFields(valid().maxParticipants(null).build()).contains("maxParticipants"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void maxParticipants가_범위를_벗어나면_위반(int count) {
        assertTrue(violatedFields(valid().maxParticipants(count).build()).contains("maxParticipants"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void maxParticipants_경계값은_허용(int count) {
        assertFalse(violatedFields(valid().maxParticipants(count).build()).contains("maxParticipants"));
    }

    // ----- password: 있다면 숫자 4자리. 필수 여부는 isPrivate에 달려 있어 서비스에서 판정 -----

    @ParameterizedTest
    @ValueSource(strings = {"123", "12345", "12a4", "abcd", "１２３４", "12 4"})
    void password가_숫자_4자리가_아니면_위반(String password) {
        assertTrue(violatedFields(valid().isPrivate(true).password(password).build()).contains("password"));
    }

    @Test
    void password_숫자_4자리는_허용() {
        assertFalse(violatedFields(valid().isPrivate(true).password("0000").build()).contains("password"));
    }

    @Test
    void password_null은_형식_검증을_통과한다() {
        // 공개방은 password가 없는 게 정상. 비공개방의 필수 여부는 서비스가 판정
        assertFalse(violatedFields(valid().password(null).build()).contains("password"));
    }

    // ----- isPrivate: 필수 -----

    @Test
    void isPrivate가_null이면_위반() {
        assertTrue(violatedFields(valid().isPrivate(null).build()).contains("isPrivate"));
    }

    // ----- dailyLimit: 필수, 0 이상. 상한은 두지 않는다 -----

    @Test
    void dailyLimit이_null이면_위반() {
        assertTrue(violatedFields(valid().dailyLimit(null).build()).contains("dailyLimit"));
    }

    @Test
    void dailyLimit이_음수면_위반() {
        assertTrue(violatedFields(valid().dailyLimit(-1).build()).contains("dailyLimit"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1_000_000_000})
    void dailyLimit은_0과_아주_큰_값도_허용(int limit) {
        assertFalse(violatedFields(valid().dailyLimit(limit).build()).contains("dailyLimit"));
    }
}
