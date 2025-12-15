package ca.dtadmi.gamehubapi.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CacheConfigTest {

    @Autowired
    private CacheManager cacheManager;

    @Test
    void cachesAreAvailable() {
        assertCacheExists("leaderboard");
        assertCacheExists("userScores");
        assertCacheExists("featuredGames");
        assertCacheExists("lb_snake_global");
    }

    private void assertCacheExists(String name) {
        Cache cache = cacheManager.getCache(name);
        assertThat(cache)
                .as("Cache '%s' should be configured", name)
                .isNotNull();
    }
}
