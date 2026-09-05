package com.nicehcy2.chatapiservice.repository;

import com.nicehcy2.chatapiservice.dto.ChatRoomHostDto;
import com.nicehcy2.chatapiservice.entity.AgeGroup;
import com.nicehcy2.chatapiservice.entity.ChatRoom;
import com.nicehcy2.chatapiservice.entity.ChatRoomMembership;
import com.nicehcy2.chatapiservice.entity.JobGroup;
import com.nicehcy2.chatapiservice.entity.User;
import com.nicehcy2.chatapiservice.entity.UserRole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 둘러보기 쿼리의 필터·정렬·커서를 실제 DB(H2)로 검증한다.
 * 서비스 단위 테스트는 mock이라 JPQL 자체의 동작은 여기서만 잡힌다.
 */
@DataJpaTest
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
class ChatRoomExploreRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ChatRoomRepository chatRoomRepository;
    @Autowired ChatRoomMembershipRepository membershipRepository;

    static final Pageable PAGE = PageRequest.of(0, 20);
    static final Set<AgeGroup> ANY_AGE = Set.of();
    static final Set<JobGroup> ANY_JOB = Set.of();

    ChatRoom room(String title, String description, Set<AgeGroup> ageGroups, Set<JobGroup> jobGroups) {
        return em.persistAndFlush(ChatRoom.builder()
                .title(title)
                .description(description)
                .maxParticipants(10)
                .participationCount(1)
                .dailyLimit(10_000)
                .ageGroups(Set.copyOf(ageGroups))
                .jobGroups(Set.copyOf(jobGroups))
                .build());
    }

    ChatRoom room(String title) {
        return room(title, null, ANY_AGE, ANY_JOB);
    }

    User user(String nickname, AgeGroup ageGroup, JobGroup jobGroup) {
        return em.persistAndFlush(User.builder()
                .nickname(nickname)
                .userRole(UserRole.USER)
                .gender("M")
                .ageGroup(ageGroup)
                .birthDay("2000-01-01")
                .jobGroup(jobGroup)
                .email(nickname + "@test.com")
                .password("x".repeat(60))
                .reward(0)
                .status(true)
                .dayTargetExpenditure(10_000)
                .build());
    }

    ChatRoomMembership membership(ChatRoom room, Long userId, boolean isHost, boolean isBanned, LocalDateTime leftAt) {
        return em.persistAndFlush(ChatRoomMembership.builder()
                .chatRoom(room)
                .userId(userId)
                .isHost(isHost)
                .isBanned(isBanned)
                .leftAt(leftAt)
                .build());
    }

    List<Long> ids(List<ChatRoom> rooms) {
        return rooms.stream().map(ChatRoom::getId).toList();
    }

    @Nested
    class findForExplore {

        @Test
        void 조건이_없으면_전체를_최신순_id_내림차순으로_돌려준다() {
            ChatRoom a = room("a");
            ChatRoom b = room("b");
            ChatRoom c = room("c");

            List<ChatRoom> result = chatRoomRepository.findForExplore(null, null, null, null, PAGE);

            assertEquals(List.of(c.getId(), b.getId(), a.getId()), ids(result));
        }

        @Test
        void before_이전_id만_돌려주고_before_자체는_제외한다() {
            ChatRoom a = room("a");
            ChatRoom b = room("b");
            ChatRoom c = room("c");

            List<ChatRoom> result = chatRoomRepository.findForExplore(null, null, null, b.getId(), PAGE);

            assertEquals(List.of(a.getId()), ids(result));
            assertFalse(ids(result).contains(c.getId()));
        }

        @Test
        void before가_존재하지_않는_id여도_그_값보다_작은_id를_돌려준다() {
            // 커서로 쓰인 방이 삭제됐거나(최후 1인 퇴장), 클라이언트가 임의 값을 보내도 id 비교만 하면 된다
            ChatRoom a = room("a");
            ChatRoom deleted = room("deleted");
            ChatRoom c = room("c");
            Long deletedId = deleted.getId();
            em.remove(deleted);
            em.flush();

            List<ChatRoom> afterDeleted = chatRoomRepository.findForExplore(null, null, null, deletedId, PAGE);
            List<ChatRoom> afterHuge = chatRoomRepository.findForExplore(null, null, null, Long.MAX_VALUE, PAGE);

            assertEquals(List.of(a.getId()), ids(afterDeleted));
            assertEquals(List.of(c.getId(), a.getId()), ids(afterHuge));
        }

        @Test
        void limit만큼만_돌려준다() {
            room("a");
            room("b");
            ChatRoom c = room("c");

            List<ChatRoom> result = chatRoomRepository.findForExplore(null, null, null, null, PageRequest.of(0, 1));

            assertEquals(List.of(c.getId()), ids(result));
        }

        @Test
        void ageGroup_필터는_해당_값을_포함한_방과_대상_미지정_방을_돌려준다() {
            ChatRoom twenties = room("20대", null, Set.of(AgeGroup.TWENTIES), ANY_JOB);
            ChatRoom twentiesAndThirties = room("20·30대", null, Set.of(AgeGroup.TWENTIES, AgeGroup.THIRTIES), ANY_JOB);
            ChatRoom thirtiesOnly = room("30대", null, Set.of(AgeGroup.THIRTIES), ANY_JOB);
            ChatRoom any = room("전체");

            List<Long> result = ids(chatRoomRepository.findForExplore(null, AgeGroup.TWENTIES, null, null, PAGE));

            assertTrue(result.containsAll(List.of(twenties.getId(), twentiesAndThirties.getId(), any.getId())));
            assertFalse(result.contains(thirtiesOnly.getId()));
        }

        @Test
        void jobGroup_필터는_해당_값을_포함한_방과_대상_미지정_방을_돌려준다() {
            ChatRoom employee = room("직장인", null, ANY_AGE, Set.of(JobGroup.EMPLOYEE));
            ChatRoom studentOnly = room("학생", null, ANY_AGE, Set.of(JobGroup.STUDENT));
            ChatRoom any = room("전체");

            List<Long> result = ids(chatRoomRepository.findForExplore(null, null, JobGroup.EMPLOYEE, null, PAGE));

            assertTrue(result.containsAll(List.of(employee.getId(), any.getId())));
            assertFalse(result.contains(studentOnly.getId()));
        }

        @Test
        void 두_필터를_함께_주면_AND로_적용된다() {
            ChatRoom both = room("20대 직장인", null, Set.of(AgeGroup.TWENTIES), Set.of(JobGroup.EMPLOYEE));
            ChatRoom ageOnly = room("20대 학생", null, Set.of(AgeGroup.TWENTIES), Set.of(JobGroup.STUDENT));
            ChatRoom jobOnly = room("30대 직장인", null, Set.of(AgeGroup.THIRTIES), Set.of(JobGroup.EMPLOYEE));
            ChatRoom any = room("전체");

            List<Long> result = ids(chatRoomRepository.findForExplore(
                    null, AgeGroup.TWENTIES, JobGroup.EMPLOYEE, null, PAGE));

            assertTrue(result.containsAll(List.of(both.getId(), any.getId())));
            assertFalse(result.contains(ageOnly.getId()));
            assertFalse(result.contains(jobOnly.getId()));
        }

        @Test
        void 검색어는_제목_또는_설명에_포함되면_매칭된다() {
            ChatRoom inTitle = room("무지출 챌린지", "하루 만원", ANY_AGE, ANY_JOB);
            ChatRoom inDescription = room("절약방", "무지출로 살아남기", ANY_AGE, ANY_JOB);
            ChatRoom none = room("배달 끊기", "집밥", ANY_AGE, ANY_JOB);

            List<Long> result = ids(chatRoomRepository.findForExplore("무지출", null, null, null, PAGE));

            assertTrue(result.containsAll(List.of(inTitle.getId(), inDescription.getId())));
            assertFalse(result.contains(none.getId()));
        }

        @Test
        void 이스케이프된_와일드카드는_문자_그대로_매칭된다() {
            // 서비스가 %·_·!를 '!'로 이스케이프해서 넘긴다(ChatApiServiceTest 참고). 쿼리는 ESCAPE '!'로 받는다
            ChatRoom percent = room("일 100% 인증", null, ANY_AGE, ANY_JOB);
            ChatRoom noPercent = room("일 100원 인증", null, ANY_AGE, ANY_JOB);
            ChatRoom underscore = room("no_spend", null, ANY_AGE, ANY_JOB);
            ChatRoom noUnderscore = room("nospend", null, ANY_AGE, ANY_JOB);

            List<Long> percentResult = ids(chatRoomRepository.findForExplore("100!%", null, null, null, PAGE));
            List<Long> underscoreResult = ids(chatRoomRepository.findForExplore("no!_spend", null, null, null, PAGE));

            assertEquals(List.of(percent.getId()), percentResult);
            assertFalse(percentResult.contains(noPercent.getId()));
            assertEquals(List.of(underscore.getId()), underscoreResult);
            assertFalse(underscoreResult.contains(noUnderscore.getId()));
        }

        @Test
        void 검색어에_맞는_방이_없으면_빈_리스트다() {
            room("무지출 챌린지");

            List<ChatRoom> result = chatRoomRepository.findForExplore("없는단어", null, null, null, PAGE);

            assertTrue(result.isEmpty());
        }

        @Test
        void 검색어와_필터를_함께_주면_AND로_적용된다() {
            ChatRoom match = room("무지출 20대", null, Set.of(AgeGroup.TWENTIES), ANY_JOB);
            ChatRoom wrongAge = room("무지출 30대", null, Set.of(AgeGroup.THIRTIES), ANY_JOB);
            ChatRoom wrongWord = room("절약 20대", null, Set.of(AgeGroup.TWENTIES), ANY_JOB);

            List<Long> result = ids(chatRoomRepository.findForExplore("무지출", AgeGroup.TWENTIES, null, null, PAGE));

            assertEquals(List.of(match.getId()), result);
            assertFalse(result.contains(wrongAge.getId()));
            assertFalse(result.contains(wrongWord.getId()));
        }

        @Test
        void 검색어와_커서를_함께_주면_커서_이전만_돌려준다() {
            ChatRoom older = room("무지출 1");
            ChatRoom cursor = room("무지출 2");
            room("무지출 3");

            List<Long> result = ids(chatRoomRepository.findForExplore("무지출", null, null, cursor.getId(), PAGE));

            assertEquals(List.of(older.getId()), result);
        }

        @Test
        void 멤버십_상태와_무관하게_방이_나온다() {
            // 참여 중·퇴장·강퇴 판정은 서비스와 프론트가 맡는다. 쿼리는 멤버십을 보지 않는다
            ChatRoom joined = room("참여 중");
            ChatRoom left = room("퇴장");
            ChatRoom banned = room("강퇴");
            membership(joined, 7L, false, false, null);
            membership(left, 7L, false, false, LocalDateTime.now());
            membership(banned, 7L, false, true, null);

            List<Long> result = ids(chatRoomRepository.findForExplore(null, null, null, null, PAGE));

            assertTrue(result.containsAll(List.of(joined.getId(), left.getId(), banned.getId())));
        }
    }

    @Nested
    class findHostsByChatRoomIds {

        @Test
        void 방_id_목록의_활성_호스트를_유저_정보와_함께_한_번에_돌려준다() {
            User host1 = user("host1", AgeGroup.THIRTIES, JobGroup.EMPLOYEE);
            User host2 = user("host2", AgeGroup.TWENTIES, JobGroup.STUDENT);
            User member = user("member", AgeGroup.FORTIES, JobGroup.HOMEMAKER);
            ChatRoom r1 = room("r1");
            ChatRoom r2 = room("r2");
            membership(r1, host1.getUserId(), true, false, null);
            membership(r1, member.getUserId(), false, false, null);
            membership(r2, host2.getUserId(), true, false, null);

            List<ChatRoomHostDto> result = membershipRepository.findHostsByChatRoomIds(List.of(r1.getId(), r2.getId()));

            assertEquals(2, result.size());
            assertTrue(result.contains(new ChatRoomHostDto(
                    r1.getId(), host1.getUserId(), "host1", null, AgeGroup.THIRTIES, JobGroup.EMPLOYEE)));
            assertTrue(result.contains(new ChatRoomHostDto(
                    r2.getId(), host2.getUserId(), "host2", null, AgeGroup.TWENTIES, JobGroup.STUDENT)));
        }

        @Test
        void 목록에_없는_방의_호스트는_돌려주지_않는다() {
            User host = user("host", AgeGroup.THIRTIES, JobGroup.EMPLOYEE);
            ChatRoom asked = room("asked");
            ChatRoom other = room("other");
            membership(asked, host.getUserId(), true, false, null);
            membership(other, host.getUserId(), true, false, null);

            List<ChatRoomHostDto> result = membershipRepository.findHostsByChatRoomIds(List.of(asked.getId()));

            assertEquals(1, result.size());
            assertEquals(asked.getId(), result.get(0).chatRoomId());
        }

        @Test
        void 나갔거나_강퇴된_호스트_멤버십은_돌려주지_않는다() {
            // 호스트 위임 전 상태 방어. 방은 목록에 남고 host만 null이 된다(서비스 테스트 참고)
            User host = user("host", AgeGroup.THIRTIES, JobGroup.EMPLOYEE);
            ChatRoom leftRoom = room("left");
            ChatRoom bannedRoom = room("banned");
            membership(leftRoom, host.getUserId(), true, false, LocalDateTime.now());
            membership(bannedRoom, host.getUserId(), true, true, null);

            List<ChatRoomHostDto> result = membershipRepository.findHostsByChatRoomIds(
                    List.of(leftRoom.getId(), bannedRoom.getId()));

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class findBannedChatRoomIds {

        @Test
        void 강퇴된_방_id만_돌려주고_leftAt_유무는_보지_않는다() {
            ChatRoom bannedActive = room("banned-active");
            ChatRoom bannedLeft = room("banned-left");
            ChatRoom joined = room("joined");
            ChatRoom left = room("left");
            membership(bannedActive, 7L, false, true, null);
            membership(bannedLeft, 7L, false, true, LocalDateTime.now());
            membership(joined, 7L, false, false, null);
            membership(left, 7L, false, false, LocalDateTime.now());

            List<Long> result = membershipRepository.findBannedChatRoomIds(7L);

            assertEquals(Set.of(bannedActive.getId(), bannedLeft.getId()), Set.copyOf(result));
        }

        @Test
        void 다른_유저의_강퇴는_영향을_주지_않는다() {
            ChatRoom room = room("r");
            membership(room, 8L, false, true, null);

            List<Long> result = membershipRepository.findBannedChatRoomIds(7L);

            assertTrue(result.isEmpty());
        }
    }
}
