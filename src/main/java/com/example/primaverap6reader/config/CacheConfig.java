package com.example.primaverap6reader.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with different cache specifications
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Set default cache specification
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100));

        // Set cache names to be created
        cacheManager.setCacheNames(Arrays.asList(
                "allProjects",
                "projectsPage",
                "filteredProjects",
                "projectCount",
                "project",
                "projectActivities",
                "projectActivitiesPage",
                "filteredActivities",
                "activityCount",
                "projectStatusStats",
                "activityTypeStats",
                "activityStatusStats",
                "overdueProjects",
                "upcomingProjects",
                "projectTimelineStats",
                "topProjectsStats",
                "projectDurationStats",
                "projectSummaryStats"
        ));

        return cacheManager;
    }

    /**
     * Configure custom cache specifications for different caches
     */
    @Bean
    public Caffeine<Object, Object> projectsCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500);
    }

    /**
     * Configure custom cache specifications for statistics caches (longer duration)
     */
    @Bean
    public Caffeine<Object, Object> statisticsCaffeine() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(100);
    }
}