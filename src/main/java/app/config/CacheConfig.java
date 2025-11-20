package app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Spring Caching.
 * Enables caching functionality throughout the application.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configures the cache manager using in-memory cache.
     * For production, consider using Redis or Caffeine.
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
            "cities",           // Cache for city listings
            "propertyTypes",    // Cache for property types
            "featuredProperties", // Cache for featured properties
            "statistics",       // Cache for statistics
            "allProperties"     // Cache for all properties from property-service
        );
    }
}

