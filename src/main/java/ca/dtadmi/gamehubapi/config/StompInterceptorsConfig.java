package ca.dtadmi.gamehubapi.config;

import ca.dtadmi.gamehubapi.interceptor.StompRateLimitInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.support.ChannelInterceptor;

@Configuration
public class StompInterceptorsConfig {

    @Bean
    @ConditionalOnProperty(name = "features.kv.redis_enabled", havingValue = "true", matchIfMissing = false)
    @Qualifier("stompRateLimiterInterceptor")
    public ChannelInterceptor stompRateLimiterInterceptor(StringRedisTemplate redis, Environment env) {
        return new StompRateLimitInterceptor(redis, env);
    }
}
