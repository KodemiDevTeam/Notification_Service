package org.notification.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfig = new RedisConfig();
    }

    @Test
    void cacheManager_returnsNonNullCacheManager() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        CacheManager cacheManager = redisConfig.cacheManager(connectionFactory);
        assertNotNull(cacheManager);
    }

    @Test
    void errorHandler_handlesAllCacheErrorsWithoutThrowing() {
        CacheErrorHandler handler = redisConfig.errorHandler();
        assertNotNull(handler);

        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("testCache");
        RuntimeException ex = new RuntimeException("Redis connection error");

        assertDoesNotThrow(() -> handler.handleCacheGetError(ex, cache, "key1"));
        assertDoesNotThrow(() -> handler.handleCachePutError(ex, cache, "key1", "val1"));
        assertDoesNotThrow(() -> handler.handleCacheEvictError(ex, cache, "key1"));
        assertDoesNotThrow(() -> handler.handleCacheClearError(ex, cache));
    }
}
