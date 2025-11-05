package app.config;

import app.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for scheduled jobs.
 * Implements both cron-based and fixed-rate scheduling.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final AgentService agentService;

    /**
     * Scheduled job with cron expression.
     * Runs daily at 2:00 AM to clean up old/draft properties.
     * NOTE: Properties are now managed in property-service microservice.
     * This scheduled job is disabled. Cleanup should be handled by property-service.
     */
    // @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldProperties() {
        log.info("Property cleanup scheduled job disabled - properties are now managed in property-service");
        // Properties are managed in property-service microservice
        // Cleanup should be handled there
    }

    /**
     * Scheduled job with fixed rate (non-cron trigger).
     * Runs every hour (3600000 milliseconds) to update property statistics.
     * NOTE: Properties are now managed in property-service microservice.
     * This scheduled job is disabled. Statistics should be handled by property-service.
     */
    // @Scheduled(fixedRate = 3600000) // Every hour
    public void updatePropertyStatistics() {
        log.info("Property statistics scheduled job disabled - properties are now managed in property-service");
        // Properties are managed in property-service microservice
        // Statistics should be handled there
    }

    /**
     * Alternative example: Fixed delay scheduled job.
     * Runs 30 minutes after the previous execution completes.
     * This demonstrates fixedDelay as another non-cron option.
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000) // 30 minutes delay, start after 1 minute
    public void updateAgentRatings() {
        log.info("Starting scheduled job: Updating agent ratings");
        try {
            // Recalculate agent ratings based on recent activity
            int updatedCount = agentService.recalculateAgentRatings();
            log.info("Scheduled agent rating update completed: {} agents updated", updatedCount);
        } catch (Exception e) {
            log.error("Error during scheduled agent rating update", e);
        }
    }
}

