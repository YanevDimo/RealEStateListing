package app.config;

import app.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SchedulingConfig {

    private final AgentService agentService;

    
     //Scheduled job with cron expression.
     // Runs daily at 3:00 AM to sync agent listing counts from property-service.
     // This ensures agent.totalListings stays in sync with actual property count.
     
    @Scheduled(cron = "0 0 3 * * ?")
    public void syncAgentListingsFromProperties() {
        log.info("Starting scheduled job: Syncing agent listings from property-service");
        try {
            int updatedCount = agentService.syncAgentListingsFromPropertyService();
            log.info("Scheduled agent listings sync completed: {} agents updated", updatedCount);
        } catch (Exception e) {
            log.error("Error during scheduled agent listings sync", e);
        }
    }

    
     // Scheduled job with fixed delay trigger.
     // Runs 15 minutes after the previous execution completes.
     
    @Scheduled(fixedDelay = 900000, initialDelay = 60000)
    public void updateAgentRatings() {
        log.info("Starting scheduled job: Updating agent ratings");
        try {
            int updatedCount = agentService.recalculateAgentRatings();
            log.info("Scheduled agent rating update completed: {} agents updated", updatedCount);
        } catch (Exception e) {
            log.error("Error during scheduled agent rating update", e);
        }
    }
}

