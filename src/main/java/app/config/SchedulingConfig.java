package app.config;

import app.service.AgentService;
import app.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Configuration class for scheduled jobs.
 * Implements both cron-based and fixed-rate scheduling as required.
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final PropertyService propertyService;
    private final AgentService agentService;

    /**
     * Scheduled job with cron expression.
     * Runs daily at 2:00 AM to clean up old/draft properties.
     * Cron format: second minute hour day month weekday
     * "0 0 2 * * ?" = Every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldProperties() {
        log.info("Starting scheduled job: Cleaning up old draft properties");
        try {
            // Find properties that are DRAFT status and older than 30 days
            // and mark them as INACTIVE or delete them
            int cleanedCount = propertyService.cleanupOldDraftProperties();
            log.info("Scheduled cleanup completed: {} old draft properties processed", cleanedCount);
        } catch (Exception e) {
            log.error("Error during scheduled property cleanup", e);
        }
    }

    /**
     * Scheduled job with fixed rate (non-cron trigger).
     * Runs every hour (3600000 milliseconds) to update property statistics.
     * This is a non-cron trigger using fixedRate.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void updatePropertyStatistics() {
        log.info("Starting scheduled job: Updating property statistics");
        try {
            // Update cached statistics for properties
            propertyService.updatePropertyStatistics();
            log.info("Scheduled statistics update completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled statistics update", e);
        }
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

