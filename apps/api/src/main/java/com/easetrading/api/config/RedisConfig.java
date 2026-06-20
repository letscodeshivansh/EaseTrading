package com.easetrading.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Redis wiring.
 *
 *  - StringRedisTemplate         : read/write the hot "quote:{token}" cache and
 *                                  PUBLISH live ticks.
 *  - RedisMessageListenerContainer: lets the WebSocket handler SUBSCRIBE to the
 *                                  ticks channel and fan messages out to browsers.
 *
 * Using Redis pub/sub (instead of pushing directly to sockets) means we can run
 * several api instances later and every instance still receives every tick.
 */
@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean
    public RedisMessageListenerContainer redisListenerContainer(RedisConnectionFactory cf) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(cf);
        return container;
    }
}
