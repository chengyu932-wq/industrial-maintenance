package com.cq.maintenance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@org.springframework.cache.annotation.EnableCaching
@Configuration
public class RedisConfig {

    @Bean public org.springframework.data.redis.cache.RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory){
        var config=org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(java.time.Duration.ofMinutes(10)).disableCachingNullValues()
            .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        return org.springframework.data.redis.cache.RedisCacheManager.builder(connectionFactory).cacheDefaults(config).transactionAware().build();
    }
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }
}

