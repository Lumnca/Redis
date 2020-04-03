package app.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;


import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RedisCacheConfig {

    @Autowired
    RedisConnectionFactory connectionFactory;

    @Bean
    @Primary
    RedisCacheManager redisCacheManager(){
        Map<String,RedisCacheConfiguration> configurationMap = new HashMap<>();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().prefixKeysWith("sang:")
                .disableCachingNullValues().entryTtl(Duration.ofMinutes(30));

        configurationMap.put("c1",configuration);
        RedisCacheWriter cacheWriter =
                RedisCacheWriter.nonLockingRedisCacheWriter(connectionFactory);
                RedisCacheManager redisCacheManager= new RedisCacheManager(cacheWriter, RedisCacheConfiguration. defaultCacheConfig(), configurationMap);
                return redisCacheManager;
    }
    @Bean
    public EhCacheCacheManager eCacheCacheManager() {
        return new EhCacheCacheManager();
    }
}
