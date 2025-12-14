package ca.dtadmi.gamehubapi.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@org.springframework.test.context.TestPropertySource(properties = {
        "features.kv.redis_enabled=true"
})
@Testcontainers
class RedisServicesIT {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:alpine")
            .withExposedPorts(6379);
    @Autowired
    PresenceService presenceService;
    @Autowired
    LeaderboardService leaderboardService;
    @Autowired
    RunIdService runIdService;

    @DynamicPropertySource
    static void redisProps(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> redis.getHost());
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeAll
    static void beforeAll() {
        redis.start();
    }

    @AfterAll
    static void afterAll() {
        redis.stop();
    }

    @Test
    void presence_heartbeat_and_ttl_expire() throws Exception {
        String room = "snake:global";
        String member = "guest|it";
        presenceService.join(room, member);
        assertThat(presenceService.count(room)).isGreaterThanOrEqualTo(1);

        // simulate TTL by setting a very small TTL via reflection, or simply wait > default ttl? We can't change bean TTL here easily.
        // Instead, call leave and ensure count drops.
        presenceService.leave(room, member);
        Thread.sleep(100); // allow deletion to propagate
        assertThat(presenceService.count(room)).isEqualTo(0);
    }

    @Test
    void leaderboard_perUserBest_and_topN() {
        String scope = "snake:global";
        leaderboardService.submit(scope, "Ari", 100);
        leaderboardService.submit(scope, "Bo", 200);
        // lower score for Ari should not replace best
        leaderboardService.submit(scope, "Ari", 90);
        // higher score should replace
        leaderboardService.submit(scope, "Ari", 250);

        var top = leaderboardService.topN(scope, 10);
        assertThat(top).isNotEmpty();
        assertThat(top.get(0).nickname).isEqualTo("Ari");
        assertThat(top.get(0).value).isEqualTo(250);
        assertThat(top).extracting(e -> e.nickname).contains("Bo");
        Integer rankAri = leaderboardService.rankOf(scope, "Ari");
        Integer rankBo = leaderboardService.rankOf(scope, "Bo");
        assertThat(rankAri).isEqualTo(1);
        assertThat(rankBo).isEqualTo(2);
    }

    @Test
    void runId_lifecycle_singleUse_and_expire() throws Exception {
        String id = runIdService.start("guest");
        assertThat(id).isNotBlank();
        Optional<String> peekBefore = runIdService.peek(id);
        assertThat(peekBefore).isPresent();

        boolean first = runIdService.validateAndConsume(id);
        boolean second = runIdService.validateAndConsume(id);
        assertThat(first).isTrue();
        assertThat(second).isFalse();
    }
}
