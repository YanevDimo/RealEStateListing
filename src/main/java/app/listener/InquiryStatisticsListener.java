package app.listener;

import app.event.InquiryCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InquiryStatisticsListener {

    @EventListener
    public void updateInquiryStatistics(InquiryCreatedEvent event) {
        log.info("EVENT: Updating statistics - New inquiry created for property: '{}' (ID: {})",
                event.getPropertyTitle(), event.getPropertyId());
        log.info("Inquiry statistics - Property ID: {}, Agent ID: {}, Total inquiries for this property will be updated",
                event.getPropertyId(), event.getAgentId());
        
    }
}

